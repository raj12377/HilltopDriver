package com.bradleyhilltopdriver.android.hilltopdriver;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Raj on 3/13/2016.
 */
public class HomeActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int REQUEST_FINE_LOC = 0;
    private Intent sendLocIntent;
    public static RequestQueue mRequestQueue;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private LatLng previous = null;
    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 35; // 35 meters

    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.home);
        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient();
            createLocationRequest();
        } else {
            Toast toast = Toast.makeText(this,"Google play services not found",Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (!isNetworkAvailable()) {
            Toast toast = Toast.makeText(this,"Please check your internet connectivity",Toast.LENGTH_LONG);
            toast.show();
        }
        //requestLocAndStartLooper();
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);
        // Start the queue
        mRequestQueue.start();


    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }


    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    protected void onStop() {
        super.onStop();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("HomeActivity", "Received Location permission, starting send location");
                    /*
                    try {
                        sendLocation();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }*/
                    startService(new Intent(this, SendLocationService.class));
                    enableStop();
                } else {
                    System.out.println("permission denied");
                }
                return;
            }
        }
    }

    public void requestLocAndStartLooper() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOC);
                // The callback method gets the result of the request.
            }

        } else {
            Log.i("HomeActivity", "Location access permission is already granted");
            sendLocIntent = new Intent(this, SendLocationService.class);
            startService(sendLocIntent);
            enableStop();
        }
    }

    public void stopService(View v) {
        System.out.println("Send location service stopped");
        //stopService(sendLocIntent);
        togglePeriodicLocationUpdates();
        enableStart();
    }

    public void startService(View v) {
        Log.i("HomeActivity", "Starting send location service");
        sendLocIntent = new Intent(this, SendLocationService.class);
        //startService(sendLocIntent);
        togglePeriodicLocationUpdates();
        enableStop();
    }

    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d("HomeActivity", "Periodic location updates started!");

        } else {
            // Changing the button text


            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            Log.d("HomeActivity", "Periodic location updates stopped!");
        }
    }

    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            System.out.println("NO permission to get location");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }



    public void enableStart() {
        ((TextView) findViewById(R.id.status)).setText("Status : Idle");
        ((Button) findViewById(R.id.start)).setEnabled(true);
        ((Button) findViewById(R.id.stop)).setEnabled(false);
    }

    public void enableStop() {
        ((TextView) findViewById(R.id.status)).setText("Status : Sending location to server..");
        ((Button) findViewById(R.id.start)).setEnabled(false);
        ((Button) findViewById(R.id.stop)).setEnabled(true);
    }

    @Override
    public void onConnected(Bundle bundle) {
        System.out.println("Connected");
        sendLocation();

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i("HomeActivity", "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    public void sendLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            System.out.println("No premission, returning");
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        if (mLastLocation == null) {
            Toast toast = Toast.makeText(this,"Could not get location, Please enable your location services",Toast.LENGTH_LONG);
            toast.show();
        }
         else {
            System.out.println("got location " + mLastLocation.getLatitude() + "," + mLastLocation.getLongitude());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("location changed. new location " + location);
        sendToServer(location);
    }

    private void sendToServer(Location loc) {
        if (loc == null) {
            System.out.println("WARNING : COULD NOT GET LOCATION, NOT SENDING TO HILLTOP SERVER");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }
        System.out.println("sending location (" + loc.getLatitude() + "," + loc.getLongitude() + ")");
        LatLng driverLoc = new LatLng(loc.getLatitude(), loc.getLongitude());

        String URL = "http://hilltop-bradleyuniv.rhcloud.com/rest/updateLocation/" + driverLoc.latitude + "," + driverLoc.longitude + ",";
        if (previous != null)
            URL += getBearing(driverLoc, previous);
        else
            URL += 0;

        previous = driverLoc;
        try {
            JsonObjectRequest req = new JsonObjectRequest(URL, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                VolleyLog.v("Response:%n %s", response.toString(4));
                                Log.i("SendLocationLooper","response " + response.toString(4));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.e("Error: ", error.getMessage());
                    error.printStackTrace();
                }
            });
            mRequestQueue.add(req);
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private float getBearing(LatLng from, LatLng to) {
        Location fromL = getLocation(from);
        Location toL = getLocation(to);
        return fromL.bearingTo(toL);
    }

    private Location getLocation(LatLng location) {
        Location loc = new Location("loc");
        loc.setLatitude(location.latitude);
        loc.setLongitude(location.longitude);
        return loc;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
