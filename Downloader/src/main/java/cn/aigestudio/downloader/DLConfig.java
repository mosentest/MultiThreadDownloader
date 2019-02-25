package cn.aigestudio.downloader;

import cn.aigestudio.downloader.bizs.DLCons;

/**
 * 作者 : moziqi
 * 邮箱 : 709847739@qq.com
 * 时间   : 2019/2/25-11:30
 * desc   :
 * version: 1.0
 */
public class DLConfig {

    /**
     * 设置下载数据库的名字  xxxx.db
     *
     * @param download_db_name
     */
    public static void setDownloadDBName(String download_db_name) {
        DLCons.DB_NAME = download_db_name;
    }

    /**
     * 是否开启log
     *
     * @param debug
     */
    public static void isDebug(boolean debug) {
        DLCons.DEBUG = debug;
    }

}
