package com.example.uberclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {

    ListView mRequestListView;
    ArrayList<String> mRequests;
    ArrayAdapter<String > mArrayAdapter;

    //location
    LocationManager mLocationManager;
    LocationListener mLocationListener;
    Location mDriverLocation;
    Context contextOnCreate;

    List<Double> riderLongitude=new ArrayList<>();
    List<Double> riderLatitude=new ArrayList<>();
    ArrayList<String> mRiderUsername=new ArrayList<>();

    //next 2 functions created the title bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu_view_request_activity,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.logout : {
                ParseUser parseUser=ParseUser.getCurrentUser();
                parseUser.logOut();
                Intent intent=new Intent(ViewRequestsActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
                Toast.makeText(getApplicationContext(),"Logged Out",Toast.LENGTH_SHORT).show();
            }
            case R.id.exit:{
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateRequestsList(Location location){

        mRequests.clear();
        riderLatitude.clear();
        riderLongitude.clear();

        Log.i("updateRequestList","counter 1");

        ParseQuery<ParseObject> query=ParseQuery.getQuery("Request");

        //getting location of rider
        ParseGeoPoint parseGeoPoint=new ParseGeoPoint(location.getLatitude(),location.getLongitude());

        query.whereNear("location",parseGeoPoint);

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                Log.i("updateRequestList","counter 2");
                if(e==null){
                    if (objects.size()>0){
                        Log.i("updateRequestList","found requests size : "+objects.size());
                        for(ParseObject obj:objects){

                            //iterating on all pending requests

                            Double distance= parseGeoPoint.distanceInKilometersTo((ParseGeoPoint) obj.get("location"));
                            distance=Double.parseDouble(String.valueOf((Math.round(distance*100)/100)));
                            Log.i("updateRequestList","found distance "+distance);

                            ///updating datasets
                            mRequests.add("Rider nearby at "+distance+" Km");
                            riderLatitude.add(((ParseGeoPoint)obj.get("location")).getLatitude());
                            riderLongitude.add(((ParseGeoPoint)obj.get("location")).getLongitude());
                            mRiderUsername.add(obj.getString("username"));
                            Log.i("updateRequestList","counter 3");

                            Log.i("updateRequestList","mRequest CHANGED TO :::  "+mRequests.toString());
                        }
                    }else{
                        Toast.makeText(getApplicationContext(),"No Users Nearby",Toast.LENGTH_LONG).show();
                    }
                    //updating dataset in list view
                    mArrayAdapter.notifyDataSetChanged();
                }else{
                    Log.i("updateRequestList","ERROR");
                    e.printStackTrace();
                }
            }
        });
        Log.i("updateRequestList","DATASET CHANGED TO :::  "+mRequests.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {    }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            mDriverLocation=mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateRequestsList(mDriverLocation);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        setTitle("Nearby Requests");
        contextOnCreate=this;

        //init
        mRequestListView=(ListView) findViewById(R.id.requestsListView);
        mRequests=new ArrayList<String>();
        mRequests.clear();
        mRequests.add("Loading nearby requests ...");
        mArrayAdapter=new ArrayAdapter<String >(this, android.R.layout.simple_list_item_1,mRequests);
        mRequestListView.setAdapter(mArrayAdapter);

        //geting the driver's location
        //Getting user's location and asking permissions
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                Log.i("user's location", location.getLongitude() + "::" + location.getLongitude());
//                mLocation = location;
//                centerMapOnLocation(location);
                //saving drivers location on parse server to show to user
                ParseUser.getCurrentUser().put("location",new ParseGeoPoint(location.getLatitude(),location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
            }
        };

        //permission not there
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        //permission already there
        else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            mDriverLocation=mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateRequestsList(mDriverLocation);
//            Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            centerMapOnLocation(lastKnownLocation);
        }
        Log.i("updateRequestList","Request list view updated");


        //list view item click listener
        mRequestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //getting the last known location and redirecting to DriverLocationActivity
                if (mRiderUsername==null || riderLatitude == null || riderLongitude == null || mDriverLocation == null) {
                    Toast.makeText(getApplicationContext(), "Cannot Retrieve the location data to track user, contact admin(Ayush)", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                    Log.i("updateRequestList", riderLatitude.get(position).toString() + mRequests.get(position));
                    intent.putExtra("userLatitude", riderLatitude.get(position));
                    intent.putExtra("userLongitude", riderLongitude.get(position));
                    intent.putExtra("driverLatitude", mDriverLocation.getLatitude());
                    intent.putExtra("driverLongitude", mDriverLocation.getLongitude());
                    intent.putExtra("username",mRiderUsername.get(position));
                    startActivity(intent);
                }
            }
        });
    }
}