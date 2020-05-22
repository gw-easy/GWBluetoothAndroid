package com.example.doublek.gw_bluetooth.GW_Connection;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.io.Closeable;
import java.util.UUID;

public class GW_AbstractBluetoothConnection<TransactCore extends Closeable> implements GW_Connection<Closeable>{
    static final String TAG = GW_AbstractBluetoothConnection.class.getSimpleName();

    protected BluetoothDevice connectedDevice;
    protected UUID sppuuid;
    protected volatile GW_BluetoothConnection.State currentState = GW_BluetoothConnection.State.INIT;
    protected TransactCore transactCore;
    protected volatile boolean isCancel;
//    连接超时时间
    protected long connectionTimeout = 6*1000;

    public GW_AbstractBluetoothConnection(UUID sppuuid,BluetoothDevice connectedDevice){
        this.sppuuid = sppuuid;
        this.connectedDevice = connectedDevice;
    }


    @Override
    public void connect() throws GW_BluetoothConnectionException,
            GW_BluetoothConnectionTimeoutException, GW_BluetoothException {
        connect(this.sppuuid, this.connectedDevice);
    }

    @Override
    public UUID getUuid() {
        return sppuuid;
    }

    @Override
    public BluetoothDevice getBluetoothDevice() {
        return connectedDevice;
    }

    @Override
    public long getTimeout() {
        return connectionTimeout;
    }

    public void setTimeout(long timeout) {
        this.connectionTimeout = timeout;
    }

    @Override
    public void reset() {
        isCancel = false;
        closeTransactCore();
    }

    @Override
    public void disconnect() {
        closeTransactCore();
    }

    @Override
    public GW_BluetoothConnection.State getState() {
        return this.currentState;
    }

    @Override
    public TransactCore getCore() {
        return this.transactCore;
    }

    @Override
    public void cancel() {
        Log.i(TAG, "[devybt sppconnection] cancel{"+this.getClass().getSimpleName()+"} enter , transactCore = " + transactCore);
        isCancel = true;
        closeTransactCore();
    }

    protected boolean isCancel(){
        return isCancel;
    }

    protected void closeTransactCore(){
        try {
            if(this.transactCore instanceof Closeable){
                Closeable closeable = (Closeable) this.transactCore;
                GW_BluetoothUtils.close(closeable);
            }
        } finally {
            this.transactCore = null;
            currentState = GW_BluetoothConnection.State.CLOSED;
        }
    }
}
