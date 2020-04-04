package com.example.uniqtracking;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;

public class MainActivity extends AppCompatActivity {

    private AppCompatButton btnLocate;
    private RadioButton driver, passenger;
    private boolean driverFlag=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                if (driverFlag){
                    Intent intent= new Intent(MainActivity.this, MapsActivity.class);
                    intent.putExtra("user","driver");
                    startActivity(intent);
                }else {
                    Intent intent= new Intent(MainActivity.this, MapsActivity.class);
                    intent.putExtra("user","passenger");
                    startActivity(intent);
                }

            }
        });
    }
}
