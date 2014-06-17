package com.nektos.kegtroller.app;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
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


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            new AuthorizationAPI(rootView).execute("get");

            ((Button) rootView.findViewById(R.id.btnRefresh)).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new AuthorizationAPI(rootView).execute("get");
                        }
                    }
            );

            ((Button) rootView.findViewById(R.id.btnUnlock)).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            new AuthorizationAPI(rootView).execute("unlock");
                        }
                    }
            );
            return rootView;
        }


    }
    private static class AuthorizationAPI extends AsyncTask<String, String, String> {

        private final View view;

        public AuthorizationAPI(View view) {
            this.view = view;
        }

        private String getBaseURL() {
            return "http://keg.nektos.com";
        }
        private String getApiKey() {
            return "pourme";
        }
        @Override
        protected String doInBackground(String... params) {
            String url = getBaseURL() + "/api/authorization";
            HttpResponse httpResponse = null;
            if(params[0].equals("get")) {
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    httpResponse = httpclient.execute(new HttpGet(url));

                } catch (Exception e) {
                    Log.d("InputStream", e.getLocalizedMessage());
                }
            } else if(params[0].equals("unlock")) {
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
                }
            } catch (Exception e) {
                Log.e("InputStream", "Unable to get response", e);
            }
            return result;

        }

        protected void onPostExecute(String result) {

            long ttl = 0;
            try {
                if (result != null && result.length()>0) {
                    JSONObject json = new JSONObject(result);

                    ttl = json.getLong("ttl");
                }
            } catch (Exception e) {
                Log.e("JSON", e.getLocalizedMessage(), e);
            }


            ImageView image = (ImageView) view.findViewById(R.id.status);
            TextView txt = (TextView)view.findViewById(R.id.statusText);

            if(ttl > 0) {
                image.setImageResource(R.drawable.green);
                txt.setText("Unlocked");

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
            }

        }

    }
}
