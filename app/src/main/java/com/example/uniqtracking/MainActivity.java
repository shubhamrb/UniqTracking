package com.example.uniqtracking;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText txtUserName;
    private AppCompatButton btnLocate;
    private RadioButton driver, passenger;
    private boolean driverFlag=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtUserName=findViewById(R.id.driverName);
        driver=findViewById(R.id.radDri);
        passenger=findViewById(R.id.radPas);

        driver.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    driverFlag=true;
                    passenger.setChecked(false);
                }
            }
        });

        passenger.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    driverFlag=false;
                    driver.setChecked(false);
                }
            }
        });

        btnLocate=findViewById(R.id.btnLocate);
        btnLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String drivername=txtUserName.getText().toString();
                if (driverFlag){
                    if (!drivername.equals("")){
                        Intent intent= new Intent(MainActivity.this, MapsActivity1.class);
                        intent.putExtra("user","driver");
                        intent.putExtra("name",drivername);
                        startActivity(intent);
                    }else {
                        Toast.makeText(MainActivity.this,"Please enter the Driver name",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Intent intent= new Intent(MainActivity.this, MapsActivity1.class);
                    intent.putExtra("user","passenger");
                    startActivity(intent);
                }

            }
        });
    }
}
