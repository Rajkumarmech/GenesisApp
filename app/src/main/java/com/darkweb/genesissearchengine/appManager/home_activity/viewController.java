package com.darkweb.genesissearchengine.appManager.home_activity;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.MenuCompat;
import com.darkweb.genesissearchengine.constants.*;
import com.darkweb.genesissearchengine.dataManager.preference_manager;
import com.darkweb.genesissearchengine.helperMethod;
import com.darkweb.genesissearchengine.httpManager.serverRequestManager;
import com.darkweb.genesissearchengine.pluginManager.admanager;
import com.darkweb.genesissearchengine.pluginManager.message_manager;
import com.darkweb.genesissearchengine.pluginManager.orbot_manager;
import com.example.myapplication.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class viewController
{
    /*ViewControllers*/
    private WebView webView;
    private ProgressBar progressBar;
    private AutoCompleteTextView searchbar;
    private ConstraintLayout splashScreen;
    private ConstraintLayout requestFailure;
    private FloatingActionButton floatingButton;
    private ImageView loading;
    private ImageView splashlogo;
    private TextView loadingText;

    /*Private Variables*/
    private boolean pageLoadedSuccessfully = true;
    private boolean isSplashLoading = false;
    private Handler updateUIHandler = null;

    /*ProgressBar Delayed Updater*/
    Handler progressBarHandler = null;

    /*Initializations*/
    private static final viewController ourInstance = new viewController();

    public static viewController getInstance()
    {
        return ourInstance;
    }

    private viewController()
    {
    }

    void initialization(WebView webView1, TextView loadingText, ProgressBar progressBar, AutoCompleteTextView searchbar, ConstraintLayout splashScreen, ConstraintLayout requestFailure, FloatingActionButton floatingButton, ImageView loading, ImageView splashlogo)
    {
        this.webView = webView1;
        this.progressBar = progressBar;
        this.searchbar = searchbar;
        this.splashScreen = splashScreen;
        this.requestFailure = requestFailure;
        this.floatingButton = floatingButton;
        this.loading = loading;
        this.splashlogo = splashlogo;
        this.loadingText = loadingText;

        app_model.getInstance().getAppInstance().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        checkSSLTextColor();
        initSplashScreen();
        initLock();
        initViews();
        createUpdateUiHandler();
        initializeSuggestionView();
    }

    private void initializeSuggestionView()
    {
        AutoCompleteAdapter suggestionAdapter = new AutoCompleteAdapter(app_model.getInstance().getAppInstance(), R.layout.hint_view, R.id.hintCompletionHeader, app_model.getInstance().getSuggestions());

        int width = Math.round(helperMethod.screenWidth());
        searchbar.setThreshold(2);
        searchbar.setAdapter(suggestionAdapter);
        searchbar.setDropDownVerticalOffset(22);
        searchbar.setDropDownWidth(Math.round(width*0.95f));
        searchbar.setDropDownHorizontalOffset(Math.round(width*0.114f)*-1);

        Drawable drawable;
        Resources res = app_model.getInstance().getAppInstance().getResources();
        try {
            drawable = Drawable.createFromXml(res, res.getXml(R.xml.rouned_corner));
            searchbar.setDropDownBackgroundDrawable(drawable);
        } catch (Exception ex) {
            Log.i("sdfsdf", ex.getMessage());
        }

    }

    void reInitializeSuggestion()
    {
        initializeSuggestionView();
    }

    private boolean isHiddenView()
    {
        return app_model.getInstance().getAppInstance().isGeckoViewRunning();
    }

    private void initViews()
    {
        floatingButton.setVisibility(View.INVISIBLE);
    }

    private void initLock()
    {
        Drawable img = app_model.getInstance().getAppInstance().getResources().getDrawable( R.drawable.icon_lock);
        searchbar.measure(0, 0);
        img.setBounds( 0, (int)(searchbar.getMeasuredHeight()*0.00), (int)(searchbar.getMeasuredHeight()*1.10), (int)(searchbar.getMeasuredHeight()*0.69) );
        searchbar.setCompoundDrawables( img, null, null, null );
    }

    void onRequestTriggered(boolean isHiddenWeb, String url)
    {
        onProgressBarUpdate(4);
        helperMethod.hideKeyboard();
        pageLoadedSuccessfully = true;
        onUpdateSearchBar(url);
        checkSSLTextColor();
        onClearSearchBarCursor();

        searchbar.setFocusableInTouchMode(false);
        searchbar.setFocusable(false);
        searchbar.setFocusableInTouchMode(true);
        searchbar.setFocusable(true);
    }

    /*Helper Methods*/
    void onInternetError()
    {
        disableSplashScreen();
        requestFailure.setVisibility(View.VISIBLE);
        webView.setAlpha(0);
        requestFailure.animate().alpha(1f).setDuration(150);
        pageLoadedSuccessfully = false;
        onClearSearchBarCursor();
        onProgressBarUpdate(0);
        disableFloatingView();
        app_model.getInstance().getAppInstance().releaseSession();
    }

    private void disableSplashScreen()
    {
        if(!isSplashLoading)
        {
            isSplashLoading = true;
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        boolean isFirstInstall = preference_manager.getInstance().getBool(keys.hasOrbotInstalled,true);
                        while (!status.isTorInitialized && (isFirstInstall || status.search_status.equals(enums.searchEngine.Google.toString()) || status.search_status.equals(enums.searchEngine.Bing.toString())))
                        {
                            startPostTask(messages.UPDATE_LOADING_TEXT);
                            sleep(100);
                        }
                        preference_manager.getInstance().setBool(keys.hasOrbotInstalled,false);
                        startPostTask(messages.DISABLE_SPLASH_SCREEN);
                    }
                    catch (Exception ex)
                    {
                        Log.i("Fizza",ex.getMessage());
                    }
                }
            }.start();
        }
    }

    public void startPostTask(int m_id)
    {
        Message message = new Message();
        message.what = m_id;
        updateUIHandler.sendMessage(message);
    }

    @SuppressLint("HandlerLeak")
    private void createUpdateUiHandler()
    {
                updateUIHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(msg.what == messages.UPDATE_LOADING_TEXT)
                {
                    loadingText.setText(orbot_manager.getInstance().getLogs());
                }
                else if(msg.what == messages.DISABLE_SPLASH_SCREEN)
                {
                    boolean e_status = app_model.getInstance().getAppInstance().initSearchEngine();

                    if(e_status)
                    {
                        hideSplashScreen();
                    }
                }
            }
        };
    }

    void hideSplashScreen()
    {
        if(splashScreen.getVisibility()!=View.GONE)
        {
            onWelcomeMessageCheck();
        }

        status.isApplicationLoaded = true;
        splashScreen.animate().alpha(0.0f).setDuration(200).setListener(null).withEndAction((() -> splashScreen.setVisibility(View.GONE)));
    }

    boolean onDisableInternetError()
    {
        if(requestFailure.getAlpha()==1)
        {
            requestFailure.animate().alpha(0f).setDuration(150).withEndAction((() -> requestFailure.setVisibility(View.INVISIBLE)));
            return true;
        }
        else
        {
            return false;
        }
    }

    @SuppressLint("RestrictedApi")
    void onPageFinished(boolean status)
    {
        helperMethod.hideKeyboard();
        progressBar.setProgress(100);

        if(!status)
        {
            if(pageLoadedSuccessfully)
            {
                requestFailure.animate().alpha(0f).setStartDelay(200).setDuration(200).withEndAction((() -> requestFailure.setVisibility(View.INVISIBLE)));
                onUpdateView(true);
            }
            //onUpdateSearchBar(webView.getUrl());
            disableSplashScreen();
            floatingButton.animate().alpha(0).withEndAction((() -> floatingButton.setVisibility(View.GONE)));

            app_model.getInstance().getAppInstance().stopHiddenView(false,false);
        }
        else
        {
            onUpdateView(false);
            floatingButton.animate().alpha(1).withEndAction((() -> floatingButton.setVisibility(View.VISIBLE)));
        }
    }

    void checkSSLTextColor()
    {
        if (searchbar == null)
        {
            return;
        }

        if (searchbar.getText().toString().contains("https://"))
        {
            SpannableString ss = new SpannableString(searchbar.getText());
            ss.setSpan(new ForegroundColorSpan(Color.argb(255, 0, 123, 43)), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(Color.GRAY), 5, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            searchbar.setText(ss);
        } else if (searchbar.getText().toString().contains("http://"))
        {
            SpannableString ss = new SpannableString(searchbar.getText());
            ss.setSpan(new ForegroundColorSpan(Color.argb(255, 0, 128, 43)), 0, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(Color.GRAY), 4, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            searchbar.setText(ss);
        } else
        {
            SpannableString ss = new SpannableString(searchbar.getText());
            ss.setSpan(new ForegroundColorSpan(Color.BLACK), 0, searchbar.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            searchbar.setText(ss);
        }
    }

    void onClearSearchBarCursor()
    {
        searchbar.clearFocus();
    }

    void disableFloatingView()
    {
        floatingButton.animate().alpha(0).withEndAction((() -> floatingButton.setVisibility(View.GONE)));

    }

    void onUpdateSearchBar(String url)
    {
        searchbar.setText(url.replace(constants.backendUrlHost,constants.frontEndUrlHost_v1));
        checkSSLTextColor();
    }

    private void initSplashScreen()
    {
        boolean hasSoftKey = helperMethod.hasSoftKeys(app_model.getInstance().getAppInstance().getWindowManager());
        int height = helperMethod.screenHeight(hasSoftKey);

        splashlogo.getLayoutParams().height = height;
        loading.setAnimation(helperMethod.getRotationAnimation());
        loading.setLayoutParams(helperMethod.getCenterScreenPoint(loading.getLayoutParams()));
    }

    void onProgressBarUpdate(int progress)
    {
        if(progress==0)
        {
            progressBar.animate().alpha(0).withEndAction((() -> progressBar.setProgress(progress)));
        }
        else if(splashScreen.getVisibility() == View.GONE)
        {
            if(progressBar.getVisibility()==View.INVISIBLE)
            {
                progressBar.setProgress(4);
            }
            else
            {
                progressBar.setProgress(progress);
            }
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(1);
        }
    }

    void onBackPressed()
    {
        if(app_model.getInstance().getNavigation().size()>0)
        {
            if(app_model.getInstance().getNavigation().size()==1)
            {
                onProgressBarUpdate(0);
                helperMethod.onMinimizeApp();
                return;
            }
            else if(app_model.getInstance().getNavigation().get(app_model.getInstance().getNavigation().size()-2).type().equals(enums.navigationType.base))
            {
                app_model.getInstance().getAppInstance().stopHiddenView(true,true);
                if(webView.getVisibility()==View.VISIBLE)
                {
                    onProgressBarUpdate(4);
                    webView.goBack();
                }
                else
                {
                    onProgressBarUpdate(0);
                }

                /*CHANGED BUT NOT TESTED*/
                app_model.getInstance().getNavigation().remove(app_model.getInstance().getNavigation().size()-1);

                webView.bringToFront();
                webView.setAlpha(1);
                webView.setVisibility(View.VISIBLE);
                requestFailure.animate().alpha(0f).setDuration(200).withEndAction((() -> requestFailure.setVisibility(View.INVISIBLE)));
                onUpdateSearchBar(webView.getUrl());
                floatingButton.animate().alpha(0).withEndAction((() -> floatingButton.setVisibility(View.GONE)));
            }
            else
            {
                app_model.getInstance().getAppInstance().stopHiddenView(true,true);
                app_model.getInstance().getNavigation().remove(app_model.getInstance().getNavigation().size()-1);
                if(webView.getVisibility()==View.VISIBLE)
                {
                    app_model.getInstance().getAppInstance().onReInitGeckoView();
                    app_model.getInstance().getAppInstance().onReloadHiddenView();
                }
                else
                {
                    app_model.getInstance().getAppInstance().onHiddenGoBack();
                }
            }
        }
    }

    void onUpdateView(boolean status)
    {
        if(status)
        {
            floatingButton.animate().alpha(0).withEndAction((() -> floatingButton.setVisibility(View.GONE)));
            webView.setAlpha(1);
            webView.setVisibility(View.VISIBLE);
            webView.bringToFront();
            onProgressBarUpdate(0);
            onUpdateSearchBar(webView.getUrl());
        }
        else
        {
            webView.animate().alpha(0).setDuration(150).withEndAction((() -> webView.setVisibility(View.GONE)));
        }
    }

    private void onWelcomeMessageCheck()
    {
        if(!preference_manager.getInstance().getBool("FirstTimeLoaded",false))
        {
            message_manager.getInstance().welcomeMessage();
        }
        serverRequestManager.getInstance().versionChecker();
    }

    public void onShowAds()
    {
        startPostTask(messages.SHOW_ADS);
    }

    void openMenu(View view)
    {
        LinearLayout parentView = (LinearLayout)view.getParent();

        PopupMenu popup = new PopupMenu(app_model.getInstance().getAppInstance(), parentView,Gravity.RIGHT);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_main, popup.getMenu());
        MenuCompat.setGroupDividerEnabled(popup.getMenu(), true);
        popup.setOnMenuItemClickListener(item ->
        {
            app_model.getInstance().getAppInstance().onMenuOptionSelected(item);
            return true;
        });
        popup.show();
        view.bringToFront();
    }

    void onReload()
    {
        if(app_model.getInstance().getNavigation().get(app_model.getInstance().getNavigation().size()-1).type()==enums.navigationType.base)
        {
            onRequestTriggered(false,webView.getUrl());
            webView.reload();
        }
        else
        {
            app_model.getInstance().getAppInstance().onReloadHiddenView();
        }
    }

    String getSearchBarUrl()
    {
        return searchbar.getText().toString();
    }

}