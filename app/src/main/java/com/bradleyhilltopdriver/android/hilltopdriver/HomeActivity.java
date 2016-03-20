package com.bradleyhilltopdriver.android.hilltopdriver;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;

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
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Raj on 3/13/2016.
 */
public class HomeActivity extends Activity {
    private LocationManager locationManager;
    private String provider;
    private static final int REQUEST_FINE_LOC = 0;
    private LatLng previous = null;


    Location myLocation;
    RequestQueue mRequestQueue;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient mGoogleApiClient;

    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.home);

        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();

        /*
        try {
           sendLocation();
        } catch (JSONException e) {
           e.printStackTrace();
        }
        */
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(AppIndex.API)
                    .build();
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_FINE_LOC);
        } else {
            try {
                sendLocation();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



    }
    public void sendLocation() throws JSONException {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                while (true)
                {
                    Location loc=getCurrentDriverLocation();
                    if (loc == null) {
                        System.out.println("WARNING : COULD NOT GET LOCATION, NOT SENDING TO HILLTOP SERVER");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    System.out.println("sending location ("+loc.getLatitude()+","+loc.getLongitude()+")");
                    LatLng driverLoc=new LatLng(loc.getLatitude(),loc.getLongitude());
                    String URL="http://hilltop-bradleyuniv.rhcloud.com/rest/updateLocation/"+driverLoc.latitude+","+driverLoc.longitude+",";
                    if (previous != null)
                        URL+=getBearing(driverLoc, previous);
                    else
                        URL+=0;

                    try {
                        JsonObjectRequest req = new JsonObjectRequest(URL,null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            VolleyLog.v("Response:%n %s", response.toString(4));
                                            System.out.println("response "+response.toString(4));

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
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };
        Thread sendLocationThread = new Thread(runnable);
        sendLocationThread.start();

    }
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
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



    private Location getCurrentDriverLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("no permission to access location of device");
            return null;
        }
        System.out.println("provider is "+provider);
        System.out.println("location manager is " + locationManager.toString());
        if (locationManager != null) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {
                        }

                        @Override
                        public void onProviderEnabled(String provider) {
                        }

                        @Override
                        public void onProviderDisabled(String provider) {
                        }

                        @Override
                        public void onLocationChanged(final Location location) {
                        }
                    });
            myLocation = locationManager.getLastKnownLocation(provider);
        }
        return myLocation;

    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Location l1=getCurrentDriverLocation();
                    System.out.print("lat" + l1.getLatitude() + "long" + l1.getLongitude());
                } else {
                    System.out.println("permission denied");
                }
                return;
            }
        }
    }



}
