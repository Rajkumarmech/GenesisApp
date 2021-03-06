package com.darkweb.genesissearchengine.appManager.setting_manager;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import com.darkweb.genesissearchengine.appManager.home_activity.app_model;
import com.darkweb.genesissearchengine.constants.keys;
import com.darkweb.genesissearchengine.dataManager.preference_manager;
import com.example.myapplication.R;

import static com.darkweb.genesissearchengine.constants.status.*;
import static com.darkweb.genesissearchengine.constants.status.search_status;

public class settings_ehandler
{
    /*Initializations*/

    private static final settings_ehandler ourInstance = new settings_ehandler();

    public static settings_ehandler getInstance()
    {
        return ourInstance;
    }

    private settings_ehandler()
    {
    }

    /*Listeners*/

    private void onJavaScriptListener(int position)
    {
        if(position==1 && setting_model.getInstance().java_status)
        {
            setting_model.getInstance().java_status = false;
            preference_manager.getInstance().setBool(keys.java_script, false);
        }
        else if(!setting_model.getInstance().java_status)
        {
            setting_model.getInstance().java_status = true;
            preference_manager.getInstance().setBool(keys.java_script, true);
        }
    }

    private void onSearchListner(AdapterView<?> parentView,int position)
    {
        if(!setting_model.getInstance().search_status.equals(parentView.getItemAtPosition(position).toString()))
        {
            setting_model.getInstance().search_status = parentView.getItemAtPosition(position).toString();
            preference_manager.getInstance().setString(keys.search_engine, setting_model.getInstance().search_status);
        }
    }

    private void onHistoryListener(int position)
    {
        if(position==1 && setting_model.getInstance().history_status)
        {
            setting_model.getInstance().history_status = false;
            preference_manager.getInstance().setBool(keys.history_clear, false);
        }
        else if(!setting_model.getInstance().history_status)
        {
            setting_model.getInstance().history_status = true;
            preference_manager.getInstance().setBool(keys.history_clear, true);
        }
    }

    void onBackPressed()
    {
        setting_model.getInstance().getSettingInstance().closeView();
    }

    /*Listener Initializations*/

    void onItemListnerInitialization(Spinner view)
    {
        view.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(parentView.getId()== R.id.search_manager)
                {
                    onSearchListner(parentView,position);
                }
                else if(parentView.getId()== R.id.javascript_manager)
                {
                    onJavaScriptListener(position);
                }
                else if(parentView.getId()== R.id.history_manager)
                {
                    onHistoryListener(position);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

}
