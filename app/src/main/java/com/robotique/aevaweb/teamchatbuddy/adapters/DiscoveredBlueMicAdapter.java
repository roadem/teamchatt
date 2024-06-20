package com.robotique.aevaweb.teamchatbuddy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.BlueMicDevice;

import java.util.List;

public class DiscoveredBlueMicAdapter extends BaseAdapter {

    private final LayoutInflater layoutInflater;
    List<BlueMicDevice> blueMicDeviceList;
    private final int listItemLayoutResource;
    private TeamChatBuddyApplication teamChatBuddyApplication;

    public DiscoveredBlueMicAdapter(Context context, int listItemLayoutResource, List<BlueMicDevice> blueMicDeviceList) {
        this.listItemLayoutResource = listItemLayoutResource;
        this.blueMicDeviceList = blueMicDeviceList;
        this.layoutInflater = LayoutInflater.from(context);
        teamChatBuddyApplication = (TeamChatBuddyApplication) context;
    }

    @Override
    public int getCount() {
        if (this.blueMicDeviceList == null) {
            return 0;
        }
        return this.blueMicDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.blueMicDeviceList.get(position);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        BlueMicDevice blueMicDevice = (BlueMicDevice) getItem(position);
        View rowView = this.layoutInflater.inflate(this.listItemLayoutResource, null, true);

        TextView textViewItemName = (TextView) rowView.findViewById(R.id.bluemic_name);
        TextView textViewItemTag = (TextView) rowView.findViewById(R.id.bluemic_tag);

        textViewItemName.setText(blueMicDevice.getName());
        textViewItemTag.setText(blueMicDevice.getTag());

        return rowView;
    }

}
