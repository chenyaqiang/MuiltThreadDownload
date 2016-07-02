package com.example.sunpeng.multithreaddownload;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Button btn_download,btn_pause,btn_resume;
    private ProgressBar progressbar;
    private TextView tv;
    private MyHandler mHandler=new MyHandler();
    private String url="http://wap3.ucweb.com/files/UCBrowser/zh-cn/999/UCBrowser_V10.9.5.729_android_pf145_(Build160303105826).apk?vh=9830be12112429f1401ed38e8fc9f20a&SESSID=af11e82af51204ab8e544ec497ca62e4";
    private String filePath="/storage/sdcard0/TestDownload/uc.apk";
    private DownloadUtil downloadUtil;
    private MyDownload myDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        btn_download= (Button) findViewById(R.id.btn_download);
        btn_pause= (Button) findViewById(R.id.btn_pause);
        btn_resume= (Button) findViewById(R.id.btn_resume);
        progressbar= (ProgressBar) findViewById(R.id.progressbar);
        tv= (TextView) findViewById(R.id.tv);

        downloadUtil = new DownloadUtil(url,"uc.apk",mHandler);
        myDownload=new MyDownload(url,"uc.apk");


        myDownload.setDownloadListener(new DownloadListener() {
            @Override
            public void beforeDownload(int maxLengeh, String filePath) {
                progressbar.setMax(maxLengeh);
            }

            @Override
            public void onDownloading(int progress, String filePath) {
                progressbar.setProgress(progress);
               Message msg = new Message();
                msg.what=500;
                msg.obj=progress;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onDownloadComplete(String filePath) {
                Message msg1 = new Message();
                msg1.what=1000;
                mHandler.sendMessage(msg1);
            }

            @Override
            public void onDownloadFailed(String error) {
                mHandler.sendEmptyMessage(-1000);
            }
        });


        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                downloadUtil.startDownload();
                myDownload.start();
            }
        });



        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDownload.pause();
            }
        });

        btn_resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDownload.resum();
            }
        });

    }



    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                //设置长度
                case 100:
                    progressbar.setMax((Integer) msg.obj);
                    break;
                //进度更新
                case 500:
                    progressbar.setProgress((Integer) msg.obj);
                    tv.setText(msg.obj.toString());
                    break;
                //下载完成
                case 1000:
                    Toast.makeText(MainActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
                    break;
                //下载失败
                case -1000:
                    Toast.makeText(MainActivity.this, "下载失败！！！！！！！", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

}
