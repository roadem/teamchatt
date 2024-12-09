package com.robotique.aevaweb.teamchatbuddy.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.OpenAiInfo;

import java.text.DecimalFormat;
import java.util.List;

public class OpenAiInfoAdapter extends RecyclerView.Adapter<OpenAiInfoAdapter.OpenAiInfoViewHolder> {

    private final TeamChatBuddyApplication teamChatBuddyApplication;
    private final Activity activity;
    private final List<OpenAiInfo> openAiInfoList;
    private final boolean modelsWithPrices;

    public OpenAiInfoAdapter(Activity activity, List<OpenAiInfo> openAiInfoList, boolean modelsWithPrices) {
        this.openAiInfoList = openAiInfoList;
        this.activity = activity;
        this.modelsWithPrices = modelsWithPrices;
        teamChatBuddyApplication = (TeamChatBuddyApplication) activity.getApplicationContext();
    }

    @NonNull
    @Override
    public OpenAiInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_openai_info, parent, false);
        return new OpenAiInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OpenAiInfoViewHolder holder, int position) {

        DecimalFormat decimalFormatter = new DecimalFormat("0.00");

        OpenAiInfo info = openAiInfoList.get(position);
        holder.modelName.setText(info.getModelName());

        if(modelsWithPrices){

            holder.layout_models_no_prices.setVisibility(View.GONE);
            holder.layout_models_prices.setVisibility(View.VISIBLE);
            String _outputTokens = teamChatBuddyApplication.getparam(info.getModelName() +"_outputTokens");
            String _inputTokens = teamChatBuddyApplication.getparam(info.getModelName() +"_inputTokens");
            String _entryConsumption = teamChatBuddyApplication.getparam(info.getModelName() +"_entryConsumption");
            String _outputConsumption = teamChatBuddyApplication.getparam(info.getModelName() +"_outputConsumption");
            if(info.getModelName().equals("whisper-1")){
                holder.outputConsumption.setVisibility(View.GONE);
                holder.outputTokens.setVisibility(View.GONE);
                holder.entryTokens.setText("0 minutes");
                holder.entryConsumption.setText("0,00 $");
                if(_inputTokens !=null && !_inputTokens.isEmpty()){
                    holder.entryTokens.setText(Double.parseDouble(_inputTokens)+" minutes");
                }
                if(_entryConsumption !=null && !_entryConsumption.isEmpty()){
                    holder.entryConsumption.setText(decimalFormatter.format(Double.parseDouble(_entryConsumption))+" $");
                }
            }
            else{
                holder.outputTokens.setText("0 tokens");
                holder.entryTokens.setText("0 tokens");
                holder.entryConsumption.setText("0,00 $");
                holder.outputConsumption.setText("0,00 $");

                holder.outputConsumption.setVisibility(View.VISIBLE);
                holder.outputTokens.setVisibility(View.VISIBLE);

                if(_outputTokens !=null && !_outputTokens.isEmpty()){
                    holder.outputTokens.setText(Double.parseDouble(_outputTokens)+" tokens");
                }
                if(_inputTokens !=null && !_inputTokens.isEmpty()){
                    holder.entryTokens.setText(Double.parseDouble(_inputTokens)+" tokens");
                }
                if(_entryConsumption !=null && !_entryConsumption.isEmpty()){
                    holder.entryConsumption.setText(decimalFormatter.format(Double.parseDouble(_entryConsumption))+" $");
                }
                if(_outputConsumption !=null && !_outputConsumption.isEmpty()){
                    holder.outputConsumption.setText(decimalFormatter.format(Double.parseDouble(_outputConsumption))+" $");
                }
            }

        }
        else {

            holder.layout_models_no_prices.setVisibility(View.VISIBLE);
            holder.layout_models_prices.setVisibility(View.GONE);
            holder.model_name.setText(info.getModelName());
            String _outputTokens = teamChatBuddyApplication.getparam(info.getModelName() +"_outputTokens");
            String _inputTokens = teamChatBuddyApplication.getparam(info.getModelName() +"_inputTokens");
            if(_outputTokens !=null && !_outputTokens.isEmpty()){
                holder.token_number_output.setText(Double.parseDouble(_outputTokens)+"");
            }
            if(_inputTokens !=null && !_inputTokens.isEmpty()){
                holder.token_number_entry.setText(Double.parseDouble(_inputTokens)+"");
            }
        }
    }

    @Override
    public int getItemCount() {
        return openAiInfoList.size();
    }

    class OpenAiInfoViewHolder extends RecyclerView.ViewHolder {
        TextView modelName, outputTokens, entryTokens, entryConsumption, outputConsumption,token_number_entry,model_name, token_number_output;
        LinearLayout layout_models_no_prices, layout_models_prices;

        public OpenAiInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.modelName);
            entryTokens = itemView.findViewById(R.id.entryTokens);
            entryConsumption = itemView.findViewById(R.id.entryConsumption);
            outputTokens = itemView.findViewById(R.id.outputTokens);
            outputConsumption = itemView.findViewById(R.id.outputConsumption);
            layout_models_no_prices = itemView.findViewById(R.id.layout_models_no_prices);
            layout_models_prices = itemView.findViewById(R.id.layout_models_prices);
            token_number_entry = itemView.findViewById(R.id.token_number_entry);
            model_name = itemView.findViewById(R.id.model_name);
            token_number_output = itemView.findViewById(R.id.token_number_output);


        }
    }
}
