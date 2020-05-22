package com.example.doublek.gw_bluetooth.GW_Base;

public class GW_BluetoothService implements GW_BluetoothServiceLifecycle{
    @Override
    public boolean init() throws GW_BluetoothException {
        return false;
    }

    @Override
    public boolean destory() {
        return false;
    }
}
