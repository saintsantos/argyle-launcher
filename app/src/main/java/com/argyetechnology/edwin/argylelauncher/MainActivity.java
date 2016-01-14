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

    private String appname = "REDAR.apk";

    private BroadcastReceiver downloadCompleteReceiver;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Makes the app fullscreen
        View decorView = getWindow().getDecorView();
        int  uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        launch();



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
                        install(appname);
                    } else {

                        if(versionFile.exists()) {

                            float currentVersion = getApplicationVersion();
                            float updateVersion = scan();

                            if(currentVersion >= updateVersion) {
                                Log.d(TAG, "The app is already at the latest version");
                            } else {
                                download("http://liveupdates.argyletechnologygroup.com/redinc/REDAR.apk", "REDAR.apk");
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
                //launch(v);
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
                download("Insert link to txt file here", "version.txt");
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

    public float getApplicationVersion() {
        String packageToCheck = APP_PACKAGE;

        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo p = packages.get(i);
            if (p.packageName.contains(packageToCheck)) {
                String versionName = p.versionName;
                float versionNumber = Float.parseFloat(versionName);
                return versionNumber;
            }
        }
        return 0;
    }

    public float scan() {
        File scanFile = new File("/sdcard/Download", "test.txt");
        try {
            Scanner fileScanner = new Scanner(scanFile);
            float i = fileScanner.nextFloat();
            return i;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadCompleteReceiver);
    }
}
