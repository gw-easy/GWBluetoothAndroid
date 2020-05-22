package com.example.doublek.gw_bluetooth.GW_SPP;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionException;

import java.io.IOException;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class GW_SPPConnectionSecurePolicy extends GW_AbstractSPPConnection{

    @Override
    public void connect(UUID sppuuid, BluetoothDevice connectedDevice) throws GW_BluetoothConnectionException {

        Log.i(TAG, "[devybt sppconnection] SPPConnectionSecurePolicy start try connect , isCancel("+isCancel()+") , sppuuid = " + sppuuid);

        try {
            transactCore = connectedDevice.createRfcommSocketToServiceRecord(sppuuid);
        } catch (IOException e) {
            Log.i(TAG, "[devybt sppconnection] createRfcommSocketToServiceRecord IOException");
            throw new GW_BluetoothConnectionException("createRfcommSocketToServiceRecord exception", e);
        }

        // test code
//		if(exeCount % 2 == 0){
//			try {
//				Thread.sleep(24*1000);
//			} catch (InterruptedException e1) {
//				e1.printStackTrace();
//			}
//		}
//
//		exeCount++;

        try {
            if(!isCancel() && transactCore != null){
                transactCore.connect();
                if(transactCore.isConnected()){
                    onConnected();
                }
            }
        } catch (IOException e) {
            closeTransactCore();
            Log.i(TAG, "[devybt sppconnection] try connect IOException");
            throw new GW_BluetoothConnectionException("spp connect exception" , e);
        }
    }
}
