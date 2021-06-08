package com.example.uberclone;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.KeyStore;
import java.util.List;
import java.util.Locale;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    TextView mDriverStatus;
    private GoogleMap mMap;
    LocationManager mLocationManager;
    LocationListener mLocationListener;
    Location mLocation;
    Location mUserLocation;
    boolean rideStatusActive;
    int markersCount;
    LatLng mDestinantionLatLng;
    LatLng mDriverLocation=null;

    //overriding the onRequestPermissionsResult function

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                              return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            centerMapOnLocation(lastKnownLocation);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        mDriverStatus=(TextView) findViewById(R.id.driverDistancetextView);

        //if exists
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        rideStatusActive = false;
        markersCount=0;

        //already checking for updates
        checkForUpdates();
        //Handler to dinamically updating distance b/w user nd driver and showing to user
        Handler handler=new Handler();

        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                checkForUpdates();
                handler.postDelayed(this,2000);
            }
        };
        handler.post(runnable);
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

        //Getting user's location and asking permissions
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.i("user's location", location.getLongitude() + "::" + location.getLongitude());
                mLocation = location;
                centerMapOnLocation(location);
            }
        };

        //permission not there
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        //permission already there
        else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            centerMapOnLocation(lastKnownLocation);
        }

        //setting on long click listner to let user select place
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                centerMapOnLocation(latLng);
                Toast.makeText(getApplicationContext(), "Location Set ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //centers the map on given location
    public void centerMapOnLocation(Location location) {
        mLocation = location;
        try {
            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            Log.i("location", "location recieved : " + location.getLongitude());
            //Geocoder
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.US);
            List<Address> place = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            mMap.addMarker(new MarkerOptions().position(userLatLng).title(place.get(0).getAddressLine(0)));
            if(markersCount==0)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error Occured while setting location 1"+e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    //centers the map on given location
    public void centerMapOnUserLocation() {
        Location location=mLocation;
        markersCount=0;
        try {
            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            Log.i("location", "location recieved : " + location.getLongitude());
            //Geocoder
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.US);
            List<Address> place = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            mMap.addMarker(new MarkerOptions().position(userLatLng).title(place.get(0).getAddressLine(0)));
            if(markersCount==0)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error Occured while setting a location"+e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    //centers the map on given latlngs
    public void centerMapOnLocation(LatLng latLng) {
        try {
            LatLng userLatLng = new LatLng(latLng.latitude, latLng.longitude);
            //Geocoder
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.US);
            List<Address> place = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            Log.i("location", "location recieved : " + place.get(0).getAddressLine(0).toString());
            mMap.clear();
            //again adding location marker
            makeMarkerOnCurrentLocation();
            mMap.addMarker(new MarkerOptions().position(userLatLng).title(place.get(0).getAddressLine(0))).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15f));
            markersCount=1;
            mDestinantionLatLng=latLng;
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error Occured while setting location 2"+e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    // saves the destination of pickup and drop
    public void getUber(View view) throws ParseException {
    Log.i("costom",rideStatusActive+"ERROR");
        //cancelling the uber
        ParseQuery<ParseObject> parseQuery = new ParseQuery<ParseObject>("Request");
        parseQuery.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);

        //if we have a valid location and ride requested
        if (mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null && rideStatusActive == false && mDestinantionLatLng!=null && parseQuery.find()!=null) {

            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            //saving data in a new class Request
            ParseObject parseObject = new ParseObject("Request");
            //saving username
            parseObject.put("username", ParseUser.getCurrentUser().getUsername());

            //saving user location
            ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

            parseObject.put("location", parseGeoPoint);

            //saving all
            parseObject.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(getApplicationContext(), "Ride Booked! Uber on it's way", Toast.LENGTH_LONG).show();

                        showDriverLocation();
                        ((Button) view).setText("Cancel Uber");
                        rideStatusActive = true;
                    } else {
                        Toast.makeText(getApplicationContext(), "Error Occured, could not book ride", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                        Log.i("ERROR ayush",e.getMessage());
                    }
                }
            });
        }

        //if canceling ride
        else if (rideStatusActive == true ) {

            parseQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        if (objects.size() > 0) {
                            for (ParseObject object : objects) {
                                object.deleteInBackground();
                            }
                            Toast.makeText(getApplicationContext(), "Request Cancelled", Toast.LENGTH_SHORT).show();
                            rideStatusActive = false;
                            ((Button) view).setText("Get Uber");
                            ((Button)findViewById(R.id.logoutUserRiderButton)).setVisibility(View.VISIBLE);
                            ((Button)findViewById(R.id.logoutUserRiderButton)).animate().alpha(1f).setDuration(1000);
                            mMap.clear();
                            deleteDriver();
                            centerMapOnUserLocation();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Error occured while deleting request", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }

        //no valid location available
        else {
            Toast.makeText(getApplicationContext(), "Select Destination by Long Clicking On Map", Toast.LENGTH_LONG).show();
        }

    }

    //making marker on current location
    private void makeMarkerOnCurrentLocation() throws IOException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        Log.i("location", "location recieved : " + location.getLongitude());
        //Geocoder
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.US);
        List<Address> place = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

        mMap.animateCamera(CameraUpdateFactory.newLatLng(userLatLng),2000,null);
        mMap.addMarker(new MarkerOptions().position(userLatLng).title(place.get(0).getAddressLine(0)));

    }

    //logout user
    public void logoutRider(View view){
        ParseUser.logOut();
        Intent intent=new Intent(getApplicationContext(),LoginActivity.class);
        startActivity(intent);
        finish();
    }

    //updates text view for driver updates to the user
    public  void checkForUpdates(){
        if(ParseUser.getCurrentUser()==null){return;}
        ParseQuery<ParseObject> query=ParseQuery.getQuery("Request");
        query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if(objects.size()>0){
                        mDriverStatus.animate().alpha(1f).setDuration(1000);
                        mDriverStatus.setText("Driver is on the way");

                        //hiding the logout and changing text of cancel button button
                        ((Button)findViewById(R.id.getUberButton)).setText("Cancel Uber");
                        rideStatusActive=true;
                        ((Button)findViewById(R.id.logoutUserRiderButton)).animate().alpha(0).setDuration(1000);
                        ((Button)findViewById(R.id.logoutUserRiderButton)).setVisibility(View.INVISIBLE);

                    }else{
                        mDriverStatus.setAlpha(0);
                    }
                }else{
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Error from server side, contact admin",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void showDriverLocation(){
        //query to get drivers location
        ParseQuery<ParseObject> query=ParseQuery.getQuery("Request");
        query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null && objects.size()>0){
                    if(objects.get(0).get("location")==null)
                        return;
                    ParseGeoPoint gpt=(ParseGeoPoint)objects.get(0).get("location");
                    Log.i("costom",gpt+" ");
                    mDriverLocation=new LatLng(gpt.getLatitude(),gpt.getLongitude());
                    Marker marker=mMap.addMarker(new MarkerOptions().position(mDriverLocation).title("Driver"));
                    marker.showInfoWindow();
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(mDriverLocation));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(15f));

                }else{
                    Toast.makeText(getApplicationContext(),"Driver on his way, yet to share his location",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void deleteDriver(){

        ParseQuery<ParseObject> query=ParseQuery.getQuery("Request");
        query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e==null && objects.size()>0){
                    objects.get(0).put("driverUsername"," ");
                    try {
                        objects.get(0).save();
                    } catch (ParseException parseException) {
                        parseException.printStackTrace();
                    }
                }
            }
        });
    }

}