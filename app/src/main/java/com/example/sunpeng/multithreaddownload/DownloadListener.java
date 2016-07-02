package com.example.sunpeng.multithreaddownload;

/**
 * 下载监听接口
 * @author sunpeng
 *
 */
public interface DownloadListener {

	//开始下载之前
	public void beforeDownload(int maxLengeh, String filePath);
	//正在下载
	public void onDownloading(int progress, String filePath);
	//下载完成（下载失败，路径返回空）
	public void onDownloadComplete(String filePath);
	//下载失败
	public void onDownloadFailed(String error);
}
