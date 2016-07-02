package com.example.sunpeng.multithreaddownload;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * 
 * @author sunpeng
 *
 */
public class DownloadUtil {

	Context context;
	String strUrl; // 下载文件的地址
	int length = 0; // 文件的大小
	String fileName;   //文件名称
	String directory; // 下载文件的存放目录
	String filePath; // 下载文件存放的全路径
	final int SHOW_DIALOG=2000;         //显示进度对话框并设置最大长度
	final int UPDATE_PROGRESS = 1000;   //进度更新
	final int DOWNLOAD_FINISH = 1200;		//下载完成
	final int DOWNLOAD_FAILED=-1000;    //下载失败

	static final int THREAD_COUNT = 1; // 默认开启一个线程下载（可实现多线程断点下载）
	
	private Handler handler;
	
	private DownloadThread t;
	

	public DownloadUtil(String strUrl,String fileName,Handler handler) {
		this.strUrl=strUrl;
		this.fileName=fileName;
		this.handler=handler;
	}
	
	public void startDownload() {
		
		//需要判断文件存在不存在
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					// 存放文件的目录，如果不存在，则创建
					directory = Environment.getExternalStorageDirectory().getPath()
							+ "/TestDownload";
					File dir = new File(directory);
					if (!dir.exists()) {
						dir.mkdir();
					}
					filePath = directory + "/"+fileName;
					URL url = new URL(DownloadUtil.this.strUrl);
					HttpURLConnection connection = (HttpURLConnection) url
							.openConnection();
					
					connection.setConnectTimeout(10000);
					connection.setRequestMethod("GET");
					int code = connection.getResponseCode();
					if (code == 200) {
						length = connection.getContentLength();
						
						RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
						raf.setLength(length);
						raf.close();

						int blockSize = length / THREAD_COUNT;
						int startPosition, endPosition;
						for (int threadId = 1; threadId <= THREAD_COUNT; threadId++) {
							startPosition = (threadId - 1) * blockSize;
							endPosition = threadId * blockSize - 1;
							// 最后一个线程下载剩余所有的长度
							if (threadId == THREAD_COUNT) {
								endPosition = length;
							}
							t=new DownloadThread(threadId, startPosition, endPosition,
									filePath);
							t.start();
						}
					}

				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
		
	}
	/**
	 * 取消下载
	 * @throws InterruptedException
	 */
	public void cancelDownload() throws InterruptedException {
		if(t!=null){
			t.interrupt();
		}
	}

	/**
	 * 下载文件的线程类
	 */
	private class DownloadThread extends Thread {
		int threadId;
		int startPosition; // 下载的开始位置
		int endPosition; // 下载的结束位置
		String filePath; // 需要保存到的文件的路径

		public DownloadThread(int threadId, int startPosition, int endPosition,
				String filePath) {
			this.threadId = threadId;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.filePath = filePath;
		}

		@Override
		public void run() {
			try {
				URL url = new URL(strUrl);
				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();
				connection.setRequestMethod("GET");
				connection.setConnectTimeout(10000);
				// 请求对象下载部分的文件，从指定的位置开始下载
				// connection.setRequestProperty("Range","bytes="+startPosition+"-");
				int code = connection.getResponseCode();
				// 部分文件是206，全部OK是200
				if (code == 200) {
					//显示进度对话框并设置最大长度
					Message msgShow=new Message();
					msgShow.what=SHOW_DIALOG;
					msgShow.obj=length;
					handler.sendMessage(msgShow);
					// 已经设置了请求的位置，返回的是设置的位置对应的文件的输入流
					InputStream is = connection.getInputStream();
					RandomAccessFile raf = new RandomAccessFile(filePath, "rwd");
					raf.seek(startPosition); // 设置文件开始写的位置

					int len = 0;
					int total = 0;
					byte[] buffer = new byte[1024];
					while ((len = is.read(buffer)) != -1) {
						raf.write(buffer, 0, len);
						total += len;
						// 通知进度条更新
						Message msg1 = new Message();
						msg1.what = UPDATE_PROGRESS;
						msg1.obj = total;
						Log.i("length", total + "");
						handler.sendMessage(msg1);
					}
					is.close();
					raf.close();

					//下载完成
					Message msg2 = new Message();
					msg2.what = DOWNLOAD_FINISH;
					msg2.obj=filePath;
					handler.sendMessage(msg2);
					// Log.d("DOWNLOAD","线程"+threadId+"下载完毕::"+"位置:"+startPosition+"-"+endPosition+"--->"+total);
				}else{
					//下载失败
					Message msg = new Message();
					msg.what = DOWNLOAD_FAILED;
					handler.sendMessage(msg);
					File file=new File(filePath);
					if(file.exists()){
						file.delete();
					}
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
