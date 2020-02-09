package com.blue.parrot.riot.test.Activity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.blue.parrot.riot.test.Test;

import org.apertium.Translator;
import org.apertium.android.LanguageTitles;
import org.apertium.android.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class DefaultActivity extends Activity {

    static final String REPO_URL = "https://svn.code.sf.net/p/apertium/svn/builds/language-pairs";

    private static class Data {
//        InstallActivity activity;
        ArrayList<String> packages = new ArrayList<String>();
        HashSet<String> installedPackages = new HashSet<String>();
        HashSet<String> updatablePackages = new HashSet<String>();
        HashSet<String> updatedPackages = new HashSet<String>();
        HashSet<String> packagesToInstall = new HashSet<String>();
//        HashSet<String> packagesToUninstall = new HashSet<String>();
        HashMap<String, String> packageToTitle = new HashMap<String, String>();
        HashMap<String, URL> packageToURL = new HashMap<String, URL>();
        File cachedRepoFile;
//        InstallActivity.RepoAsyncTask repoTask;
//        InstallActivity.InstallRemoveAsyncTask installTask;
//        String progressText;
//        private int progressMax;
//        private int progress;
    }

    private Data data;

    private static void loadLanguagePackages(Data d, InputStream inputStream, boolean useNetwork) throws IOException {
        ArrayList<String> packages = new ArrayList<String>();
        // Get a copy of the list of installed packages, as we modify it below
        HashSet<String> installedPackages = new HashSet<String>(Test.apertiumInstallation.modeToPackage.values());

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length > 3) {
                // apertium-af-nl	https://svn.code.sf.net/p/apertium/svn/builds/apertium-af-nl/apertium-af-nl.jar	file:apertium-af-nl-0.2.0.tar.gz	af-nl, nl-af
                // apertium-ca-it	https://svn.code.sf.net/p/apertium/svn/builds/apertium-ca-it/apertium-ca-it.jar	file:apertium-ca-it-0.1.0.tar.gz	ca-it, it-ca
                String pkg = columns[0];
                packages.add(pkg);
                URL url = new URL(columns[1]);
                d.packageToURL.put(pkg, url);
                String modeTitle = LanguageTitles.getTitle(columns[3]);
                d.packageToTitle.put(pkg, modeTitle);
                if (installedPackages.contains(pkg)) {
                    installedPackages.remove(pkg);
                    d.installedPackages.add(pkg);
                    if (useNetwork) {
                        long localLastModified = new File(Test.apertiumInstallation.getBasedirForPackage(pkg)).lastModified();
                        long onlineLastModified = url.openConnection().getLastModified();
                        if (onlineLastModified > localLastModified) {
                            d.updatablePackages.add(pkg);
                        } else {
                            d.updatedPackages.add(pkg);
                        }
                    }
                }
            }
        }

        for (String pkg : installedPackages) {
            packages.add(pkg);
            d.installedPackages.add(pkg);
        }

        Collections.sort(packages);
        d.packages = packages;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /** This is the name of the package of the language used for this test */
        String pkg = "apertium-en-es";

        /** Create data object that holds languages information for this activity */
        data = (Data) getLastNonConfigurationInstance();
        if (data == null) {
            data = new Data();
            data.packagesToInstall.add(pkg);
            data.cachedRepoFile = new File(getCacheDir(), new File(REPO_URL).getName());
        }

        initLanguagePackages();
        /** Android force to download resources in background, this is the case with the asynchronous task */
        new InstallLanguageTask().execute();

    }

    private void initLanguagePackages() {
        if (data.cachedRepoFile.exists()) {
            try {
                loadLanguagePackages(data, new FileInputStream(data.cachedRepoFile), false);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("ERROR","Error loading language package file from repo");
            }
        } else {
            try {
                loadLanguagePackages(data, this.getResources().openRawResource(R.raw.language_pairs), false);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("ERROR","Error loading language package file from network");
            }
        }
    }

    private void installLanguage() {

        int packageNo = 1;
        for (String pkg : new HashSet<String>(data.packagesToInstall)) { // Avoid ConcurrentModificationException
            try {
                Log.i("INFO", "Downloading "+pkg+"...");
                URL url = data.packageToURL.get(pkg);
                URLConnection uc = url.openConnection();
                long lastModified = uc.getLastModified();
                int contentLength = uc.getContentLength();
                BufferedInputStream in = new BufferedInputStream(uc.getInputStream());
                File tmpjarfile = new File(this.getCacheDir(), pkg + ".jar");
                FileOutputStream fos = new FileOutputStream(tmpjarfile);
                byte data[] = new byte[8192];
                int count;
                int total = 0;
                while ((count = in.read(data, 0, 1024)) != -1) {
                    fos.write(data, 0, count);
                    total += count;
                    Log.i("INFO",""+100 * packageNo + 90 * total / contentLength);
                }
                fos.close();
                in.close();
                tmpjarfile.setLastModified(lastModified);
                Log.i("INFO",this.getString(R.string.installing) + " " + pkg + "...");
                Test.apertiumInstallation.installJar(tmpjarfile, pkg);
                tmpjarfile.delete();
                packageNo++;
                Log.i("INFO",""+98 * packageNo);
                this.data.installedPackages.add(pkg);
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.e("ERROR","Error installing language "+pkg);
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        String pkg = "apertium-en-es";
        String currentModeTitle = "English → Spanish";
        String mode = Test.apertiumInstallation.titleToMode.get(currentModeTitle);
        String modeToPkg = Test.apertiumInstallation.modeToPackage.get(mode);
        try {
            Translator.setBase(Test.apertiumInstallation.getBasedirForPackage(pkg), Test.apertiumInstallation.getClassLoaderForPackage(modeToPkg));
            Translator.setMode(mode);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR","There was an error setting the translator"+" "+e.getMessage());
        }

        try {
            String textToTranslate = "This is my very first translation using APERTIUM";
            String translation = Translator.translate(textToTranslate);
            Log.i("INFO","Translation for the following text ["+textToTranslate+"] is:");
            Log.i("INFO","!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+translation+"!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR","There was an error performing the translation"+" "+e.getMessage());
        }

    }

    private void postInstallation() {
        Test.apertiumInstallation.rescanForPackages();
    }

    class InstallLanguageTask extends AsyncTask<Void, Void, Void> {

        private Exception exception;

        protected Void doInBackground(Void... voids) {
            try {
                installLanguage();
            } catch (Exception e) {
                this.exception = e;

            }
            return null;
        }

        protected void onPostExecute() {
            postInstallation();
        }

    }
}
