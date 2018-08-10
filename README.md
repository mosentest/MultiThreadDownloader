[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-MultiThreadDownloader-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1865)
***
# Android Multi-Thread Downloader

- 由于原作者不做维护了，我在基于上修改部分代码
- 喜欢用HttpURLConnection可以考虑看看这个

# 注意
- 本库不会校验文件是否正确，只会比较大小是否一致
- 下载的时候，会比较拉取服务器的contentLength跟本地文件是否一致，不一致就会重新下载
- 需要依赖第三方开发者，在下载前和下载后，对文件做MD5或者crc32校验
- 目前我没有对多线程下载同一个文件深入研究代码逻辑，最好还是选择单线程下载单个文件
- 对stop的逻辑没做任何的调整，存在的bug，自己解决

#新增逻辑
- 增加https的支持
- 数据库的操作改为单例的数据库实例
- 增加控制单线程下载文件还是多线程下载文件
- 当前的链接请求失败自动下载下一个链接，并且在有网络的情况执行
- 增加适配v21以上的网络判断逻辑
- 增加网络变化，可以把等待队列加入到下载队列里面，提高下载率，但是需要手动添加
```java
//代码已经封装好了，在合适的地方注册和销毁就好
//<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
//<uses-permission android:name="android.permission.INTERNET"/>
NetWorkStateReceiver.registerReceiver();
NetWorkStateReceiver.unregisterReceiver();

```

#待处理问题
- 重启应用的时候，是否遍历上次失败或者挂了的本地数据，继续下载