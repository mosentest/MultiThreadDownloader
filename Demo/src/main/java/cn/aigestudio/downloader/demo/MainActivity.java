package cn.aigestudio.downloader.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.File;

import cn.aigestudio.downloader.bizs.DLManager;
import cn.aigestudio.downloader.interfaces.SimpleDListener;

public class MainActivity extends Activity {
    /**
     * http://www.anzhi.com/免费测试的apk
     */
    private static final String[] URLS = {
            "https://raw.githubusercontent.com/chenupt/DragTopLayout/master/imgs/sample-debug-1.2.1.apk",
            "http://yapkwww.cdn.anzhi.com/data4/apk/201808/07/8980366ee6df5297363324bf7e09fff1_70066700.apk",
            "http://yapkwww.cdn.anzhi.com/data4/apk/201808/07/6e95a178757f05e8ca56608d923e5a80_32638800.apk",
            "http://yapkwww.cdn.anzhi.com/data4/apk/201808/07/73bc34791719d8272cf682456cc49059_26505900.apk",
            "http://yapkwww.cdn.anzhi.com/data4/apk/201807/30/2320db1fcfc851b13c507de245724d52_07973100.apk",
            "http://yapkwww.cdn.anzhi.com/data4/apk/201808/07/e4033d387ee80560c6a9037bdab978a7_68722700.apk",
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
                            null, null, new SimpleDListener() {
                                @Override
                                public void onStart(String fileName, String realUrl, int fileLength) {
                                    pbDLs[finalI].setMax(fileLength);
                                }

                                @Override
                                public void onProgress(int progress) {
                                    pbDLs[finalI].setProgress(progress);
                                }

                                @Override
                                public void onFinish(File file) {
                                    super.onFinish(file);
                                    Log.i("moziqi", "onFinish.." + file.getAbsolutePath());
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
