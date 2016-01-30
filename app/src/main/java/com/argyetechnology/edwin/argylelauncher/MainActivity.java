package com.argyetechnology.edwin.argylelauncher;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Argyle Launcher";
    private static final String APP_PACKAGE = "com.argyletechnologygroup.REDAR";
    private DownloadManager manager;
    private long myDownloadReference;
    private String downloadCompleteIntentName = DownloadManager.ACTION_DOWNLOAD_COMPLETE;
    private IntentFilter downloadCompleteIntentFilter = new IntentFilter(downloadCompleteIntentName);

    //The buttons to handle the actions in the platform
    private Button launchBtn;

    //Locations and app name
    private String updtloc = Environment.getExternalStorageDirectory() + "/Download/version.txt";
    private String apploc = Environment.getExternalStorageDirectory() + "/Download/REDAR.apk";

    private String versionLink = "http://liveupdates.argyletechnologygroup.com/redar/version.txt";
    private String appLink = "http://liveupdates.argyletechnologygroup.com/redar/REDAR.apk";

    private String appname = "REDAR.apk";
   // private String currentVersion = getApplicationVersion();

    private BroadcastReceiver downloadCompleteReceiver;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Makes the app fullscreen
        View decorView = getWindow().getDecorView();
        int  uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        //Check if the app is installed initially
        final String currentVersion = getApplicationVersion();
        Log.d(TAG, "The current version is: " + currentVersion);
       if (currentVersion != "") {
           Log.d(TAG, "launch app");
            //launch();
        } else {
            Toast.makeText(getApplicationContext(), "App not installed, please run updater", Toast.LENGTH_SHORT).show();
        }



        //The receiver to signal when the download of the file is complete
         downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {

                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(myDownloadReference);
                    Cursor c = manager.query(query);

                    if (!c.moveToFirst()) {
                        Log.e(TAG, "Empty Row");
                        return;
                    }

                    int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                    if (DownloadManager.STATUS_SUCCESSFUL != c.getInt(statusIndex)) {
                        Log.w(TAG, "Download Failed");
                        return;
                    }

                    Log.d(TAG, "Download Complete!");
                    File versionFile = new File(updtloc);
                    File appFile = new File(apploc);

                    if(appFile.exists()) {
                        Log.d(TAG, "Install the app");
                        install(appname);
                    } else {

                        if(versionFile.exists()) {

                            String currentVersion = getApplicationVersion();
                            String updateVersion = scan();
                            Log.d(TAG, "Current Version: " + currentVersion);
                            Log.d(TAG, "Update Verison: " + updateVersion);

                            if(currentVersion.equals(updateVersion)) {
                                Log.d(TAG, "The app is already at the latest version");
                                Toast.makeText(getApplicationContext(), "Application already at latest version", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "Downloading the apk installer");
                                Toast.makeText(getApplicationContext(), "New update, please wait for download to finish", Toast.LENGTH_SHORT).show();
                                download(appLink, "REDAR.apk");
                            }

                        }

                    }
                }
            }
        };

        registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);

        launchBtn = (Button) findViewById(R.id.Launch);
        launchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentVersion = getApplicationVersion();
                Log.d(TAG, "The current version is: " + currentVersion);
                if (!currentVersion.equals("") ) {
                    Log.d(TAG, "launch app");
                    launch();
                } else {
                    Toast.makeText(getApplicationContext(), "App not installed, please run updater", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {

            case R.id.updateRedar:
                Log.d(TAG, "The service to update REDAR was started");
                delete(apploc, updtloc);
                download(versionLink, "version.txt");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void launch() {
        Log.d(TAG, "Launching package");
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(APP_PACKAGE);
        startActivity(launchIntent);
    }

    public void download(String link, String filename) {
        //Perform the required downloads for the update
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
        request.setDescription("I am downloading the update").setTitle("Downloading update");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        request.setVisibleInDownloadsUi(true);
        manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        myDownloadReference = manager.enqueue(request);

    }

    public void install(String filename) {
        //Installs the application
        Log.d(TAG, "Install application");
        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
        promptInstall.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Download/" + filename)), "application/vnd.android.package-archive");
        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(promptInstall);
    }

    public void delete(String appLocation, String versionLocation) {
        File file1 = new File(versionLocation);
        File file2 = new File(appLocation);

        if(file1.exists()) {
            Log.d(TAG, "Old update file found, deleting... ");
            file1.delete();
        } else {
            Log.d(TAG, "Old update not found, skipping... ");
        }

        if(file2.exists()) {
            Log.d(TAG, "Old apk found, deleting... ");
            file2.delete();
        } else {
            Log.d(TAG, "Old apk not found, skipping... ");
        }
    }

    public String getApplicationVersion() {
        String packageToCheck = APP_PACKAGE;

        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo p = packages.get(i);
            if (p.packageName.contains(packageToCheck)) {
                String versionName = p.versionName;
                return versionName;
            }
        }
        return "";
    }

    public String scan() {
        File scanFile = new File("/sdcard/Download", "version.txt");
        try {
            Scanner fileScanner = new Scanner(scanFile);
            String line = fileScanner.nextLine();
            return line;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }


    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadCompleteReceiver);
    }
}
