package cn.aigestudio.downloader.bizs;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.aigestudio.downloader.utils.HttpsUtils;

import static cn.aigestudio.downloader.bizs.DLCons.Base.DEFAULT_TIMEOUT;

class DLThread implements Runnable {
    private static final String TAG = DLThread.class.getSimpleName();

    private DLThreadInfo dlThreadInfo;
    private DLInfo dlInfo;
    private IDLThreadListener listener;

    private Context context;

    public DLThread(Context context, DLThreadInfo dlThreadInfo, DLInfo dlInfo, IDLThreadListener listener) {
        this.context = context;
        this.dlThreadInfo = dlThreadInfo;
        this.listener = listener;
        this.dlInfo = dlInfo;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        HttpURLConnection conn = null;
        RandomAccessFile raf = null;
        InputStream is = null;
        try {
            //如果不支持多线程的话
            boolean supportMultiThread = DLManager.getInstance(context).isSupportMultiThread();
            if (!supportMultiThread) {
                if (dlInfo.file.exists() && dlInfo.file.length() == dlInfo.totalBytes) {
                    if (DLCons.DEBUG) {
                        Log.d(TAG, "DLThread.The file which we want to download was already here.");
                    }
                    //如果存在就回调出吧，别卡在这里吧
                    listener.onProgress(dlInfo.totalBytes);
                    listener.onFinish(null);
                    return;
                }
            }
//            conn = (HttpURLConnection) new URL(dlInfo.realUrl).openConnection();
            conn = HttpsUtils.https(new URL(dlInfo.realUrl));
            conn.setConnectTimeout(DEFAULT_TIMEOUT);
            conn.setReadTimeout(DEFAULT_TIMEOUT);

            addRequestHeaders(conn);

            raf = new RandomAccessFile(dlInfo.file, "rwd");
            raf.seek(dlThreadInfo.start);

            is = conn.getInputStream();

            byte[] b = new byte[4096];
            int len;
            while (!dlThreadInfo.isStop && (len = is.read(b)) != -1) {
                dlThreadInfo.start += len;
                raf.write(b, 0, len);
                listener.onProgress(len);
            }
            if (dlThreadInfo.isStop) {
                if (DLCons.DEBUG) {
                    Log.d(TAG, "Thread " + dlThreadInfo.id + " will be stopped.");
                }
                listener.onStop(dlThreadInfo);
            } else {
                if (DLCons.DEBUG) {
                    Log.d(TAG, "Thread " + dlThreadInfo.id + " will be finished.");
                }
                listener.onFinish(dlThreadInfo);
            }
        } catch (Exception e) {
            listener.onStop(dlThreadInfo);
            if (DLCons.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (null != is) is.close();
                if (null != raf) raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (null != conn) conn.disconnect();
        }
    }

    private void addRequestHeaders(HttpURLConnection conn) {
        for (DLHeader header : dlInfo.requestHeaders) {
            conn.addRequestProperty(header.key, header.value);
        }
        conn.setRequestProperty("Range", "bytes=" + dlThreadInfo.start + "-" + dlThreadInfo.end);
    }
}