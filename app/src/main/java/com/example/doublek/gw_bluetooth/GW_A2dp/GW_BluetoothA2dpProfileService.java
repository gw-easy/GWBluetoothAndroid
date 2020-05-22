package com.example.doublek.gw_bluetooth.GW_A2dp;

import android.bluetooth.BluetoothDevice;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileService;

public interface GW_BluetoothA2dpProfileService extends GW_BluetoothProfileService {

    public boolean isA2dpPlaying(BluetoothDevice device);
}
