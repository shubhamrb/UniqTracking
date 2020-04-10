package com.example.uniqtracking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.uniqtracking.models.Drivers;
import com.firebase.geofire.GeoFire;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener
{

    private GoogleMap mMap;

    //Play Service
    private static final int MY_PERMISSION_REQUEST_CODE=7000;
    private static final int PLAY_SERVICE_RES_REQUEST=7001;

    private LocationRequest mLocationReq;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private static int UPDATE_INTERVAL=3000;
    private static int FASTEST_INTERVAL=1500;
    private static int DISPLACEMENT=0;

    DatabaseReference drivers;
    GeoFire geoFire;
    Marker mCurrent;

    SupportMapFragment mapFragment;

    AppCompatButton btnSwitch;
    boolean flag=false,passenger=false;
    LocationManager locationManager;
    private double latitude,longitude;
    private String driverName;
    private HashMap<String,Marker> hashMap=new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        btnSwitch=findViewById(R.id.btnSwitch);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Geo Fire
        FirebaseApp.initializeApp(this);
        drivers= FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire=new GeoFire(drivers);

        if (getIntent().getStringExtra("user").equals("driver")){
            driverName =getIntent().getStringExtra("name");
            btnSwitch.setVisibility(View.VISIBLE);
            setUpLocation();
        }else {
            passenger=true;
            btnSwitch.setVisibility(View.GONE);
        }


        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag){
                    startLocationUpdates();
                    displayLocation();
                    btnSwitch.setText("OFFLINE");
                    Snackbar.make(mapFragment.getView(),"You are online",Snackbar.LENGTH_SHORT).show();
                    flag=false;
                }else {
                    mCurrent.remove();
                    stopLocationUpdates();
                    btnSwitch.setText("ONLINE");
                    Snackbar.make(mapFragment.getView(),"You are offline",Snackbar.LENGTH_SHORT).show();
                    flag=true;

                }
            }
        });

    }

    @Override
    protected void onPause() {
        if (!passenger)
        stopLocationUpdates();
        super.onPause();
    }

    private void getLatLongFromFirebase() {

        ChildEventListener childEventListener=new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.hasChildren()){
                    Drivers drivers=dataSnapshot.getValue(Drivers.class);

                    //Log.e("NAME", drivers.getName());
                    latitude=drivers.getLat();
                    longitude=drivers.getLng();
                    driverName=drivers.getName();
                    displayLocationForPassenger(latitude,longitude,driverName);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                Marker marker=hashMap.get(dataSnapshot.getKey());
                //Log.e("MARKER",marker.getTitle());
                if (marker!=null){
                    marker.remove();
                    hashMap.remove(dataSnapshot.getKey());
                    //Add updated marker
                    Drivers drivers=dataSnapshot.getValue(Drivers.class);
                    latitude=drivers.getLat();
                    longitude=drivers.getLng();
                    driverName=drivers.getName();
                    displayLocationForPassenger(latitude,longitude,driverName);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Marker marker=hashMap.get(dataSnapshot.getKey());
                //Log.e("MARKER",marker.getTitle());

                //Remove marker
                if (marker!=null){
                    marker.remove();
                    hashMap.remove(dataSnapshot.getKey());

                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ERROR","Something went wrong");
            }
        };

        drivers.addChildEventListener(childEventListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    if (checkPlayServices()){
                        buildGoogleApiClient();
                        //if (!passenger)
                        createLocationRequest();

                        displayLocation();
                    }
                }
        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            //Request run time permission
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }else {
            if (checkPlayServices()){
                buildGoogleApiClient();
                createLocationRequest();

                displayLocation();
            }
        }
    }

    private void createLocationRequest() {
        mLocationReq=new LocationRequest();
        mLocationReq.setInterval(UPDATE_INTERVAL);
        mLocationReq.setFastestInterval(FASTEST_INTERVAL);
        mLocationReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationReq.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode!=ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)){
                GooglePlayServicesUtil.getErrorDialog(resultCode,this,PLAY_SERVICE_RES_REQUEST).show();
            }else {
                Toast.makeText(this,"This device is not supported",Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }



    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation!=null){
            double latitude=mLastLocation.getLatitude();
            double longitude=mLastLocation.getLongitude();

            //Add Marker
            if (mCurrent!=null)
                mCurrent.remove(); // remove existing marker

            mCurrent=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                    .position(new LatLng(latitude,longitude)).title("You"));

            //Move camera to this position
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));

        }else {
            Log.e("ERROR","Can not find your location");
        }
    }
    private void displayLocationForPassenger(double latitude, double longitude, String driverName) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

            //Add Marker
            if (mCurrent!=null)
                //mCurrent.remove(); // remove existing marker
            Log.e("PAS LAT", String.valueOf(latitude));
            Log.e("PAS LONG", String.valueOf(longitude));
            mCurrent=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                    .position(new LatLng(latitude, longitude)).title(driverName));

            hashMap.put(mCurrent.getTitle(),mCurrent);
            //Move camera to this position

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(this.latitude, this.longitude),15.0f));

    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationReq,this);
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,  this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.e("mMap","Called");
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            //Request run time permission
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }else {
            if (passenger){
                getLatLongFromFirebase();
            }
        }

    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        if (!passenger)
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (!passenger) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            Log.e("LAT", String.valueOf(latitude));
            Log.e("LONG", String.valueOf(longitude));

            mLastLocation = location;
            drivers.child(driverName).child("lat").setValue(latitude);
            drivers.child(driverName).child("lng").setValue(longitude);
            drivers.child(driverName).child("name").setValue(driverName);
            displayLocation();
        }

    }
}
