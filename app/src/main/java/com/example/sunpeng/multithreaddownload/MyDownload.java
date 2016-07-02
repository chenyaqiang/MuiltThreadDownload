package com.example.sunpeng.multithreaddownload;

import android.content.Context;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * Created by sunpeng on 2016/3/5.
 */
public class MyDownload extends Thread {

    String strUrl; // 下载文件的地址
    int length = 0; // 文件的大小
    String fileName;   //文件名称
    String directory; // 下载文件的存放目录
    String filePath; // 下载文件存放的全路径
    private DownloadListener downloadListener;
    private Object mPauseLock = new Object();
    private boolean mPause=false;


    public MyDownload(String strUrl, String fileName) {
        this.strUrl = strUrl;
        this.fileName = fileName;
    }

    /**
     * 继续下载
     */
    public  void resum(){
        synchronized (mPauseLock){
            mPause=false;
            mPauseLock.notify();
        }
    }
    /**
     * 暂停下载
     */
    public void pause(){
            mPause=true;
    }

    /**
     * 此种写法，界面第一次点击暂停，会卡住一会，主线程暂停，但是下载线程并没有立即暂停，还会下载一会儿
     * 主界面卡住一会的原因是因为主界面线程在等待同步锁对象
     */
    public void pause1(){
        synchronized (mPauseLock){

            mPause=true;
        }
    }


    @Override
    public void run() {
        HttpURLConnection connection = null;
        try {
            // 存放文件的目录，如果不存在，则创建
            directory = Environment.getExternalStorageDirectory().getPath()
                    + "/TestDownload";
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdir();
            }
            filePath = directory + "/" + fileName;
            URL url = new URL(strUrl);
            connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30 * 1000);
            // 请求对象下载部分的文件，从指定的位置开始下载
            // connection.setRequestProperty("Range","bytes="+startPosition+"-");
            int code = connection.getResponseCode();
            // 部分文件是206，全部OK是200
            if (code == 200) {
                length=connection.getContentLength();
                //开始下载之前
                if (downloadListener != null) {
                    downloadListener.beforeDownload(length, filePath);
                }
                // 已经设置了请求的位置，返回的是设置的位置对应的文件的输入流
                InputStream is = connection.getInputStream();
                BufferedInputStream bis  = new BufferedInputStream(is);
                RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
                raf.seek(0); // 设置文件开始写的位置

                int len = 0;
                int total = 0;
                byte[] buffer = new byte[1024];

                    while ((len = bis.read(buffer)) != -1) {
                        synchronized (mPauseLock){
                            if(mPause){
                                mPauseLock.wait();
                            }
                            raf.write(buffer, 0, len);
                            total += len;
                            // 正在下载
                            if (downloadListener != null) {
                                downloadListener.onDownloading(total, filePath);
                            }
                            Log.i("length", total + "");
                        }
                    }
                bis.close();
                is.close();
                raf.close();
                //下载完成
                if (downloadListener != null) {
                    downloadListener.onDownloadComplete(filePath);
                }
                // Log.d("DOWNLOAD","线程"+threadId+"下载完毕::"+"位置:"+startPosition+"-"+endPosition+"--->"+total);
            } else {
                //下载失败
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                if (downloadListener != null) {
                    downloadListener.onDownloadFailed("错误代码：" + code);
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.i("e", e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("e", e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (Looper.myLooper() != null) {
                Looper.myLooper().quit();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void setDownloadListener(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }
}



