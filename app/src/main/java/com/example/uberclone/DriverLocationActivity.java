package com.example.uberclone;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class DriverLocationActivity extends FragmentActivity implements OnMapReadyCallback  {

    private GoogleMap mMap;
    Intent intent;
    Button mToggleLocationBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //getting intent data from the ViewRequestActivity
        intent=getIntent();
        mToggleLocationBtn=findViewById(R.id.goToUserButton);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //this dors the work to centter the location of driver and user
        positionTheLocations();
    }

    private void positionTheLocations() {

        // Add a marker on driver and move the camera
        LatLng driverLatLng=new LatLng(intent.getDoubleExtra("driverLatitude",-34),intent.getDoubleExtra("driverLongitude",151));
        Log.i("LOC",driverLatLng+"");
        mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Location")).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLatLng,15f));

        // Add a marker on user and move the camera
        LatLng userLatLng=new LatLng(intent.getDoubleExtra("userLatitude",-34),intent.getDoubleExtra("userLongitude",151));
        mMap.addMarker(new MarkerOptions().position(userLatLng).title("User Location")).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        //user driver map switch listener
        ((Button)findViewById(R.id.goToUserButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mToggleLocationBtn.getTag()=="driver"){
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,15f));
                    mToggleLocationBtn.setText("You");
                    mToggleLocationBtn.setTag("user");
                }else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLatLng,15f));
                    mToggleLocationBtn.setText("User");
                    mToggleLocationBtn.setTag("driver");
                }
            }
        });

    }

    public void acceptUserRequest(View view) {

        //creating the dialogue box
        new MaterialAlertDialogBuilder(this).setIcon(android.R.drawable.ic_input_add)
                .setTitle("Confirm Ride ?")
                .setMessage("The user will be confirmed about the request")


                //if the driver confirms request
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //saving the requst confirmation after driver accepts
                        ParseQuery<ParseObject> query= ParseQuery.getQuery("Request");
                        query.whereEqualTo("username",intent.getStringExtra("username"));

                        query.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> objects, ParseException e) {
                                if(e==null){
                                    if(objects.size()>0){
                                        for (ParseObject obj:objects){
                                            obj.put("dirverUsername", ParseUser.getCurrentUser().getUsername());

                                            //saving the object
                                            obj.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    Intent directions=new Intent((Intent.ACTION_VIEW),
                                                            Uri.parse("http://maps.google.com/maps?saddr="+intent.getDoubleExtra("driverLatitude",0)+","+intent.getDoubleExtra("driverLongitude",0)+"&daddr="+intent.getDoubleExtra("userLatitude",10)+","+intent.getDoubleExtra("userLongitude",10)));
                                                    startActivity(directions);
                                                }
                                            });
                                        }
                                    }
                                }else{
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(),"Error confirming uber",Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("More rides", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent2=new Intent(DriverLocationActivity.this,ViewRequestsActivity.class);
                        startActivity(intent2);
                        finish();
                    }
                })
                .show();
    }
}