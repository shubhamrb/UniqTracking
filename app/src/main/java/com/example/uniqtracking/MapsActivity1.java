package com.example.uniqtracking;

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

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

public class MapsActivity1 extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener
{

    private GoogleMap mMap;

    //Play Services
    private static final int MY_PERMISSION_REQUEST_CODE=7000;
    private static final int PLAY_SERVICE_RES_REQUEST=7001;
    //Location request object
    private LocationRequest mLocationReq;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private static int UPDATE_INTERVAL=3000;
    private static int FASTEST_INTERVAL=1500;
    private static int DISPLACEMENT=0;
    boolean isWithin10Km=false;
    //firebase database reference
    DatabaseReference drivers;
    GeoFire geoFire;
    Marker mCurrent;

    SupportMapFragment mapFragment;

    AppCompatButton btnSwitch;
    boolean flag=false,passenger=false;
    private double latitude,longitude;
    private String driverName;
    //HashMap to store the Markers present on the Map
    private HashMap<String,Marker> hashMap=new HashMap<>();

    ChildEventListener childEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps1);

        btnSwitch=findViewById(R.id.btnSwitch);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        FirebaseApp.initializeApp(this);
        drivers= FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire=new GeoFire(drivers);
        //Log.e("USER",getIntent().getStringExtra("user"));

        //Check whether the user is Driver or Passenger
        if (getIntent().getStringExtra("user").equals("driver")){ //Driver
            passenger=false;
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
                if (flag){ //Toggle button between Online/Offline

                    startLocationUpdates();//Start location update

                    displayLocation();//Display driver's Location marker on map
                    btnSwitch.setText("OFFLINE");
                    Snackbar.make(mapFragment.getView(),"You are online",Snackbar.LENGTH_SHORT).show();
                    flag=false;
                }else {
                    mCurrent.remove(); //Remove driver's marker
                    stopLocationUpdates(); //Stop location update
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
        stopLocationUpdates(); // Stop location update only if user is Driver because at the time of passenger we didn't start it
        super.onPause();
    }

    //Get all the driver's lat long from the Firebase to show on map for passenger.
    private void getLatLongFromFirebase() {
        Log.e("passenger", String.valueOf(passenger));
        childEventListener=new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (dataSnapshot.hasChildren()){
                    Drivers drivers=dataSnapshot.getValue(Drivers.class);

                    ////Log.e("NAME", drivers.getName());
                    latitude=drivers.getLat();
                    longitude=drivers.getLng();
                    driverName=drivers.getName();

                    //check whether the driver is within the given radius or not
                    //if yes show it on Map
                    float[] result=new float[1];
                    Location.distanceBetween(23.2472984,77.43307,latitude,longitude,result);
                    float disInMeters=result[0];
                    isWithin10Km=disInMeters<10000;

                    //Display on Map
                    displayLocationForPassenger(latitude,longitude,driverName);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                //This method will be called when the child in firebase will change OR you can say Driver's location gets changed
                Marker marker=hashMap.get(dataSnapshot.getKey());
                //Log.e("MARKER",dataSnapshot.getKey()+" = "+marker.getTitle());

                if (dataSnapshot.getKey().equals(marker.getTitle())){
                    marker.remove(); // remove old marker
                    hashMap.remove(dataSnapshot.getKey()); // also remove it from HashMap

                }
                    //Get updated location (LAT/LNG)
                    Drivers drivers=dataSnapshot.getValue(Drivers.class);
                    latitude=drivers.getLat();
                    longitude=drivers.getLng();
                    driverName=drivers.getName();

                    //Again check if it is within the given radius or not
                    float[] result=new float[1];
                    Location.distanceBetween(23.2472984,77.43307,latitude,longitude,result);
                    float disInMeters=result[0];
                    isWithin10Km=disInMeters<10000;
                    //Log.e("disInMeters ", String.valueOf(disInMeters));

                //Display it on Map
                    displayLocationForPassenger(latitude,longitude,driverName);

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Marker marker=hashMap.get(dataSnapshot.getKey());
                ////Log.e("MARKER",marker.getTitle());

                //Remove marker when child/Driver gets deleted from database
                if (marker!=null){
                    //Also remove from Google Map and HashMap
                    marker.remove();
                    hashMap.remove(dataSnapshot.getKey());
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                //Log.e("ERROR","Something went wrong");
            }
        };
        drivers.addChildEventListener(childEventListener);

    }

    @Override
    public void onBackPressed() {
        if (passenger)
        drivers.removeEventListener(childEventListener);// Stop location update
        super.onBackPressed();
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


    //Display location for Driver
    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        //Get the Last known location
        mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation!=null){
            double latitude=mLastLocation.getLatitude();
            double longitude=mLastLocation.getLongitude();

            if (mCurrent!=null)
                mCurrent.remove(); // remove existing marker

            if (!passenger){// Driver's marker as Car icon
                mCurrent=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                        .position(new LatLng(latitude,longitude)).title("You"));
            }else {
                //passenger's default marker icon
                mCurrent=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.pas_location))
                        .position(new LatLng(latitude,longitude)).title("You"));
            }


            //Move camera to this position
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));

        }else {
            //Log.e("ERROR","Can not find your location");
        }
    }


    //Display location for passenger
    private void displayLocationForPassenger(double latitude, double longitude, String driverName) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        //Show marker only if the driver is present within the 100m radius
        if (isWithin10Km){
            //Log.e("within"," true");
            mCurrent=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                    .position(new LatLng(latitude, longitude)).title(driverName));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude),15.0f));
        }
            hashMap.put(driverName,mCurrent);
            //Move camera to this position



    }

    //Start location update
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationReq,this);
    }

    //Stop location update
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
        //Log.e("mMap","Called");
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

            //Map is ready and location permission is given
            //Now get the driver's Lat long from the firebase only if the user is a passenger
            if (passenger){
                Log.e("getLatLongFromFirebase(",") Called");
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

            ////Log.e("LAT", String.valueOf(latitude));
            ////Log.e("LONG", String.valueOf(longitude));

            mLastLocation = location;
            drivers.child(driverName).child("lat").setValue(latitude);
            drivers.child(driverName).child("lng").setValue(longitude);
            drivers.child(driverName).child("name").setValue(driverName);

            // each time location changes update the marker
            displayLocation();
        }

    }
}
