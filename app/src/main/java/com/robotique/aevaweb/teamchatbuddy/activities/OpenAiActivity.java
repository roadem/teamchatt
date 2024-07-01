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
import android.widget.TextView;

import com.bfr.buddysdk.BuddyCompatActivity;
import com.robotique.aevaweb.teamchatbuddy.R;
import com.robotique.aevaweb.teamchatbuddy.adapters.OpenAiInfoAdapter;
import com.robotique.aevaweb.teamchatbuddy.application.TeamChatBuddyApplication;
import com.robotique.aevaweb.teamchatbuddy.models.OpenAiInfo;

import java.text.DecimalFormat;
import java.util.List;

public class OpenAiActivity extends BuddyCompatActivity {

    private static final String TAG = "OpenAiAct";
    private static final String TAG_LIFE_CYCLE_DEBUG = "OpenAiAct_LIFE_CYCLE";

    private TeamChatBuddyApplication teamChatBuddyApplication;
    private View decorView;

    private RecyclerView list_prices;
    private TextView layout_open_ai_title;
    private TextView header_model_txt;
    private TextView header_input_txt;
    private TextView header_output_txt;
    private TextView total_txt;
    private TextView total_title_txt;



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

        list_prices.setLayoutManager(new LinearLayoutManager(OpenAiActivity.this));
        List<OpenAiInfo> openAiInfoList = teamChatBuddyApplication.parseAndLoadPrices();
        OpenAiInfoAdapter adapter = new OpenAiInfoAdapter(OpenAiActivity.this, openAiInfoList);
        list_prices.setAdapter(adapter);

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
        }
        else if(teamChatBuddyApplication.getLangue().getNom().equals("Espagnol")){
            layout_open_ai_title.setText(R.string.consommation_title_es);
            header_model_txt.setText(R.string.model_es);
            header_input_txt.setText(R.string.input_es);
            header_output_txt.setText(R.string.output_es);
            total_title_txt.setText(R.string.consommation_title_2_es);
        }
        else  if(teamChatBuddyApplication.getLangue().getNom().equals("Allemand")){
            layout_open_ai_title.setText(R.string.consommation_title_de);
            header_model_txt.setText(R.string.model_de);
            header_input_txt.setText(R.string.input_de);
            header_output_txt.setText(R.string.output_de);
            total_title_txt.setText(R.string.consommation_title_2_de);
        }
        else {
            layout_open_ai_title.setText(R.string.consommation_title_en);
            header_model_txt.setText(R.string.model_en);
            header_input_txt.setText(R.string.input_en);
            header_output_txt.setText(R.string.output_en);
            total_title_txt.setText(R.string.consommation_title_2_en);
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