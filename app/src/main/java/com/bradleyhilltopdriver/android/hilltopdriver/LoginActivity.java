package com.bradleyhilltopdriver.android.hilltopdriver;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;


import org.json.JSONException;
import org.json.JSONObject;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.entity.StringEntity;


/**
 * Login Activity Class
 */
public class LoginActivity extends Activity {
    RequestQueue mRequestQueue;
    // Progress Dialog Object
    ProgressDialog prgDialog;
    // Error Msg TextView Object
    TextView errorMsg;
    // Email Edit View Object
    EditText driverText;
    // Passwprd Edit View Object
    EditText pwdET;
    Context context;
    final String URL = "http://hilltop-bradleyuniv.rhcloud.com/rest/driverLogin";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        // Find Error Msg Text View control by ID
        errorMsg = (TextView) findViewById(R.id.login_error);
        // Find Email Edit View control by ID
        driverText = (EditText) findViewById(R.id.loginEmail);
        // Find Password Edit View control by ID
        pwdET = (EditText) findViewById(R.id.loginPassword);
        // Instantiate Progress Dialog object
        prgDialog = new ProgressDialog(this);
        // Set Progress Dialog Text
        prgDialog.setMessage("Please wait...");
        // Set Cancelable as False
        prgDialog.setCancelable(false);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2 = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();
    }

    /**
     * Method gets triggered when Login button is clicked
     *
     * @param view
     */
    public void loginUser(View view)  {
        // Get Email Edit View Value
        String driver = driverText.getText().toString();
        // Get Password Edit View Value
        String password = pwdET.getText().toString();
        // Instantiate Http Request Param Object

        // When Email Edit View and Password Edit View have values other than Null
        //if(Utility.isNotNull(driver) && Utility.isNotNull(password)){
                // Invoke RESTful Web Service with Http parameters

            // pass second argument as "null" for GET requests

            HashMap<String, String> params = new HashMap<String, String>();
            params.put("user",driver);
            params.put("pass",password);
            JsonObjectRequest req = new JsonObjectRequest(URL,new JSONObject(params),
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                VolleyLog.v("Response:%n %s", response.toString(4));
                                System.out.println("response "+response.toString(4));
                                if (response.getString("status").contains("success")){
                                    navigatetoHomeActivity();
                                } else {
                                    System.out.println("Invalid user/pass");
                                    //change this and show dialog to driver.
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.e("Error: ", error.getMessage());
                }
            });
            mRequestQueue.add(req);
        //} else{
        //    Toast.makeText(getApplicationContext(), "Please fill the form, don't leave any field blank", Toast.LENGTH_LONG).show();
        //}


    }

    /**
     * Method that performs RESTful webservice invocations
     *
     * @param params
     */


    /**
     * Method which navigates from Login Activity to Home Activity
     */
    public void navigatetoHomeActivity() {
        Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
        startActivity(homeIntent);
    }

    /**
     * Method gets triggered when Register button is clicked
     *
     * @param view
     */
    public void navigatetoRegisterActivity(View view) {
        Intent loginIntent = new Intent(getApplicationContext(), RegisterActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client2.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Login Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.bradleyhilltopdriver.android.hilltopdriver/http/host/path")
        );
        AppIndex.AppIndexApi.start(client2, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Login Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.bradleyhilltopdriver.android.hilltopdriver/http/host/path")
        );
        AppIndex.AppIndexApi.end(client2, viewAction);
        client2.disconnect();
    }
}