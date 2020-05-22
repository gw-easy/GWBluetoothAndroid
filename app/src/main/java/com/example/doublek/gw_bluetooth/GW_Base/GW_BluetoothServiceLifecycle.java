package com.example.doublek.gw_bluetooth.GW_Base;

public interface GW_BluetoothServiceLifecycle {
    public boolean init() throws GW_BluetoothException;

    public boolean destory();
}
