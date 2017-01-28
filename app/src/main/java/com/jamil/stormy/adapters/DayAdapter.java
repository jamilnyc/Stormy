package com.jamil.stormy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jamil.stormy.R;
import com.jamil.stormy.weather.Day;

public class DayAdapter extends BaseAdapter {

    // The context is the Activity that we are coming from
    private Context mContext;
    private Day[] mDays;

    public DayAdapter(Context context, Day[] days) {
        mContext = context;
        mDays = days;
    }

    @Override
    public int getCount() {
        return mDays.length;
    }

    @Override
    public Object getItem(int position) {
        return mDays[position];
    }

    @Override
    public long getItemId(int position) {
        return 0; // Not used. Used to tag items for easy reference
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Called when list is initially displayed and each time we scroll a new item onto the list.
        // This helps use memory and power efficiently

        // convertView is the object that is reused
        // First time it is null, otherwise we reset the data

        ViewHolder holder;
        if (convertView == null) {
            // New view and everything needs to created

            // Converts an xml layout to a view object
            convertView = LayoutInflater.from(mContext).inflate(R.layout.daily_list_item, null);

            holder = new ViewHolder();
            holder.iconImageView = (ImageView) convertView.findViewById(R.id.iconImageView);
            holder.temperatureLabel = (TextView) convertView.findViewById(R.id.temperatureLabel);
            holder.dayLabel = (TextView) convertView.findViewById(R.id.dayNameLabel);
            holder.circleImageView = (ImageView) convertView.findViewById(R.id.circleImageView);

            convertView.setTag(holder); // Sets a tag for the view that can be reused

        } else {
            // Get the existing holder that was previously set up.
            holder = (ViewHolder) convertView.getTag();
        }

        Day day = mDays[position];
        holder.iconImageView.setImageResource(day.getIconId());
        holder.temperatureLabel.setText("" + day.getTemperatureMax());

        // Set the first day to show "Today"
        if (position == 0) {
            holder.dayLabel.setText("Today");
        } else {
            holder.dayLabel.setText(day.getDayOfTheWeek());
        }
        holder.circleImageView.setImageResource(R.drawable.bg_temperature);

        return convertView;
    }

    // Custom View Holder class
    // Helper object associated with a view lets us reuse
    // the same references to objects in the view like text views and images
    // Holds the views added to our list item layout

    private static class ViewHolder {
        ImageView iconImageView;
        TextView temperatureLabel;
        TextView dayLabel;
        ImageView circleImageView;
    }
}
