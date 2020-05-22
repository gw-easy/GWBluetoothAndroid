package com.example.doublek.gw_bluetooth.GW_Connection;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;

import java.util.UUID;

public class GW_DefaultRetryPolicy implements GW_RetryPolicy{

    static final String TAG = GW_DefaultRetryPolicy.class.getSimpleName();

//    默认尝试次数
    static final int DEFAULT_RETRY_COUNT = 3;

    private int maxRetryCount = DEFAULT_RETRY_COUNT;

//    当前连接尝试次数
    private int currentRetryCount = 0;
//    超时时间
    static int[] TIMEOUT = new int[]{100,500,1000};

    public GW_DefaultRetryPolicy(){
        this(DEFAULT_RETRY_COUNT);
    }

    public GW_DefaultRetryPolicy(int maxCount){
        maxRetryCount = maxCount;
    }

    @Override
    public void reset() {
        currentRetryCount = 0;
    }

    @Override
    public int getCurrentRetryCount() {
        return currentRetryCount;
    }

    @Override
    public void retry(UUID sppuuid, BluetoothDevice connectedDevice, Exception error) throws GW_BluetoothConnectionException, GW_BluetoothException {
        if(currentRetryCount>=maxRetryCount){
            throw new GW_BluetoothConnectionException("spp retry connect fail",error);
        }

        try {
            Thread.sleep(TIMEOUT[getCurrentRetryCount()]);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        currentRetryCount++;
        Log.i(TAG, "[devybt sppconnection] retryconnect count = " + currentRetryCount);

        GW_BluetoothConnectionImpl sppConnection = GW_BluetoothConnectionImpl.open(sppuuid,connectedDevice,null);
        try {
            sppConnection.connect();
        } catch (GW_BluetoothConnectionException e) {
            Log.i(TAG, "[devybt sppconnection] retryconnect BluetoothConnectionException");

            retry(sppuuid, connectedDevice, error);

        } catch(GW_BluetoothConnectionTimeoutException e){
            Log.i(TAG, "[devybt sppconnection] retryconnect BluetoothConnectionTimeoutException");

            retry(sppuuid, connectedDevice, error);

        } catch (GW_BluetoothException e) {
            throw e;
        }
    }
}
