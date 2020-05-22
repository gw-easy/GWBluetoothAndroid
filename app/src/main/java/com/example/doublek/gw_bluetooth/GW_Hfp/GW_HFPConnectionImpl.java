package com.example.doublek.gw_bluetooth.GW_Hfp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Message.GW_BluetoothMessage;
import com.example.doublek.gw_bluetooth.GW_Message.GW_BluetoothMessageDispatcher;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.io.Closeable;
import java.io.IOException;

public class GW_HFPConnectionImpl extends BroadcastReceiver implements Closeable {
    static String TAG = GW_HFPConnectionImpl.class.getSimpleName();

    private GW_HFPConnectionImpl(){

    }

    @Override
    public void close() throws IOException {
        try {
            GW_Bluetooth.getContext().unregisterReceiver(this);
        } catch(Exception e){
            throw new IOException("unregisterReceiver fail",e);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent == null){
            return ;
        }

        String cmd = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
        Object[] cmdArgs = (Object[]) intent.getExtras().get(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_ARGS);
        int cmdType = intent.getIntExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, -1);
        Parcelable device = (Parcelable)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        String cmdTypeString = GW_BluetoothUtils.getHeadsetEventTypeString(cmdType);

        String cmdArgsString = GW_BluetoothUtils.toString(cmdArgs);

        GW_BluetoothMessage<String> bluetoothMessage = GW_BluetoothMessage.obtain(cmdArgsString,GW_BluetoothConnection.Protocol.HFP);

        if(device!=null){
            bluetoothMessage.setBluetoothDevice((BluetoothDevice) device);
        }

        GW_BluetoothMessageDispatcher.dispatch(bluetoothMessage);

        Log.i(TAG, "[devybt sppconnection] cmd = " + cmd + ", args = " + cmdArgsString + " , cmdTypeString = " + cmdTypeString);
    }
}
