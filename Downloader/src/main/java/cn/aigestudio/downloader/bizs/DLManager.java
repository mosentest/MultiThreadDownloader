package cn.aigestudio.downloader.bizs;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.aigestudio.downloader.interfaces.IDListener;

import static cn.aigestudio.downloader.bizs.DLCons.DEBUG;
import static cn.aigestudio.downloader.bizs.DLError.ERROR_INVALID_URL;
import static cn.aigestudio.downloader.bizs.DLError.ERROR_NOT_NETWORK;
import static cn.aigestudio.downloader.bizs.DLError.ERROR_REPEAT_URL;

/**
 * 下载管理器
 * Download manager
 * 执行具体的下载操作
 *
 * @author AigeStudio 2015-05-09
 * 开始一个下载任务只需调用{@link #dlStart}方法即可
 * 停止某个下载任务需要调用{@link #dlStop}方法 停止下载任务仅仅会将对应下载任务移除下载队列而不删除相应数据 下次启动相同任务时会自动根据上一次停止时保存的数据重新开始下载
 * 取消某个下载任务需要调用{@link #dlCancel}方法 取消下载任务会删除掉相应的本地数据库数据但文件不会被删除
 * 相同url的下载任务视为相同任务
 * Use {@link #dlStart} for a new download task.
 * Use {@link #dlStop} to stop a download task base on url.
 * Use {@link #dlCancel} to cancel a download task base on url.
 * By the way, the difference between {@link #dlStop} and {@link #dlCancel} is whether the data in database would be deleted or not,
 * for example, the state of download like local file and data in database will be save when you use {@link #dlStop} stop a download task,
 * if you use {@link #dlCancel} cancel a download task, anything related to download task would be deleted.
 * @author AigeStudio 2015-05-26
 * 对不支持断点下载的文件直接使用单线程下载 该操作将不会插入数据库
 * 对转向地址进行解析
 * 更改下载线程分配逻辑
 * DLManager will download with single thread if server does not support break-point, and it will not insert to database
 * Support url redirection.
 * Change download thread size dispath.
 * @author AigeStudio 2015-05-29
 * 修改域名重定向后无法多线程下载问题
 * 修改域名重定向后无法暂停问题
 * Bugfix:can not start multi-threads to download file when we in url redirection.
 * Bugfix:can not stop a download task when we in url redirection.
 * @author zhangchi 2015-10-13
 * Bugfix：修改多次触发任务时的并发问题，防止同时触发多个相同的下载任务；修改任务队列为线程安全模式；
 * 修改多线程任务的线程数量设置机制，每个任务可以自定义设置下载线程数量；通过同构方法dlStart(String url, String dirPath, DLTaskListener listener,int threadNum)；
 * 添加日志开关及日志记录，开关方法为setDebugEnable，日志TAG为DLManager；方便调试;
 * @author AigeStudio 2015-10-23
 * 修复大部分已知Bug
 * 优化代码逻辑适应更多不同的下载情况
 * 完善错误码机制，使用不同的错误码标识不同错误的发生，详情请参见{@link DLError}
 * 不再判断网络类型只会对是否联网做一个简单的判断
 * 修改{@link #setDebugEnable(boolean)}方法
 * 新增多个不同的{@link #dlStart}方法便于回调
 * 新增{@link #setMaxTask(int)}方法限制多个下载任务的并发数
 * @author AigeStudio 2015-11-05
 * 修复较大文件下载暂停后无法续传问题
 * 修复下载无法取消问题
 * 优化线程分配
 * 优化下载逻辑提升执行效率
 * @author AigeStudio 2015-11-27
 * 新增{@link #getDLInfo(String)}方法获取瞬时下载信息
 * 新增{@link #getDLDBManager()}方法获取数据库管理对象
 * @author AigeStudio 2015-12-16
 * 修复非断点下载情况下无法暂停问题
 * 修复非断点下载情况下载完成后无法获得文件的问题
 */
public final class DLManager {
    private static final String TAG = DLManager.class.getSimpleName();

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final int POOL_SIZE = CORES + 1;
    private static final int POOL_SIZE_MAX = CORES * 2 + 1;

    private static final BlockingQueue<Runnable> POOL_QUEUE_TASK = new LinkedBlockingQueue<>(56);
    private static final BlockingQueue<Runnable> POOL_QUEUE_THREAD = new LinkedBlockingQueue<>(256);

    private static final ThreadFactory TASK_FACTORY = new ThreadFactory() {
        private final AtomicInteger COUNT = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "DLTask #" + COUNT.getAndIncrement());
        }
    };

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger COUNT = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "DLThread #" + COUNT.getAndIncrement());
        }
    };

    private static final ExecutorService POOL_TASK = new ThreadPoolExecutor(POOL_SIZE,
            POOL_SIZE_MAX, 3, TimeUnit.SECONDS, POOL_QUEUE_TASK, TASK_FACTORY);//任务的线程池
    private static final ExecutorService POOL_Thread = new ThreadPoolExecutor(POOL_SIZE * 5,
            POOL_SIZE_MAX * 5, 1, TimeUnit.SECONDS, POOL_QUEUE_THREAD, THREAD_FACTORY);//多线程执行下载的线程池

    private static final ConcurrentHashMap<String, DLInfo> TASK_DLING = new ConcurrentHashMap<>();//这是正在下载的列表
    private static final List<DLInfo> TASK_PREPARE = Collections.synchronizedList(new ArrayList<DLInfo>());//等待队列
    private static final ConcurrentHashMap<String, DLInfo> TASK_STOPPED = new ConcurrentHashMap<>();//暂停的队列

    private static DLManager sManager;

    private Context context;

    private int maxTask = 2;//同时最多2个

    private boolean isSupportMultiThread = false; //多线程下载同一个包

    private final static long timeout_download = 1 * 60 * 1000L;//1 * 60 * 60 * 1000L

    private DLManager(Context context) {
        this.context = context;
    }

    public static DLManager getInstance(Context context) {
        if (null == sManager) {
            synchronized (DLManager.class) {
                if (null == sManager) {
                    sManager = new DLManager(context.getApplicationContext());
                }
            }
        }
        return sManager;
    }

    /**
     * 设置并发下载任务最大值
     * The max task of DLManager.
     *
     * @param maxTask ...
     * @return ...
     */
    public DLManager setMaxTask(int maxTask) {
        this.maxTask = maxTask;
        return sManager;
    }

    /**
     * 设置是否开启Debug模式 默认不开启
     * Is debug mode, default is false.
     *
     * @param isDebug ...
     * @return ...
     */
    public DLManager setDebugEnable(boolean isDebug) {
        DLCons.DEBUG = isDebug;
        return sManager;
    }


    /**
     * 是否支持多线程下载同一个包，默认false
     *
     * @param supportMultiThread
     */
    public void setSupportMultiThread(boolean supportMultiThread) {
        isSupportMultiThread = supportMultiThread;
    }

    public boolean isSupportMultiThread() {
        return isSupportMultiThread;
    }

    /**
     * @see #dlStart(String, String, String, List, IDListener)
     */
    public void dlStart(String url) {
        dlStart(url, "", "", null, null);
    }

    /**
     * @see #dlStart(String, String, String, List, IDListener)
     */
    public void dlStart(String url, IDListener listener) {
        dlStart(url, "", "", null, listener);
    }

    /**
     * @see #dlStart(String, String, String, List, IDListener)
     */
    public void dlStart(String url, String dir, IDListener listener) {
        dlStart(url, dir, "", null, listener);
    }

    /**
     * @see #dlStart(String, String, String, List, IDListener)
     */
    public void dlStart(String url, String dir, String name, IDListener listener) {
        dlStart(url, dir, name, null, listener);
    }

    /**
     * 开始一个下载任务
     * Start a download task.
     *
     * @param url      文件下载地址
     *                 Download url.
     * @param dir      文件下载后保存的目录地址，该值为空时会默认使用应用的文件缓存目录作为保存目录地址
     *                 The directory of download file. This parameter can be null, in this case we
     *                 will use cache dir of app for download path.
     * @param name     文件名，文件名需要包括文件扩展名，类似“AigeStudio.apk”的格式。该值可为空，为空时将由程
     *                 序决定文件名。
     *                 Name of download file, include extension like "AigeStudio.apk". This
     *                 parameter can be null, in this case the file name will be decided by program.
     * @param headers  请求头参数
     *                 Request header of http.
     * @param listener 下载监听器
     *                 Listener of download task.
     */
    public void dlStart(String url, String dir, String name, List<DLHeader> headers, IDListener listener) {
        boolean hasListener = listener != null;
        if (TextUtils.isEmpty(url)) {
            if (hasListener) listener.onError(ERROR_INVALID_URL, "Url can not be null.");
            return;
        }
        if (!DLUtil.isNetworkAvailable(context)) {
            if (hasListener) listener.onError(ERROR_NOT_NETWORK, "Network is not available.");
            return;
        }
        if (TASK_DLING.containsKey(url)) {
            if (null != listener) listener.onError(ERROR_REPEAT_URL, url + " is downloading.");
        } else {
            DLInfo info;
            if (TASK_STOPPED.containsKey(url)) {
                if (DEBUG) Log.d(TAG, "Resume task from memory.");
                info = TASK_STOPPED.remove(url);
                if (DEBUG) Log.d(TAG, "check form database");
                if (info.threads != null) {
                    info.threads.clear();
                }
                info.threads.addAll(DLDBManager.getInstance(context).queryAllThreadInfo(url));
            } else {
                if (DEBUG) Log.d(TAG, "Resume task from database.");
                //如果是数据库还存在这个url
                info = DLDBManager.getInstance(context).queryTaskInfo(url);
                if (null != info) {
                    //清除多线程的下载的信息,这里感觉看上去多余，没啥意义
                    if (info.threads != null) {
                        info.threads.clear();
                    }
                    info.threads.addAll(DLDBManager.getInstance(context).queryAllThreadInfo(url));
                }
            }
            if (null == info) {
                if (DEBUG) Log.d(TAG, "New task will be start.");
                info = new DLInfo();
                info.baseUrl = url;
                info.realUrl = url;
                if (TextUtils.isEmpty(dir)) dir = context.getCacheDir().getAbsolutePath();
                info.dirPath = dir;
                info.fileName = name;
            } else {
                //修改为isResume true
                info.isResume = true;
                if (info.threads != null || !info.threads.isEmpty()) {
                    //修改为isStop false
                    for (DLThreadInfo threadInfo : info.threads) {
                        threadInfo.isStop = false;
                    }
                }
            }
            info.redirect = 0;
            info.requestHeaders = DLUtil.initRequestHeaders(headers, info);
            info.listener = listener;
            info.hasListener = hasListener;
            if (TASK_DLING.size() >= maxTask) {
                try {
                    //查看下TASK_DLING是否存在文件下载了1个小时都进度的，有可能是线程挂了
                    Set<Map.Entry<String, DLInfo>> entries = TASK_DLING.entrySet();
                    for (Map.Entry<String, DLInfo> temp : entries) {
                        DLInfo value = temp.getValue();
                        if (value != null) {
                            File file = value.file;
                            if (file != null && file.exists()) {
                                long time = System.currentTimeMillis() - file.lastModified();
                                if (DEBUG)
                                    Log.w(TAG, "Downloading urls is out of range..time.." + time);
                                if (time > timeout_download) {
                                    //一个小时还没下载完，就删了吧
                                    removeDLTask(temp.getKey());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    if (DEBUG) e.printStackTrace();
                }
                if (DEBUG) Log.w(TAG, "Downloading urls is out of range.");
                TASK_PREPARE.add(info);
            } else {
                boolean containsKey = TASK_DLING.containsKey(url);
                if (!containsKey) {
                    if (DEBUG) Log.d(TAG, "Prepare download from " + info.baseUrl);
                    if (hasListener) listener.onPrepare();
                    TASK_DLING.put(url, info);
                    POOL_TASK.execute(new DLTask(context, info));
                }
            }
        }
    }

    /**
     * 根据Url暂停一个下载任务
     * Stop a download task according to url.
     *
     * @param url 文件下载地址
     *            Download url.
     */
    public void dlStop(String url) {
        if (TASK_DLING.containsKey(url)) {
            DLInfo info = TASK_DLING.get(url);
            info.isStop = true;
            if (!info.threads.isEmpty()) {
                for (DLThreadInfo threadInfo : info.threads) {
                    threadInfo.isStop = true;
                }
            }
        }
    }

    /**
     * 根据Url取消一个下载任务
     * Cancel a download task according to url.
     *
     * @param url 文件下载地址
     *            Download url.
     */
    public void dlCancel(String url) {
        dlStop(url);
        DLInfo info;
        if (TASK_DLING.containsKey(url)) {
            info = TASK_DLING.get(url);
        } else {
            info = DLDBManager.getInstance(context).queryTaskInfo(url);
        }
        if (null != info) {
            File file = new File(info.dirPath, info.fileName);
            if (file.exists()) file.delete();
        }
        DLDBManager.getInstance(context).deleteTaskInfo(url);
        DLDBManager.getInstance(context).deleteAllThreadInfo(url);
    }

    public DLInfo getDLInfo(String url) {
        return DLDBManager.getInstance(context).queryTaskInfo(url);
    }

    @Deprecated
    public DLDBManager getDLDBManager() {
        return DLDBManager.getInstance(context);
    }

    synchronized DLManager removeDLTask(String url) {
        TASK_DLING.remove(url);
        return sManager;
    }

    /**
     * 暴露出来，允许监网络可以下载文件
     * @return
     */
    public synchronized DLManager addDLTask() {
        //没网络的时候不执行任何操作
        if (!DLUtil.isNetworkAvailable(context)) {
            return sManager;
        }
        if (!TASK_STOPPED.isEmpty()) { //优先执行暂停的任务
            if (TASK_DLING.size() < maxTask) {
                if (DEBUG) Log.w(TAG, "addDLTask TASK_STOPPED .Downloading urls is here");
                DLInfo dlInfo = TASK_STOPPED.remove(0);
                if (DEBUG) Log.d(TAG, "addDLTask TASK_STOPPED  check form database");
                nextDLTask(dlInfo);
            }
        } else if (!TASK_PREPARE.isEmpty()) { //没有才校验等待队列的任务
            if (TASK_DLING.size() < maxTask) {
                if (DEBUG) Log.w(TAG, "addDLTask TASK_PREPARE .Downloading urls is here");
                DLInfo removeDl = TASK_PREPARE.remove(0);
                if (DEBUG) Log.d(TAG, "addDLTask TASK_STOPPED  check form database");
                nextDLTask(removeDl);
            }
        }
        return sManager;
    }

    private void nextDLTask(DLInfo dlInfo) {
        if (dlInfo.threads != null) {
            dlInfo.threads.clear();
        }
        dlInfo.threads.addAll(DLDBManager.getInstance(context).queryAllThreadInfo(dlInfo.baseUrl));
        //这里查询数据库，判断是否isResume需要改为true
        DLInfo info = DLDBManager.getInstance(context).queryTaskInfo(dlInfo.baseUrl);
        //修改为isResume true
        if (info != null) {
            dlInfo.isResume = true;
        }
        if (dlInfo.threads != null || !dlInfo.threads.isEmpty()) {
            //修改为isStop false
            for (DLThreadInfo threadInfo : dlInfo.threads) {
                threadInfo.isStop = false;
            }
        }
        boolean containsKey = TASK_DLING.containsKey(dlInfo.baseUrl);
        if (!containsKey) {//task_dling没有才加进去
            if (dlInfo.hasListener) {
                dlInfo.listener.onPrepare();
            }
            TASK_DLING.put(dlInfo.baseUrl, dlInfo);
            POOL_TASK.execute(new DLTask(context, dlInfo));
        }
    }

    synchronized DLManager addStopTask(DLInfo info) {
        TASK_STOPPED.put(info.baseUrl, info);
        return sManager;
    }

    synchronized DLManager addDLThread(DLThread thread) {
        POOL_Thread.execute(thread);
        return sManager;
    }

}