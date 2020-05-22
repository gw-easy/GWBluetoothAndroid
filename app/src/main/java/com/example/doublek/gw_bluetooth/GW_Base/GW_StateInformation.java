package com.example.doublek.gw_bluetooth.GW_Base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

public class GW_StateInformation {

//    初始化
    static final GW_StateInformation EMPTY = new GW_StateInformation();
//    当前的链接状态
    private int currentState = GW_BluetoothUtils.UNKNOW;
//    之前的链接状态
    private int previousState = GW_BluetoothUtils.UNKNOW;
//    设备
    private BluetoothDevice device;

    private String broadcastAction = GW_BluetoothUtils.EMPTY_STRING;

    private Intent intent;
//    获取蓝牙各个连接信息
    public static GW_StateInformation toInformation(Intent intent){

        if(intent == null) return GW_StateInformation.EMPTY;

        int currentState = GW_BluetoothUtils.UNKNOW;
        if(intent.hasExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE)){
            currentState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, GW_BluetoothUtils.UNKNOW);
        }

        int previousState = GW_BluetoothUtils.UNKNOW;

        if(intent.hasExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE)) {
            previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, GW_BluetoothUtils.UNKNOW);
        }

        BluetoothDevice device = null;
        if(intent.hasExtra(BluetoothDevice.EXTRA_DEVICE)) {
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        }

        String broadcastAction = intent.getAction();
        return obtain(currentState, previousState, device, broadcastAction,intent);
    }

    public static GW_StateInformation obtain(int currentState,int previousState,BluetoothDevice device,String broadcastAction,Intent intent){

        GW_StateInformation information = new GW_StateInformation();

        information.currentState = currentState;
        information.previousState = previousState;
        information.device = device;
        information.broadcastAction = broadcastAction;
        information.intent = intent;

        return information;
    }

    public int getCurrentState() {
        return currentState;
    }

    public Intent getIntent() {
        return intent;
    }
}
