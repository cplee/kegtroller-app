package com.nektos.kegtroller.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by casele on 6/16/14.
 */
public class AuthorizationAPI extends AsyncTask<String, String, String> {

    private final View view;
    private final SharedPreferences sharedPrefs;

    public AuthorizationAPI(View view) {
        this.view = view;
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(view.getContext());
    }

    private String getApiURL() {
        return sharedPrefs.getString("prefApiUrl","http://keg.nektos.com/api/authorization");
    }
    private String getApiKey() {
        return sharedPrefs.getString("prefApiKey","");
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }
    @Override
    protected String doInBackground(String... params) {
        SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);

        String url = getApiURL();
        HttpResponse httpResponse = null;
        if(params[0].equals("get")) {
            swipeLayout.setRefreshing(true);
            try {
                HttpClient httpclient = new DefaultHttpClient();
                httpResponse = httpclient.execute(new HttpGet(url));

            } catch (Exception e) {
                Log.d("InputStream", e.getLocalizedMessage());
            }
        } else if(params[0].equals("unlock")) {
            swipeLayout.setRefreshing(true);
            JSONObject o = new JSONObject();
            try {
                o.put("api_key",getApiKey());
                o.put("confirm",true);
                o.put("ttl",30000);
            } catch (JSONException e) {
            }

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPut put = new HttpPut(url);
                StringEntity entity = new StringEntity(o.toString());
                entity.setContentType("application/json;charset=UTF-8");//text/plain;charset=UTF-8
                entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
                put.setEntity(entity);

                httpResponse = httpclient.execute(put);
            } catch (Exception e) {
                Log.e("InputStream", "Unable to put", e);
            }
        }


        String result = "";
        try {
            InputStream inputStream = null;
            if(httpResponse.getStatusLine().getStatusCode()==200) {
                inputStream = httpResponse.getEntity().getContent();
                if (inputStream != null)
                    result = convertInputStreamToString(inputStream);
                else
                    result = "";
            } else if(httpResponse.getStatusLine().getStatusCode()==401) {
                result = "denied";
            }
        } catch (Exception e) {
            Log.e("InputStream", "Unable to get response", e);
        }

        swipeLayout.setRefreshing(false);
        return result;

    }

    protected void onPostExecute(String result) {

        long ttl = 0;
        try {
            if (result != null && result.length()>0) {
                if(result.equals("denied")) {
                    AlertDialog alertDialog = new AlertDialog.Builder(view.getContext()).create();
                    alertDialog.setTitle("Error!");
                    alertDialog.setMessage("Access denied...check your api_key in settings");
                    alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    alertDialog.show();
                } else {
                    JSONObject json = new JSONObject(result);

                    ttl = json.getLong("ttl");
                }
            }
        } catch (Exception e) {
            Log.e("JSON", e.getLocalizedMessage(), e);
        }


        ImageView image = (ImageView) view.findViewById(R.id.status);
        TextView txt = (TextView)view.findViewById(R.id.statusText);
        Button b = (Button)view.findViewById(R.id.btnUnlock);

        if(ttl > 0) {
            image.setImageResource(R.drawable.green);
            txt.setText("Unlocked");
            b.setVisibility(View.INVISIBLE);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    new AuthorizationAPI(view).execute("get");
                }
            }, ttl);
        } else {
            image.setImageResource(R.drawable.red);
            txt.setText("Locked");
            b.setVisibility(View.VISIBLE);
        }

    }

}
