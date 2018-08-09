package cn.aigestudio.downloader.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import cn.aigestudio.downloader.bizs.DLManager;
import cn.aigestudio.downloader.interfaces.SimpleDListener;

public class MainActivity extends Activity {
    private static final String[] URLS = {
            "https://raw.githubusercontent.com/chenupt/DragTopLayout/master/imgs/sample-debug-1.2.1.apk",
            "http://cdn2.ime.sogou.com/7a5b0274064e2d89f75e3cb4d8269b26/5b46c55e/dl/index/1531213050/sogou_pinyin_90c.exe",
            "https://www.charlesproxy.com/assets/release/4.2.6/charles-proxy-4.2.6-win64.msi",
            "http://220.112.193.194/files/7021000006963E3B/dl2.xmind.cn/XMind-ZEN-Update3-for-Windows-64bit.exe",
            "http://dldir1.qq.com/weixin/Windows/WeChatSetup.exe",
            "https://www.python.org/ftp/python/2.7.15/python-2.7.15.amd64.msi"
    };

    private static final int[] RES_ID_BTN_START = {
            R.id.main_dl_start_btn1,
            R.id.main_dl_start_btn2,
            R.id.main_dl_start_btn3,
            R.id.main_dl_start_btn4,
            R.id.main_dl_start_btn5,
            R.id.main_dl_start_btn6};
    private static final int[] RES_ID_BTN_STOP = {
            R.id.main_dl_stop_btn1,
            R.id.main_dl_stop_btn2,
            R.id.main_dl_stop_btn3,
            R.id.main_dl_stop_btn4,
            R.id.main_dl_stop_btn5,
            R.id.main_dl_stop_btn6};
    private static final int[] RES_ID_PB = {
            R.id.main_dl_pb1,
            R.id.main_dl_pb2,
            R.id.main_dl_pb3,
            R.id.main_dl_pb4,
            R.id.main_dl_pb5,
            R.id.main_dl_pb6};
    private static final int[] RES_ID_NOTIFY = {
            R.id.main_notify_btn1,
            R.id.main_notify_btn2,
            R.id.main_notify_btn3,
            R.id.main_notify_btn4,
            R.id.main_notify_btn5,
            R.id.main_notify_btn6};

    private String saveDir;

    private ProgressBar[] pbDLs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DLManager.getInstance(MainActivity.this).setMaxTask(2);
        Button[] btnStarts = new Button[RES_ID_BTN_START.length];
        for (int i = 0; i < btnStarts.length; i++) {
            btnStarts[i] = (Button) findViewById(RES_ID_BTN_START[i]);
            final int finalI = i;
            btnStarts[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DLManager.getInstance(MainActivity.this).dlStart(URLS[finalI], saveDir,
                            null, null, new SimpleDListener(){
                        @Override
                        public void onStart(String fileName, String realUrl, int fileLength) {
                            pbDLs[finalI].setMax(fileLength);
                        }

                        @Override
                        public void onProgress(int progress) {
                            pbDLs[finalI].setProgress(progress);
                        }
                    });
                }
            });
        }

        Button[] btnStops = new Button[RES_ID_BTN_STOP.length];
        for (int i = 0; i < btnStops.length; i++) {
            btnStops[i] = (Button) findViewById(RES_ID_BTN_STOP[i]);
            final int finalI = i;
            btnStops[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DLManager.getInstance(MainActivity.this).dlStop(URLS[finalI]);
                }
            });
        }

        pbDLs = new ProgressBar[RES_ID_PB.length];
        for (int i = 0; i < pbDLs.length; i++) {
            pbDLs[i] = (ProgressBar) findViewById(RES_ID_PB[i]);
            pbDLs[i].setMax(100);
        }

        Button[] btnNotifys = new Button[RES_ID_NOTIFY.length];
        for (int i = 0; i < btnNotifys.length; i++) {
            btnNotifys[i] = (Button) findViewById(RES_ID_NOTIFY[i]);
            final int finalI = i;
            btnNotifys[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NotificationUtil.notificationForDLAPK(MainActivity.this, URLS[finalI]);
                }
            });
        }

        saveDir = Environment.getExternalStorageDirectory() + "/moziqi/";
    }

    @Override
    protected void onDestroy() {
        for (String url : URLS) {
            DLManager.getInstance(this).dlStop(url);
        }
        super.onDestroy();
    }
}
