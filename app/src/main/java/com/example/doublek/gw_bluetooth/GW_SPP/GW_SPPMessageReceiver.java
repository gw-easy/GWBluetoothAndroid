package com.example.doublek.gw_bluetooth.GW_SPP;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GW_SPPMessageReceiver extends Thread{
    private static final String TAG = GW_SPPMessageReceiver.class.getSimpleName();
    private InputStream mInStream;
    private OutputStream mOutStream;
    private volatile boolean isStopped = false;
    private BluetoothDevice bluetoothDevice;
    private GW_SPPBluetoothMessageParser<?> messageParser;

    public GW_SPPMessageReceiver(BluetoothSocket socket, GW_SPPBluetoothMessageParser<?> parser, BluetoothDevice bluetoothDevice) {
        Log.i(TAG, "create SPPMessageReader");

        this.messageParser = parser;
        this.bluetoothDevice = bluetoothDevice;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }

        mInStream = tmpIn;
        mOutStream = tmpOut;
    }

    public void stopReceiver(){
        isStopped = true;
        closeStream();
    }

    public void closeStream() {
        try {
            GW_BluetoothUtils.close(mInStream);
            GW_BluetoothUtils.close(mOutStream);
        } finally {
            mInStream = null;
            mOutStream = null;
        }

    }

}
