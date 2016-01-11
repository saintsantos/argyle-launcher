package com.argyetechnology.edwin.argylelauncher;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Argyle Launcher";
    private static final String APP_PACKAGE = "com.argyletechnologygroup.REDAR";
    private DownloadManager manager;
    private String link = "https://www.dropbox.com/s/5tsbcvjyy2ozpfi/app.zip?dl=1";
    private long myDownloadReference;
    private String downloadCompleteIntentName = DownloadManager.ACTION_DOWNLOAD_COMPLETE;
    private IntentFilter downloadCompleteIntentFilter = new IntentFilter(downloadCompleteIntentName);

    //The buttons to handle the actions in the platform
    private Button launchBtn;
    private Button updateBtn;
    private Button installBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO: Move the update button to a menu option instead of simply a button
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Makes the app fullscreen
        View decorView = getWindow().getDecorView();
        int  uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

         BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {

                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(myDownloadReference);
                    Cursor c = manager.query(query);

                    if (downloadId != downloadId) {
                        Log.v(TAG, "Ignoring unrelated download" + downloadId);
                        return;

                    }

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
                    install();

                }
            }
        };
        registerReceiver(downloadCompleteReceiver, downloadCompleteIntentFilter);

        installBtn = (Button) findViewById(R.id.manUpdate);
        installBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                install();
            }
        });

        launchBtn = (Button) findViewById(R.id.Launch);
        launchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launch(v);
            }
        });

        updateBtn = (Button) findViewById(R.id.updateButton);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                download();
            }
        });
    }

    public void launch(View view) {
        //TODO: Make the launch button larger
        Log.d(TAG, "Launching package");
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(APP_PACKAGE);
        startActivity(launchIntent);
    }

    public void download() {
        //TODO: Add check for the existence of the downloaded files before simply deleting them.
        //Deletes files from the Download directory before downloading the updates
        String root = Environment.getExternalStorageDirectory().toString();
        File file1 = new File(root + "/Download/app.zip");
        File file2 = new File(root + "/Download/quicklaunch.apk");
        Log.d(TAG, "Deleting files");
        file1.delete();
        file2.delete();

        //Perform the required downloads for the update
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(link));
        request.setDescription("I am downloading the update").setTitle("Downloading update");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app.zip");
        request.setVisibleInDownloadsUi(true);
        manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        myDownloadReference = manager.enqueue(request);

    }

    public void install() {
        //Installs the application
        Log.d(TAG, "Install application");
        unpackZip("/sdcard/Download/", "app.zip");
        Intent promptInstall = new Intent(Intent.ACTION_VIEW);
        promptInstall.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Download/" + "quicklaunch.apk")), "application/vnd.android.package-archive");
        promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(promptInstall);
    }

    private boolean unpackZip(String path, String zipname)
    {
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            Log.d(TAG, "File path: " + path + zipname);
            is = new FileInputStream(path + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();

                if (ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(path + filename);

                while ((count = zis.read(buffer)) != -1)
                {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();

        } catch (IOException e) {

            e.printStackTrace();
            return false;

        }

        return true;

    }

}
