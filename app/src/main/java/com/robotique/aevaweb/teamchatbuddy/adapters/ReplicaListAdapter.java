package com.robotique.aevaweb.teamchatbuddy.adapters;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.Replica;

public class ReplicaListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Replica[] mDataset;
    int item_send=1;
    int item_receive=2;
    private TeamChatBuddyApplication teamChatBuddyApplication;

    public static class SentViewHolder extends RecyclerView.ViewHolder {

        private TextView sentmessage;
        public SentViewHolder(View itemView, TeamChatBuddyApplication teamChatBuddyApplication){
            super(itemView);
            sentmessage =itemView.findViewById(R.id.txt_sent_message);
            sentmessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, teamChatBuddyApplication.getTextSizeBullesPX());

        }
        public TextView getTextView() {
            return sentmessage;
        }
    }
    public static class ReceiveViewHolder extends RecyclerView.ViewHolder {

        private TextView receivemessage;
        private TextView messageDuration;

        public ReceiveViewHolder( View itemView,TeamChatBuddyApplication teamChatBuddyApplication){
            super(itemView);
            receivemessage =itemView.findViewById(R.id.txt_receive_message);
            messageDuration = itemView.findViewById(R.id.txt_response_time);
            receivemessage.setTextSize(TypedValue.COMPLEX_UNIT_PX, teamChatBuddyApplication.getTextSizeBullesPX());
            messageDuration.setTextSize(TypedValue.COMPLEX_UNIT_PX, teamChatBuddyApplication.getTextSizeBullesPX());

        }

        public TextView getTextView() {
            return receivemessage;
        }
    }


    public ReplicaListAdapter(TeamChatBuddyApplication teamChatBuddyApplication,Replica[] dataSet) {
        mDataset=dataSet;
        this.teamChatBuddyApplication = teamChatBuddyApplication;
    }

    public void setData(Replica[] newdata){
        mDataset=newdata;
        notifyDataSetChanged();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType==1){
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_send, parent, false);
            return new SentViewHolder(view,this.teamChatBuddyApplication);}
        else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_receive, parent, false);
            return new ReceiveViewHolder(view,this.teamChatBuddyApplication);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getClass()==SentViewHolder.class){
            ((SentViewHolder) holder).sentmessage.setText(mDataset[position].getValue());

        }
        else if (holder.getClass()==ReceiveViewHolder.class){
            ((ReceiveViewHolder) holder).receivemessage.setText(mDataset[position].getValue());
            ((ReceiveViewHolder) holder).messageDuration.setText(mDataset[position].getDuration());
        }

    }

    @Override
    public int getItemViewType(int position) {

        if (mDataset[position].getType().equals("question") ){
            return item_send;
        }
        else {
            return item_receive;
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }

}
