package com.jamil.stormy.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jamil.stormy.R;
import com.jamil.stormy.weather.Current;
import com.jamil.stormy.weather.Day;
import com.jamil.stormy.weather.Forecast;
import com.jamil.stormy.weather.Hour;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1492;
    private static final int REQUEST_RESOLVE_ERROR = 123;

    private Forecast mForecast;
    //private TextView mTemperatureLabel;

    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;

    private LocationRequest mLocationRequest;

    private boolean mResolvingError = false;

    /**
     * ButterKnife Alternative to declaring a member variable for each layout view object
     * and then initializing in the onCreate() method.
     */
    @BindView(R.id.timeLabel)
    TextView mTimeLabel;
    @BindView(R.id.temperatureLabel)
    TextView mTemperatureLabel;
    @BindView(R.id.humidityValue)
    TextView mHumidityValue;
    @BindView(R.id.precipValue)
    TextView mPrecipValue;
    @BindView(R.id.summaryLabel)
    TextView mSummaryLabel;
    @BindView(R.id.iconImageView)
    ImageView mIconImageView;
    @BindView(R.id.refreshImageView)
    ImageView mRefreshImageView;
    @BindView(R.id.progressBar)
    ProgressBar mProgressBar;

    // Default Location: New York City
    private final double DEFAULT_LATITUDE = 40.7128;
    private final double DEFAULT_LONGITUDE = -74.0059;

    private double mCurrentLatitude = DEFAULT_LATITUDE;
    private double mCurrentLongitude = DEFAULT_LONGITUDE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind all the BK variables declared above
        ButterKnife.bind(this);

        // Hide the loading circle initially
        mProgressBar.setVisibility(View.INVISIBLE);

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Refreshing info . . . ");
                getForecast(mCurrentLatitude, mCurrentLongitude);
            }
        });

        // getForecast(latitude, longitude);
        Log.d(TAG, "Main UI code is running");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        Log.d(TAG, "Creating location request");
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * 1000)
                .setFastestInterval(1 * 1000);

    } // OnCreate()

    private void getForecast(double latitude, double longitude) {
        // Construct URL for communicating with the API
        String apiKey = "4445fb7afe5d2d4bc20034b83aa8e9bc";
        String forecastUrl = "https://api.darksky.net/forecast/"
                + apiKey + "/" + latitude + "," + longitude;

        Log.d(TAG, "Forecast URL: " + forecastUrl);
        if (isNetworkAvailable()) {
            // Toggle the visibility of the refresh button
            toggleRefresh();

            // Create  OkHTTP client
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();
            Call call = client.newCall(request);

            // Enqueue puts in another thread that isn't he main UI thread
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d(TAG, "Failed making request");
                    // Run UI updates on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        // Try to parse the JSON response
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);

                        if (response.isSuccessful()) {
                            Log.d(TAG, "Response was successful!");
                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            Log.d(TAG, "Response was not successful");
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else {
            Toast.makeText(this, R.string.network_unavailable_message,
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "No Network.");
        }
    }

    private void toggleRefresh() {
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }

    }

    private void updateDisplay() {
        Current current = mForecast.getCurrent();
        Log.d(TAG, "Updating the display now . . . ");

        // Update the Main layout with the data from the current weather conditions
        mTemperatureLabel.setText("" + current.getTemperature());
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText("" + current.getHumidity());
        mPrecipValue.setText(current.getPrecipChance() + "%");
        mSummaryLabel.setText(current.getSummary());

        Drawable drawable = getResources().getDrawable(current.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException {
        // Parse out all the relevant info from the JSON response and populate the Forecast object
        Forecast forecast = new Forecast();
        forecast.setCurrent(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));
        return forecast;
    }

    private Day[] getDailyForecast(String jsonData) throws JSONException {
        Log.d(TAG, "Begin parsing daily forecast");
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject daily = forecast.getJSONObject("daily");
        JSONArray data = daily.getJSONArray("data");
        Day[] days = new Day[data.length()];

        for (int i = 0; i < data.length(); ++i) {
            JSONObject jsonDay = data.getJSONObject(i);
            Day day = new Day();
            day.setSummary(jsonDay.getString("summary"));
            day.setIcon(jsonDay.getString("icon"));
            day.setTemperatureMax(jsonDay.getDouble("temperatureMax"));
            day.setTime(jsonDay.getLong("time"));
            day.setTimezone(timezone);

            days[i] = day;
        }
        return days;
    }

    private Hour[] getHourlyForecast(String jsonData) throws JSONException {
        Log.d(TAG, "Begin parsing hourly forecast");
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");

        JSONObject hourly = forecast.getJSONObject("hourly");
        JSONArray data = hourly.getJSONArray("data");
        Hour[] hours = new Hour[data.length()];

        // Populate an array of Hour objects created from the "hourly" property of the String
        // JSON response
        for (int i = 0; i < data.length(); ++i) {
            JSONObject jsonHour = data.getJSONObject(i);
            Hour hour = new Hour();
            hour.setSummary(jsonHour.getString("summary"));
            hour.setTemperature(jsonHour.getDouble("temperature"));
            hour.setIcon(jsonHour.getString("icon"));
            hour.setTimezone(timezone);
            hour.setTime(jsonHour.getLong("time"));
            hours[i] = hour;
        }

        return hours;
    }

    private Current getCurrentDetails(String jsonData) throws JSONException {
        // Create a JSONObject from the String response
        JSONObject forecast = new JSONObject(jsonData);
        String timezone = forecast.getString("timezone");
        Log.i(TAG, "From JSON: " + timezone);

        // Extract data from the "currently" property of the JSONOBject
        JSONObject currently = forecast.getJSONObject("currently");

        // Create a Current object to store these current conditions
        Current current = new Current();
        current.setHumidity(currently.getDouble("humidity"));
        current.setTime(currently.getLong("time"));
        current.setIcon(currently.getString("icon"));
        current.setPrecipChance(currently.getDouble("precipProbability"));
        current.setSummary(currently.getString("summary"));
        current.setTemperature(currently.getDouble("temperature"));
        current.setTimeZone(timezone);

        Log.d(TAG, "Current Time: " + current.getFormattedTime());
        return current;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) { // network is present and available
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError() {
        Log.d(TAG, "About to send dialog to user");
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }

    // BK way of creating a listener on a view object
    @OnClick(R.id.dailyButton)
    public void startDailyActivity(View view) {
        Log.d(TAG, "Starting DailyActivity");

        // Create an intent and pass along the daily forecast data to display in the next activity
        Intent intent = new Intent(this, DailyForecastActivity.class);
        intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        startActivity(intent);
    }

    @OnClick(R.id.hourlyButton)
    public void startHourlyActivity(View view) {
        Log.d(TAG, "Starting Hourly Activity");
        Intent intent = new Intent(this, HourlyForecastActivity.class);
        intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());
        startActivity(intent);
    }

    protected void onResume()
    {
        super.onResume();
        mGoogleApiClient.connect();
    }

    protected void onPause()
    {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Do not currently have permission");
            ActivityCompat.requestPermissions(
                this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            );
            return;
        }

        Log.d(TAG, "The app has sufficient permissions. Great.");

        // Permission granted, continue as usual
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d(TAG, mLastLocation.toString());
            mCurrentLatitude = mLastLocation.getLatitude();
            mCurrentLongitude = mLastLocation.getLongitude();
            Log.d(TAG, "Latitude: " + mLastLocation.getLatitude());
            Log.d(TAG, "Longitude: " + mLastLocation.getLongitude());
            getForecast(mCurrentLatitude, mCurrentLongitude);
        } else {
            Log.d(TAG, "Last location is null! :-(");
            Log.d(TAG, "Requesting an update");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            //showErrorDialog(connectionResult.getErrorCode());
            Log.d(TAG, "Error: " + connectionResult.getErrorMessage());
            mResolvingError = true;
        }
    }

    // Called when user has decided on the location permission of the app
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        Log.d(TAG, "Request for permission granted");
        if(requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    Log.d(TAG, "Permission Granted.");
                    mCurrentLatitude = mLastLocation.getLatitude();
                    mCurrentLongitude = mLastLocation.getLongitude();
                    Log.d(TAG, "Latitude: " + mLastLocation.getLatitude());
                    Log.d(TAG, "Longitude: " + mLastLocation.getLongitude());
                    getForecast(mCurrentLatitude, mCurrentLongitude);
                } else {
                    // Permission denied, use default location
                    getForecast(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Received a new location");
        Log.d(TAG, location.toString());
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mCurrentLatitude = mLastLocation.getLatitude();
        mCurrentLongitude = mLastLocation.getLongitude();
        Log.d(TAG, "Latitude: " + mLastLocation.getLatitude());
        Log.d(TAG, "Longitude: " + mLastLocation.getLongitude());
        getForecast(mCurrentLatitude, mCurrentLongitude);
    }
}
