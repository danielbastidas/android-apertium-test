package com.blue.parrot.riot.test;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apertium.Translator;
import org.apertium.android.ApertiumInstallation;
import org.apertium.utils.IOUtils;

import java.io.File;
import java.io.IOException;

public class Test extends Application {

    public static Test instance;
    public static Handler handler;
    public static SharedPreferences prefs;

    public static ApertiumInstallation apertiumInstallation;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        instance = this;
        handler = new Handler();

        // The '2' below is for historic reasons, we keep these names as users have already installed pairs there
        File packagesDir = new File(getFilesDir(), "packages"); // where packages' data are installed
        File bytecodeDir = new File(getFilesDir(), "bytecode"); // where packages' bytecode are installed. Must be private
        File bytecodeCacheDir = new File(getCacheDir(), "bytecodecache"); // where bytecode cache is kept. Must be private
        IOUtils.cacheDir = new File(getCacheDir(), "apertium-index-cache"); // where cached transducerindexes are kept
        apertiumInstallation = new ApertiumInstallation(packagesDir, bytecodeDir, bytecodeCacheDir);
        apertiumInstallation.rescanForPackages();


        Log.i("TAG", "IOUtils.cacheDir set to " + IOUtils.cacheDir);


    }


}
