package com.example.doublek.gw_bluetooth.GW_Connection;

import com.example.doublek.gw_bluetooth.GW_Base.GW_StateInformation;

public class GW_BluetoothConnectionStateListener {

    public static final GW_BluetoothConnectionStateListener EMTPY = new GW_BluetoothConnectionStateListener() {
    };

    public void onConnecting(GW_StateInformation stateInformation) {
    }

    public void onConnected(GW_StateInformation stateInformation) {
    }

    public void onDisconnected(GW_StateInformation stateInformation) {
    }

    public void onDisconnecting(GW_StateInformation stateInformation) {
    }

    public void onError(GW_StateInformation stateInformation) {
    }
}
