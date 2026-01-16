package com.robotique.aevaweb.teamchatbuddy.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.activities.MainActivity;
import com.robotique.aevaweb.teamchatbuddy.adapters.OpenAiInfoAdapter;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.OpenAiInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OpenAiFragment extends Fragment {

    private static final String TAG_LIFE_CYCLE_DEBUG = "OpenAiAct_LIFE_CYCLE";

    private MainActivity _parentActivity;

    private TeamChatBuddyApplication teamChatBuddyApplication;
    private View decorView;

    private RecyclerView list_prices, models_no_prices_list;
    private RelativeLayout close_open_ai;
    private TextView layout_open_ai_title;
    private TextView header_model_txt;
    private TextView header_input_txt;
    private TextView header_output_txt;
    private TextView total_txt;
    private TextView total_title_txt;
    private TextView list_empty;
    private LinearLayout list_no_prices_header;
    private TextView header_model_name;
    private TextView header_input_token;
    private TextView second_list_title;
    private ImageView refresh_btn;

    private List<OpenAiInfo> openAiInfoList;
    List<OpenAiInfo> otherModels;
    private OpenAiInfoAdapter adapter, adapter1;


    public OpenAiFragment() {
    }

    public OpenAiFragment(MainActivity _parentActivity) {
        this._parentActivity = _parentActivity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
       View view =  inflater.inflate(R.layout.activity_open_ai, container, false);

        Log.i(TAG_LIFE_CYCLE_DEBUG,"onCreate()");

        teamChatBuddyApplication = (TeamChatBuddyApplication) requireActivity().getApplicationContext();

        //Gestion d'affichage des barres du systemUI
        teamChatBuddyApplication.hideSystemUI(requireActivity());
        decorView=requireActivity().getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(teamChatBuddyApplication.hideSystemUI(requireActivity()));
                }
            }
        });

        //init views
        list_prices = view.findViewById(R.id.list_prices);
        layout_open_ai_title = view.findViewById(R.id.layout_open_ai_title);
        header_model_txt = view.findViewById(R.id.header_model_txt);
        header_input_txt = view.findViewById(R.id.header_input_txt);
        header_output_txt = view.findViewById(R.id.header_output_txt);
        total_txt = view.findViewById(R.id.total_txt);
        total_title_txt = view.findViewById(R.id.total_title_txt);
        list_empty = view.findViewById(R.id.list_empty);
        models_no_prices_list = view.findViewById(R.id.models_no_prices_list);
        list_no_prices_header = view.findViewById(R.id.list_no_prices_header);
        header_input_token = view.findViewById(R.id.header_input_token);
        header_model_name = view.findViewById(R.id.header_model_name);
        second_list_title =  view.findViewById(R.id.second_list_title);
        refresh_btn = view.findViewById(R.id.refresh_btn);
        close_open_ai = view.findViewById(R.id.close_open_ai);

        refresh_btn.setOnClickListener(view1 -> refresh(view1));
        close_open_ai.setOnClickListener(view2 -> closeOpenAiPage(view2));

        list_prices.setLayoutManager(new LinearLayoutManager(getActivity()));
        openAiInfoList = teamChatBuddyApplication.parseAndLoadPrices();
        adapter = new OpenAiInfoAdapter(requireActivity(), openAiInfoList, true);
        list_prices.setAdapter(adapter);
        models_no_prices_list.setLayoutManager(new LinearLayoutManager(requireActivity()));
        otherModels = getOtherModels();
        if(otherModels.isEmpty()){
            models_no_prices_list.setVisibility(View.GONE);
            list_no_prices_header.setVisibility(View.GONE);
            list_empty.setVisibility(View.VISIBLE);
        }
        else{
            models_no_prices_list.setVisibility(View.VISIBLE);
            list_no_prices_header.setVisibility(View.VISIBLE);
            list_empty.setVisibility(View.GONE);
            adapter1 = new OpenAiInfoAdapter(requireActivity(), otherModels, false);
            models_no_prices_list.setAdapter(adapter1);
        }

        setLanguageText();

        //Gestion du calcul de la consommation d'openai
        handlerConsommation();
        return  view;
    }

    @Override
    public void onPause() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onPause()");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onResume()");
        super.onResume();
        teamChatBuddyApplication.hideSystemUI(requireActivity());
    }

    @Override
    public void onDestroy() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onDestroy()");
        super.onDestroy();
    }



    /**
     *   -------------------------------  Fonctions utiles  ----------------------------------------------
     */

    public void closeOpenAiPage(View view) {
        ((MainActivity) requireActivity()).navigateTo(new SettingsFragment(_parentActivity), false);
    }

    public void refresh(View view) {
        teamChatBuddyApplication.setparam("Total_cons","0");
        total_txt.setText("0,00");

        for(OpenAiInfo model: openAiInfoList){
            teamChatBuddyApplication.setparam(model.getModelName() + "_outputTokens", String.valueOf(0));
            teamChatBuddyApplication.setparam(model.getModelName() + "_inputTokens", String.valueOf(0));
            teamChatBuddyApplication.setparam(model.getModelName() + "_entryConsumption", String.valueOf(0));
            teamChatBuddyApplication.setparam(model.getModelName() + "_outputConsumption", String.valueOf(0));
        }
        if(adapter!=null){
            adapter.notifyDataSetChanged();
        }

        for(OpenAiInfo model: otherModels){
            teamChatBuddyApplication.setparam(model.getModelName() + "_outputTokens", String.valueOf(0));
            teamChatBuddyApplication.setparam(model.getModelName() + "_inputTokens", String.valueOf(0));
        }
        if(adapter1!=null){
            adapter1.notifyDataSetChanged();
        }

    }

    private List<OpenAiInfo> getOtherModels(){
        List<String> otherModels = new ArrayList<>();
        List<OpenAiInfo> modelsNoPrices = new ArrayList<>();

        List<String> listedModels = openAiInfoList.stream()
                .map(OpenAiInfo::getModelName)
                .collect(Collectors.toList());

        String[] modelKeys = {
                "Model",
                "Whisper_model",
                "emotion_Model"
        };
        // Iterate over model keys and add them to otherModels if not in listedModels
        for (String key : modelKeys) {
            String model = teamChatBuddyApplication.getParamFromFile(key, "TeamChatBuddy.properties");
            if (model != null && !listedModels.contains(model)) {
                otherModels.add(model);
                modelsNoPrices.add(new OpenAiInfo(model, 0, 0, ""));
            }
        }
        return modelsNoPrices;
    }

    private void handlerConsommation(){
        double totalConsumption = 0;
        String totalConsumptionSaved = teamChatBuddyApplication.getparam("Total_cons");
        if(totalConsumptionSaved != null && !totalConsumptionSaved.isEmpty()){
            totalConsumption = Double.parseDouble(totalConsumptionSaved);
        }
        teamChatBuddyApplication.setparam("Total_cons",totalConsumption+"");
        DecimalFormat decimalFormatter = new DecimalFormat("0.00");
        String formattedValue = decimalFormatter.format(totalConsumption);
        total_txt.setText(formattedValue);
    }

    private void setLanguageText(){

        if(teamChatBuddyApplication.getLangue().getNom().equals("Français")){
            layout_open_ai_title.setText(R.string.consommation_title_fr);
            header_model_txt.setText(R.string.model_fr);
            header_input_txt.setText(R.string.input_fr);
            header_output_txt.setText(R.string.output_fr);
            total_title_txt.setText(R.string.consommation_title_2_fr);
            second_list_title.setText(R.string.other_models_fr);
            header_input_token.setText(R.string.number_of_tokens_fr);
            header_model_name.setText(R.string.model_fr);
            list_empty.setText(R.string.section_empty_fr);
        }
        else if(teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")){
            layout_open_ai_title.setText(R.string.consommation_title_es);
            header_model_txt.setText(R.string.model_es);
            header_input_txt.setText(R.string.input_es);
            header_output_txt.setText(R.string.output_es);
            total_title_txt.setText(R.string.consommation_title_2_es);
            second_list_title.setText(R.string.other_models_es);
            header_input_token.setText(R.string.number_of_tokens_es);
            header_model_name.setText(R.string.model_es);
            list_empty.setText(R.string.section_empty_es);
        }
        else  if(teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
            layout_open_ai_title.setText(R.string.consommation_title_de);
            header_model_txt.setText(R.string.model_de);
            header_input_txt.setText(R.string.input_de);
            header_output_txt.setText(R.string.output_de);
            total_title_txt.setText(R.string.consommation_title_2_de);
            second_list_title.setText(R.string.other_models_de);
            header_input_token.setText(R.string.number_of_tokens_de);
            header_model_name.setText(R.string.model_de);
            list_empty.setText(R.string.section_empty_de);
        }
        else {
            layout_open_ai_title.setText(R.string.consommation_title_en);
            header_model_txt.setText(R.string.model_en);
            header_input_txt.setText(R.string.input_en);
            header_output_txt.setText(R.string.output_en);
            total_title_txt.setText(R.string.consommation_title_2_en);
            second_list_title.setText(R.string.other_models_en);
            header_input_token.setText(R.string.number_of_tokens_en);
            header_model_name.setText(R.string.model_en);
            list_empty.setText(R.string.section_empty_en);
        }

    }

}
