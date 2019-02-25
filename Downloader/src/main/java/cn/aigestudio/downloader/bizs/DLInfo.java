package cn.aigestudio.downloader.bizs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.aigestudio.downloader.interfaces.IDListener;

/**
 * 下载实体类
 * Download entity.
 *
 * @author AigeStudio 2015-05-16
 */
public class DLInfo {
    public int totalBytes;
    public int currentBytes;
    public String fileName;
    public String dirPath;
    public String baseUrl;
    public String realUrl;

    int redirect;
    boolean hasListener;
    boolean isResume;
    boolean isStop;
    String mimeType;
    String eTag;
    String disposition;
    String location;
    List<DLHeader> requestHeaders;
    final List<DLThreadInfo> threads;
    IDListener listener;
    File file;

    long createTime = System.currentTimeMillis();//表示这个对象创建的时间

    DLInfo() {
        threads = new ArrayList<DLThreadInfo>();
    }

    synchronized void addDLThread(DLThreadInfo info) {
        threads.add(info);
    }

    synchronized void removeDLThread(DLThreadInfo info) {
        boolean contains = threads.contains(info);
        if (contains) {
            threads.remove(info);
        }
    }

    /**
     * DLManager.java 需要比较
     * if (DEBUG) Log.w(TAG, "Downloading urls is out of range.");
     * boolean contains = TASK_PREPARE.contains(info);
     * if (!contains) {
     * TASK_PREPARE.add(info);
     * }
     *
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DLInfo dlInfo = (DLInfo) o;

        if (!baseUrl.equals(dlInfo.baseUrl)) return false;
        return realUrl.equals(dlInfo.realUrl);
    }

    @Override
    public int hashCode() {
        int result = baseUrl.hashCode();
        result = 31 * result + realUrl.hashCode();
        return result;
    }
}