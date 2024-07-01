package com.robotique.aevaweb.teamchatbuddy.adapters;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
    DecimalFormat decimalFormatter = new DecimalFormat("0.#####");

    public OpenAiInfoAdapter(Activity activity, List<OpenAiInfo> openAiInfoList) {
        this.openAiInfoList = openAiInfoList;
        this.activity = activity;
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
        OpenAiInfo info = openAiInfoList.get(position);
        holder.modelName.setText(info.getModelName());
        holder.inputPrice.setText(decimalFormatter.format(info.getInputPrice()));
        holder.outputPrice.setText(decimalFormatter.format(info.getOutputPrice()));
        holder.additionalInfo.setText(info.getAdditionalInfo());

        holder.inputPrice.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        holder.inputPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String text = s.toString().replace(",", ".");
                    double newInputPrice = Double.parseDouble(text);
                    info.setInputPrice(newInputPrice);
                    teamChatBuddyApplication.setparam(info.getModelName() + "_inputPrice", String.valueOf(newInputPrice));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        holder.inputPrice.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = activity.getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                activity.getWindow().setNavigationBarColor(0x80000000); // color black + opacity 50%
            } else {
                teamChatBuddyApplication.hideSystemUI(activity);
            }
        });

        holder.outputPrice.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
        holder.outputPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String text = s.toString().replace(",", ".");
                    double newOutputPrice = Double.parseDouble(text);
                    info.setOutputPrice(newOutputPrice);
                    teamChatBuddyApplication.setparam(info.getModelName() + "_outputPrice", String.valueOf(newOutputPrice));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        holder.outputPrice.setOnFocusChangeListener((v,hasFocus) -> {
            if (hasFocus) {
                View decorView = activity.getWindow().getDecorView();
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                activity.getWindow().setNavigationBarColor(0x80000000); // color black + opacity 50%
            } else {
                teamChatBuddyApplication.hideSystemUI(activity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return openAiInfoList.size();
    }

    class OpenAiInfoViewHolder extends RecyclerView.ViewHolder {
        TextView modelName, additionalInfo;
        EditText inputPrice, outputPrice;

        public OpenAiInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            modelName = itemView.findViewById(R.id.modelName);
            inputPrice = itemView.findViewById(R.id.inputPrice);
            outputPrice = itemView.findViewById(R.id.outputPrice);
            additionalInfo = itemView.findViewById(R.id.additionalInfo);
        }
    }
}
