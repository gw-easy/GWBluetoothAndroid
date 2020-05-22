package com.example.doublek.gw_bluetooth.GW_Connection;

import android.bluetooth.BluetoothDevice;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;

import java.util.UUID;

/**
 * 重连策略
 * 默认实现：{@link DefaultRetryPolicy}
 */
public interface GW_RetryPolicy {
    public void reset();
    public int getCurrentRetryCount();
    public void retry(UUID sppuuid, BluetoothDevice connectedDevice, Exception e) throws GW_BluetoothConnectionException,GW_BluetoothException;
}
