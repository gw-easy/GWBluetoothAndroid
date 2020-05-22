package com.example.doublek.gw_bluetooth.GW_Base;

import android.os.Handler;
import android.os.HandlerThread;

public class GW_BluetoothThread extends HandlerThread implements GW_Executor{
    private static GW_BluetoothThread sInstance;
    private static Handler sHandler;

    private GW_BluetoothThread() {
        super("bt.runtime.base", android.os.Process.THREAD_PRIORITY_DEFAULT);
    }

    @Override
    public boolean execute(Runnable runnable) {
        if(runnable!=null){
            synchronized (GW_BluetoothThread.class) {
                getHandler().post(runnable);
            }
            return true;
        }
        return false;
    }

    public static GW_BluetoothThread get() {
        synchronized (GW_BluetoothThread.class) {
            ensureThreadLocked();
            return sInstance;
        }
    }

    static Handler getHandler() {
        synchronized (GW_BluetoothThread.class) {
            ensureThreadLocked();
            return sHandler;
        }
    }

//    创建线程
    private static void ensureThreadLocked() {
        if (sInstance == null) {
            sInstance = new GW_BluetoothThread();
            sInstance.start();
            sHandler = new Handler(sInstance.getLooper());
        }
    }
}
