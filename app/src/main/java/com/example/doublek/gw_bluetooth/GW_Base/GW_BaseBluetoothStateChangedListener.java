package com.example.doublek.gw_bluetooth.GW_Base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.doublek.gw_bluetooth.GW_Bluetooth;

//蓝牙状态监听基类
public abstract class GW_BaseBluetoothStateChangedListener extends BroadcastReceiver {

//    监听是否开启
    private volatile boolean mCalled;

    public abstract boolean onChanged(GW_StateInformation information);
    public abstract String[] actions();

    public void startListener() throws GW_BluetoothException {

        check();
        String[] actions = actions();

        if(actions == null || actions.length == 0){
            throw new GW_BluetoothException("listener actions is empty");
        }

        synchronized (this) {
            IntentFilter stateFilter = new IntentFilter();
            for(int i = 0;i < actions.length;i++){
                stateFilter.addAction(actions[i]);
            }
            GW_Bluetooth.getContext().registerReceiver(this, stateFilter);
            mCalled = true;
        }
    }

    public void stopListener() {

        check();
        synchronized (this) {
            if (mCalled) {
                try {
                    GW_Bluetooth.getContext().unregisterReceiver(this);
                } finally {
                    mCalled = false;
                }
            }
        }
    }

    void check(){

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        GW_StateInformation information = GW_StateInformation.toInformation(intent);

        onChanged(information);
    }
}
