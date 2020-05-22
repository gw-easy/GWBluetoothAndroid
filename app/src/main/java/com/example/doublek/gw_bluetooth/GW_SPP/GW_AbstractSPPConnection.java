package com.example.doublek.gw_bluetooth.GW_SPP;

import android.bluetooth.BluetoothSocket;

import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_AbstractBluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

public class GW_AbstractSPPConnection extends GW_AbstractBluetoothConnection<BluetoothSocket> {
    static final String TAG = GW_AbstractSPPConnection.class.getSimpleName();

    protected GW_SPPMessageReceiver messageReceiver;
    @Override
    public boolean isConnected(){
        return transactCore!=null ? transactCore.isConnected() : false;
    }

    @Override
    public GW_BluetoothConnection.State getState(){
        return GW_BluetoothUtils.getBluetoothSocketState(this.transactCore);
    }

    public void onConnected(){

        if(messageReceiver!=null){
            messageReceiver.stopReceiver();
        }

        GW_SPPBluetoothMessageParser<String> parser = GW_Bluetooth.getSppMessageParser();
        messageReceiver = new GW_SPPMessageReceiver(this.transactCore,parser == null ? new GW_DefaultSPPMessageParser() : parser,this.getBluetoothDevice());
        messageReceiver.start();
    }

    @Override
    protected void closeTransactCore() {
        super.closeTransactCore();

        if(messageReceiver!=null){
            messageReceiver.closeStream();
            messageReceiver = null;
        }

    }
}
