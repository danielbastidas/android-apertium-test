/*
 * Copyright (C) 2012 Arink Verma
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
/**
 ExtendedApertiumActivity.java Main Launcher Activity of application

 @author Arink Verma

 */
package org.apertium.android.extended;

import org.apertium.android.extended.SMS.SMSInboxActivity;

import org.apertium.Translator;
import org.apertium.android.helper.Prefs;
import org.apertium.android.extended.languagepair.TranslationMode;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.apertium.android.R;
import org.apertium.android.App;
import org.apertium.utils.IOUtils;

public class ExtendedApertiumActivity extends Activity implements OnClickListener {
  private final String TAG = "ApertiumActiviy";
  private Activity thisActivity = null;

  /*Layout variable*/
  //Text Fields
  private EditText inputEditText;
  private TextView outputTextView;
  //Button
  private Button submitButton;
  private Button toButton;
  private Button fromButton;
  private Button dirButton;

  /*Mode related variable*/
  private String currentMode = null;
  private String fromLanguage = null;
  private String toLanguage = null;
  private String outputText = null;
  private String inputText = null;
  private TranslationMode translationMode = null;
  private ProgressDialog progressDialog;


  /* OnCreate */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Extended.init(this);
    thisActivity = this;
    getExtrasData();


    // FileManager.setDIR();  done in Application singledon
    updateDirChanges();

    /* Fetching if mode is sent by widgets */
    if (currentMode == null) {
      currentMode = Extended.rulesHandler.getCurrentMode();
    } else {
      Extended.rulesHandler.setCurrentMode(currentMode);
    }


    /* Setting up Translator base and properties */
    Log.i(TAG, "Current mode = " + currentMode + ", Cache = " + Prefs.isCacheEnabled() + ", Clipboard push get = " + Prefs.isClipBoardPushEnabled() + Prefs.isClipBoardGetEnabled());
    translationMode = Extended.databaseHandler.getMode(currentMode);
    if (translationMode != null && translationMode.isValid()) {
      try {
        Log.i(TAG, "ExtractPath =" + Extended.rulesHandler.ExtractPathCurrentPackage() + ", Jar= " + Extended.rulesHandler.PathCurrentPackage());
        Translator.setBase(Extended.rulesHandler.ExtractPathCurrentPackage(), Extended.rulesHandler.getDexClassLoader());
        Translator.setDelayedNodeLoadingEnabled(true);
        Translator.setParallelProcessingEnabled(false);
        Translator.setCacheEnabled(Prefs.isCacheEnabled());
      } catch (Exception e) {
        Log.e(TAG, "Error while loading class" + e);
        e.printStackTrace();
      }
    } else {
      Extended.rulesHandler.clearCurrentMode();
    }

    /* Generating layout view */
    initView();
  }

  /* OnResume */
  @Override
  protected void onResume() {
    super.onResume();
    Log.i(TAG, "onResume mode=" + Extended.rulesHandler.getCurrentMode());

    /* Fetching if mode is sent by widgets */
    getExtrasData();

    /**
     Giving priority to incoming text from intent over clipboardtext
     */
    if (inputText == null && Prefs.isClipBoardGetEnabled()) {
      inputText = App.clipboardHandler.getText();
    }

    if (inputText != null) {
      inputEditText.setText(inputText);
    }

    if (currentMode == null) {
      currentMode = Extended.rulesHandler.getCurrentMode();
    } else {
      Extended.rulesHandler.setCurrentMode(currentMode);
    }

    /* updating if mode is changed */
    translationMode = Extended.databaseHandler.getMode(currentMode);
    if (translationMode != null && translationMode.isValid()) {
      UpdateMode();
    } else {
      Log.i(TAG, "Invalid mode");
      toButton.setText(R.string.to);
      fromButton.setText(R.string.from);

      toLanguage = getString(R.string.to);
      fromLanguage = getString(R.string.from);
    }
  }

  void getExtrasData() {
    Intent intent = getIntent();

    /**
     First look for shared from other apps
     */
    String action = intent.getAction();
    String type = intent.getType();

    if (Intent.ACTION_SEND.equals(action) && type != null) {
      if ("text/plain".equals(type)) {
        inputText = intent.getStringExtra(Intent.EXTRA_TEXT);
        return;
      }
    }

    /**
     Then look for data from clipboard *
     */
    Bundle extras = intent.getExtras();
    if (extras != null) {
      //Getting input from ModeManageActivity and Widget Button
      String BundleMODE = extras.getString("Mode");
      if (BundleMODE != null) {
        currentMode = BundleMODE;
      }

      //Gettting input from SMS Activity
      BundleMODE = extras.getString("input");
      if (BundleMODE != null) {
        inputText = BundleMODE;
      }
    }
  }

  /* Init View,
   * Initialing view */
  private void initView() {
    Log.i(TAG, "ApertiumActivityInitView Started");
    setContentView(R.layout.main_layout);
    outputTextView = (TextView) findViewById(R.id.outputText);
    inputEditText = (EditText) findViewById(R.id.inputtext);

    /**
     Giving priority to incoming text from intent over clipboardtext
     */
    if (inputText == null && Prefs.isClipBoardGetEnabled()) {
      inputText = App.clipboardHandler.getText();
    }

    if (inputText != null) {
      inputEditText.setText(inputText);
    }

    submitButton = (Button) findViewById(R.id.translateButton);
    toButton = (Button) findViewById(R.id.toButton);
    fromButton = (Button) findViewById(R.id.fromButton);
    dirButton = (Button) findViewById(R.id.modeSwitch);

    submitButton.setOnClickListener(this);
    toButton.setOnClickListener(this);
    fromButton.setOnClickListener(this);
    dirButton.setOnClickListener(this);
  }


  /* Update Translator mode if user change */
  private void UpdateMode() {
    if (currentMode == null) {
      if (Extended.databaseHandler.getAllModes().isEmpty()) {
        // No modes, go to download
        startActivity(new Intent(ExtendedApertiumActivity.this, DownloadActivity.class));
      }
      toButton.setText(R.string.to);
      toButton.setText(R.string.from);
      return;
    }


    Log.i(TAG, "UpdateMode =" + currentMode + ", cache= " + Prefs.isCacheEnabled());

    try {

      String currentPackage = Extended.rulesHandler.getCurrentPackage();
      String PackageTOLoad = Extended.rulesHandler.findPackage(currentMode);

      //If mode is changed
      if (!currentMode.equals(Extended.rulesHandler.getCurrentMode())) {
        Log.i(TAG, "Mode changed , setCurrentMode=" + currentMode);
        Extended.rulesHandler.setCurrentMode(currentMode);
      }

      //If package is changed
      if (currentPackage == null || !currentPackage.equals(PackageTOLoad)) {
        Log.i(TAG, "BaseChanged =" + Extended.rulesHandler.getDexClassLoader() + "path = " + Extended.rulesHandler.ExtractPathCurrentPackage());
        Translator.setBase(Extended.rulesHandler.ExtractPathCurrentPackage(), Extended.rulesHandler.getDexClassLoader());
        Translator.setDelayedNodeLoadingEnabled(true);
        Translator.setParallelProcessingEnabled(false);
        Translator.setCacheEnabled(Prefs.isCacheEnabled());
      }

      IOUtils.timing = new org.apertium.utils.Timing("setMode");
      Translator.setMode(currentMode);
      IOUtils.timing.report();
      IOUtils.timing = null;

      translationMode = Extended.databaseHandler.getMode(currentMode);
      fromLanguage = translationMode.fromLang;
      toLanguage = translationMode.toLang;

      toButton.setText(toLanguage);
      fromButton.setText(fromLanguage);
    } catch (Exception e) {
      Log.e(TAG, "UpdateMode " + e + "Mode = " + currentMode);
      e.printStackTrace();
    }

  }

  @Override
  public void onClick(View v) {

    if (v.equals(submitButton)) {
      if (translationMode != null && translationMode.isValid()) {
        //Hiding soft keypad
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(inputEditText.getApplicationWindowToken(), 0);

        TranslationRun();
      }
    } else if (v.equals(fromButton)) {

      if (Extended.databaseHandler.getAllModes().isEmpty()) {
        // No modes, go to download
        startActivity(new Intent(ExtendedApertiumActivity.this, DownloadActivity.class));
        return;
      }

      final String[] ModeTitle = Extended.databaseHandler.getModeTitlesOut();

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(getString(R.string.translate_from));
      builder.setItems(ModeTitle, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int position) {
          Toast.makeText(getApplicationContext(), ModeTitle[position], Toast.LENGTH_SHORT).show();
          fromLanguage = ModeTitle[position];
          toLanguage = null;
          fromButton.setText(fromLanguage);
          toButton.setText(R.string.to);
        }
      });
      AlertDialog alert = builder.create();
      alert.show();
    } else if (v.equals(toButton)) {

      if (Extended.databaseHandler.getAllModes().isEmpty()) {
        // No modes, go to download
        startActivity(new Intent(ExtendedApertiumActivity.this, DownloadActivity.class));
        return;
      }

      final String[] ModeTitle = Extended.databaseHandler.getModeTitlesInFrom(fromLanguage);

      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(getString(R.string.translate_to));
      builder.setItems(ModeTitle, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int position) {
          Toast.makeText(getApplicationContext(), ModeTitle[position], Toast.LENGTH_SHORT).show();
          toLanguage = ModeTitle[position];
          toButton.setText(toLanguage);
          Log.i(TAG, Extended.databaseHandler.getModeID(fromLanguage, toLanguage));
          currentMode = Extended.databaseHandler.getModeID(fromLanguage, toLanguage);

          UpdateMode();
        }
      });

      AlertDialog alert = builder.create();
      alert.show();
    } else if (v.equals(dirButton)) {
      if (toLanguage == null) {
        Toast.makeText(getApplicationContext(), getString(R.string.no_mode_to), Toast.LENGTH_SHORT).show();

      } else {
        String reverseMode = Extended.databaseHandler.getModeID(toLanguage, fromLanguage);
        if (reverseMode == null) {
          Toast.makeText(getApplicationContext(), getString(R.string.no_mode_available, fromLanguage, toLanguage), Toast.LENGTH_SHORT).show();
        } else {
          String temp = fromLanguage;
          fromLanguage = toLanguage;
          toLanguage = temp;
          currentMode = reverseMode;
          UpdateMode();
        }
      }
    }

  }


  /* Translation Thread,
   * Load translation rules and excute lttoolbox.jar */
  private void TranslationRun() {
    progressDialog = ProgressDialog.show(this, getString(R.string.translating), getString(R.string.working), true, true);
    Thread t = new Thread() {
      @Override
      public void run() {
        String inputText = inputEditText.getText().toString();
        if (!TextUtils.isEmpty(inputText)) {
          outputText = "";

          Runtime rt = Runtime.getRuntime();
          Log.d(TAG, "start mem f=" + rt.freeMemory() / 1000000 + "  t=" + rt.totalMemory() / 1000000 + " m=" + rt.maxMemory() / 1000000);
          IOUtils.timing = new org.apertium.utils.Timing("overall");
          try {
            Translator.setCacheEnabled(Prefs.isCacheEnabled());
            Log.i(TAG, "Translator Run Cache =" + Prefs.isCacheEnabled() + ", Mark =" + Prefs.isDisplayMarkEnabled() + ", MODE = " + currentMode);
            Translator.setDisplayMarks(Prefs.isDisplayMarkEnabled());
            outputText = Translator.translate(inputEditText.getText().toString());

            if (Prefs.isClipBoardPushEnabled()) {
              App.clipboardHandler.putText(outputText);
            }

          } catch (Throwable e) {
            Log.e(TAG, "ApertiumActivity.TranslationRun MODE =" + currentMode + ";InputText = " + inputEditText.getText());
            e.printStackTrace();
          }
          IOUtils.timing.report();
          IOUtils.timing = null;
          Log.d(TAG, "start mem f=" + rt.freeMemory() / 1000000 + "  t=" + rt.totalMemory() / 1000000 + " m=" + rt.maxMemory() / 1000000);
        }

        App.handler.post(new Runnable() {
          @Override
          public void run() {
            outputTextView.setText(outputText);
            progressDialog.dismiss();
          }
        });
      }
    };
    t.start();


  }

  private void updateDirChanges() {
    if (Prefs.isStateChanged()) {
      progressDialog = ProgressDialog.show(thisActivity, getString(R.string.updating_db), getString(R.string.working), true, false);

      Thread t = new Thread() {
        @Override
        public void run() {
          Extended.databaseHandler.reloadAll();

          App.handler.post(new Runnable() {
            @Override
            public void run() {
              progressDialog.dismiss();
              Prefs.saveState();
            }
          });
        }
      };
      t.start();
    }
  }

  /**
   **
   Option menu 1. share 2. inbox 3. manage 4. setting
   */
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.option_menu, menu);
    return true;
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent = null;
    switch (item.getItemId()) {
      case R.id.share:
        share_text();
        return true;
      case R.id.inbox:
        intent = new Intent(ExtendedApertiumActivity.this, SMSInboxActivity.class);
        startActivityForResult(intent, 0);
        return true;
      case R.id.manage:
        intent = new Intent(ExtendedApertiumActivity.this, ManageActivity.class);
        startActivity(intent);
        return true;
      case R.id.clear:
        inputEditText.setText("");
        outputTextView.setText("");
        inputText = "";
        outputText = "";
        return true;
      case R.id.about:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.about));
        WebView wv = new WebView(this);
        Log.d(TAG, getString(R.string.aboutText));
        wv.loadData(getString(R.string.aboutText), "text/html", "utf-8");
        builder.setView(wv);
        AlertDialog alert = builder.create();
        alert.show();
        /*
         try {
         ArrayList al = new ArrayList();
         Runtime rt = Runtime.getRuntime();
         while (true) {
         al.add(new byte[1000000]);
         Log.d(TAG, "al "+al.size()+"M f="+rt.freeMemory()/1000000+"  t="+rt.totalMemory()/1000000+" m="+rt.maxMemory()/1000000);
         }
         } catch (Throwable t) {
         t.printStackTrace();
         }
         */
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /* Share text
   * Intent to share translated text over other installed application services */
  private void share_text() {
    Log.i(TAG, "ApertiumActivity.share_text Started");
    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
    sharingIntent.setType("text/plain");
    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Apertium Translate");
    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, outputText);
    startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)));
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    inputText = data.getStringExtra("input");
  }
}