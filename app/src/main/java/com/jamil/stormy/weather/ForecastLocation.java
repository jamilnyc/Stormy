package com.jamil.stormy.weather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by jamil on 2/11/17.
 */

public class ForecastLocation {
    private static final String TAG = ForecastLocation.class.getSimpleName();

    private double mLatitude;
    private double mLongitude;
    private Context mContext;

    public static final String DEFAULT_LOCATION_NAME = "Unnamed Location";

    public ForecastLocation(double latitude, double longitude, Context context)
    {
        mLatitude = latitude;
        mLongitude = longitude;
        mContext = context;
    }

    public String getLocationName()
    {
        String locationName = "";
        try {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1);
            if (addresses.size() > 0) {
                locationName = addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            Log.d(TAG, "Unable to determine location name");
        }

        return (locationName == null || locationName.isEmpty()) ?  DEFAULT_LOCATION_NAME : locationName;
    }

    public double getLatitude() {
        return mLatitude;
    }

    // TODO: Check if coordinates are valid lat/long values
    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }
}
