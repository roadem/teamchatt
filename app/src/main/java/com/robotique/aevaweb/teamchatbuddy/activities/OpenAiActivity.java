package com.robotique.aevaweb.teamchatbuddy.activities;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bfr.buddysdk.BuddyCompatActivity;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.adapters.OpenAiInfoAdapter;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.OpenAiInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OpenAiActivity extends BuddyCompatActivity {

    private static final String TAG = "OpenAiAct";
    private static final String TAG_LIFE_CYCLE_DEBUG = "OpenAiAct_LIFE_CYCLE";

    private TeamChatBuddyApplication teamChatBuddyApplication;
    private View decorView;

    private RecyclerView list_prices, models_no_prices_list;
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

    private List<OpenAiInfo> openAiInfoList;
    List<OpenAiInfo> otherModels;
    private OpenAiInfoAdapter adapter, adapter1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_ai);

        teamChatBuddyApplication = (TeamChatBuddyApplication) getApplicationContext();

        //Gestion d'affichage des barres du systemUI
        teamChatBuddyApplication.hideSystemUI(this);
        decorView=getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if(visibility==0){
                    decorView.setSystemUiVisibility(teamChatBuddyApplication.hideSystemUI(OpenAiActivity.this));
                }
            }
        });

        //init views
        list_prices = findViewById(R.id.list_prices);
        layout_open_ai_title = findViewById(R.id.layout_open_ai_title);
        header_model_txt = findViewById(R.id.header_model_txt);
        header_input_txt = findViewById(R.id.header_input_txt);
        header_output_txt = findViewById(R.id.header_output_txt);
        total_txt = findViewById(R.id.total_txt);
        total_title_txt = findViewById(R.id.total_title_txt);
        list_empty = findViewById(R.id.list_empty);
        models_no_prices_list = findViewById(R.id.models_no_prices_list);
        list_no_prices_header = findViewById(R.id.list_no_prices_header);
        header_input_token = findViewById(R.id.header_input_token);
        header_model_name = findViewById(R.id.header_model_name);
        second_list_title =  findViewById(R.id.second_list_title);

        list_prices.setLayoutManager(new LinearLayoutManager(OpenAiActivity.this));
        openAiInfoList = teamChatBuddyApplication.parseAndLoadPrices();
        adapter = new OpenAiInfoAdapter(OpenAiActivity.this, openAiInfoList, true);
        list_prices.setAdapter(adapter);
        models_no_prices_list.setLayoutManager(new LinearLayoutManager(OpenAiActivity.this));
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
            adapter1 = new OpenAiInfoAdapter(OpenAiActivity.this, otherModels, false);
            models_no_prices_list.setAdapter(adapter1);
        }


        setLanguageText();

        //Gestion du calcul de la consommation d'openai
        handlerConsommation();
    }


    @Override
    protected void onPause() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onPause()");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onResume()");
        super.onResume();
        teamChatBuddyApplication.hideSystemUI(this);
    }

    @Override
    protected void onStart() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onStart()");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG_LIFE_CYCLE_DEBUG,"onStop()");
        super.onStop();
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
        finish();
        overridePendingTransition(0, 0);
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



    /**
     * Cacher les barres systemUI
     */

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            teamChatBuddyApplication.hideSystemUI(this);
        }
    }


    /**
     * Fermer le clavier lorsqu'on clique en dehors
     */

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }


}