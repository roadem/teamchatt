package com.robotique.aevaweb.teamchatbuddy.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.ChatBot;

import java.util.List;

public class ChatbotSpinnerAdapter extends BaseAdapter {
    private final LayoutInflater layoutInflater;
    private List<ChatBot> chatBots;
    private final int listItemLayoutResource;
    private final int itemNameId;
    private final int itemCheckedId;
    private TeamChatBuddyApplication teamChatBuddyApplication;

    public ChatbotSpinnerAdapter(Context context, int listItemLayoutResource, int itemNameId, int itemCheckedId, List<ChatBot> chatBots) {
        this.listItemLayoutResource = listItemLayoutResource;
        this.itemNameId = itemNameId;
        this.itemCheckedId = itemCheckedId;
        this.chatBots = chatBots;
        this.layoutInflater = LayoutInflater.from(context);
        teamChatBuddyApplication = (TeamChatBuddyApplication) context;
    }

    @Override
    public int getCount() {
        if (this.chatBots == null) {
            return 0;
        }
        return this.chatBots.size();
    }

    @Override
    public Object getItem(int position) {
        return this.chatBots.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatBot chatBot = (ChatBot) getItem(position);
        View rowView = this.layoutInflater.inflate(this.listItemLayoutResource, null, true);
        TextView textViewItemName = (TextView) rowView.findViewById(this.itemNameId);
        String nameToDisplay = chatBot.getNom();

        textViewItemName.setGravity(Gravity.CENTER_HORIZONTAL);
        textViewItemName.setText(nameToDisplay);
        ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
        checkedIcon.setVisibility(View.GONE);
        return rowView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        ChatBot chatBot = (ChatBot) getItem(position);
        View rowView = this.layoutInflater.inflate(this.listItemLayoutResource, null, true);
        TextView textViewItemName = (TextView) rowView.findViewById(this.itemNameId);
        textViewItemName.setText(chatBot.getNom());
        ImageView checkedIcon = (ImageView) rowView.findViewById(itemCheckedId);
        if (chatBot.isChosen()) {
            checkedIcon.setVisibility(View.VISIBLE);
        } else {
            checkedIcon.setVisibility(View.INVISIBLE);
        }
        return rowView;
    }

    public void updateDataSet(List<ChatBot> data) {
        this.chatBots = data;
        notifyDataSetChanged();
    }
}
