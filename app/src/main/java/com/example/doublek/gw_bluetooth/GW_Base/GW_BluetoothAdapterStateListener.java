package com.example.doublek.gw_bluetooth.GW_Base;

public class GW_BluetoothAdapterStateListener {
    public static final GW_BluetoothAdapterStateListener EMPTY = new GW_BluetoothAdapterStateListener() {};

    /**
     * 正在开启
     */
    public void onOpening(GW_StateInformation stateInformation){}
    /**
     * 已开启
     */
    public void onOpened(GW_StateInformation stateInformation){}
    /**
     * 关闭中
     */
    public void onCloseing(GW_StateInformation stateInformation){}
    /**
     * 已关闭
     */
    public void onClosed(GW_StateInformation stateInformation){}
}
