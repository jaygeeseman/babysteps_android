package com.example.babysteps_android;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String DEBUG_TAG = "HttpExample";
    private TextView resultLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        resultLabel = (TextView) findViewById(R.id.result_label);
        return true;
    }

    /**
     * Responds to Create Account button click
     * 
     * @param view
     */
    public void createAccount(View view) {
        EditText editText = (EditText) findViewById(R.id.username_field);
        String username = editText.getText().toString();

        TextView resultLabel = (TextView) findViewById(R.id.result_label);
        resultLabel.setText(String.format("Trying to create new account for %s...", username));
        
        if (!isNetworkAvailable()) {
            resultLabel.append("No network connection available.");
            return;
        }

        new CreateAccountTask().execute(username, "device_id", "device_type");
    }

    /**
     * Tells if a network connection is available.
     * 
     * @return
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * Uses AsyncTask to create a task away from the main UI thread. This task takes a
     * URL string and uses it to create an HttpUrlConnection. Once the connection
     * has been established, the AsyncTask downloads the contents of the webpage as
     * an InputStream. Finally, the InputStream is converted into a string, which is
     * displayed in the UI by the AsyncTask's onPostExecute method.
     */
    private class CreateAccountTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            // params comes from the execute() call:
            // params[0] is the username
            // params[1] is device_id
            // params[2] is device_type
            try {
                String postBody = getPostJson(params[0], params[1], params[2]);
                String responseBody = doPost(postBody);
                return responseBody;
            }
            catch (IOException e) {
                e.printStackTrace();
                return "Connection failed! Error - " + e.toString();
            }
            catch (JSONException e) {
                e.printStackTrace();
                return "Error parsing JSON: " + e.toString();
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            String userResult = handlePostResponse(result);
            resultLabel.append("\n\n" +userResult);
        }
        
//        @Override
//        protected void onProgressUpdate(String... values) {
//            if (values[0].equals("success")) {
//                // Deactivate submit button
//                ((Button)findViewById(R.id.create_account_button)).setEnabled(false);
//            }
//
//        }

        /**
         * Executes the HTTP POST to tell our service to create a new user account.
         * Returns the response body.
         * @param postBody
         * @return
         * @throws IOException
         */
        private String doPost(String postBody) throws IOException {
            InputStream inputStream = null;
            HttpURLConnection conn = null;

            try {
                // Create the connection
                URL url = new URL("http://10.0.2.2:9000/users");
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.connect();
                
                // Send the POST body
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(postBody);
                out.flush();
                out.close();

                // Get the response
                int response = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response code is: " + response);

                if (response > 199 && response < 300) {
                    inputStream = conn.getInputStream();
                } else {
                    inputStream = conn.getErrorStream();
                }
                
                String contentAsString = inputStreamToString(inputStream);
                Log.d(DEBUG_TAG, "The response body is: " + contentAsString);

                return contentAsString;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        
        /**
         * Reads an InputStream and converts it to a String.
         * @param stream
         * @return
         * @throws IOException
         * @throws UnsupportedEncodingException
         */
        private String inputStreamToString(InputStream stream) throws IOException, UnsupportedEncodingException {
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, "UTF-8");
            return writer.toString();
        }

        /**
         * Parses the JSON response and returns a string result to be displayed to the user
         * @param response
         * @return
         */
        private String handlePostResponse(String response) {
            JSONObject jsonObject = null;
            StringBuilder result = new StringBuilder(response);
            String jsonStatus = null;
            String jsonMessage = null;
            
            try {
                jsonObject = new JSONObject(response);
                
                // This commented block lets us know what's up with JSON parsing
//                Iterator keys = jsonObject.keys();
//                while (keys.hasNext()) {
//                    String key = (String)keys.next();
//                    result.append(String.format("Key: %s Value: %s\n", key, (String)jsonObject.getString(key)));
//                }
            }
            catch (JSONException e) {
                result.append("\n\nError parsing JSON: " + e.toString());
            }

            jsonStatus = jsonObject.optString("status", "");
            jsonMessage = jsonObject.optString("message", "");
            
            if (jsonStatus.equals("") || jsonMessage.equals("")) {
                result.append("\n\nInvalid response. Expected 'status' and 'message' elements. Got " + response);
            } else if (jsonStatus.equals("success")) {
                // Deactivate submit button
                ((Button)findViewById(R.id.create_account_button)).setEnabled(false);
                result.append("\n\nYour account has been created.");
            } else if (jsonStatus.equals("duplicate")) {
                // Select input field
                EditText usernameField = (EditText)findViewById(R.id.username_field);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(usernameField, InputMethodManager.SHOW_IMPLICIT);
                result.append("\n\nThat username already exists. Please try another.");
            } else if (jsonStatus.equals("fail")) {
                result.append("\n\nError. See output above.");
            } else {
                result.append("\n\n" + String.format("Unrecognized status \"%s\"", jsonStatus));
            }
            
            publishProgress(jsonStatus);
            return result.toString();
        }
        
        private String getPostJson(String username, String device_id, String device_type) throws JSONException {
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("device_type", device_type);
            jsonParam.put("device_id", device_id);
            jsonParam.put("username", username);
            return jsonParam.toString();
        }
    }
}
