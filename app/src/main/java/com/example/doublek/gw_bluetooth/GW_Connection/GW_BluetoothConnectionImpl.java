package com.example.doublek.gw_bluetooth.GW_Connection;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GW_BluetoothConnectionImpl extends GW_AbstractBluetoothConnection<Closeable>{

    final GW_RetryPolicy retryPolicy;
    final GW_BluetoothConnection.BluetoothConnectionListener connectionListener;

    final List<GW_Connection> sppConnectPolicyList;

    private GW_BluetoothConnectionImpl(UUID sppid,BluetoothDevice connectedDevice,GW_Connection<? extends Closeable>[] connectPolicies,GW_RetryPolicy retryPolicy,GW_BluetoothConnection.BluetoothConnectionListener connectionListener){
        super(sppid,connectedDevice);
        sppConnectPolicyList = new ArrayList<GW_Connection>();
        if(connectPolicies!=null){
            for(int i = 0;i < connectPolicies.length;i++){
                sppConnectPolicyList.add(connectPolicies[i]);
            }
        }
        this.retryPolicy = retryPolicy;
        this.connectionListener = connectionListener;
    }

    static GW_BluetoothConnectionImpl open(BluetoothDevice connectedDevice) throws GW_BluetoothException {
        return open(GW_BluetoothConnection.DEFAULT_UUID,connectedDevice,new GW_DefaultRetryPolicy(),null,null);
    }

    static GW_BluetoothConnectionImpl open(UUID sppuuid,BluetoothDevice connectedDevice) throws GW_BluetoothException {
        return open(sppuuid,connectedDevice,new GW_DefaultRetryPolicy(),null,null);
    }

    static GW_BluetoothConnectionImpl open(UUID sppuuid,BluetoothDevice connectedDevice,GW_RetryPolicy policy) throws GW_BluetoothException {
        return open(sppuuid,connectedDevice,policy,null,null);
    }

    static GW_BluetoothConnectionImpl open(UUID sppuuid,BluetoothDevice connectedDevice,GW_RetryPolicy policy,GW_BluetoothConnection.BluetoothConnectionListener lis) throws GW_BluetoothException {
        return open(sppuuid,connectedDevice,policy,lis,null);
    }

    /**
     * 开启一个SPP连接
     * @param sppuuid 连接蓝牙设备时需要的ID
     * @param connectedDevice 已连接的蓝牙设备
     * @param policy 当SPP连接失败后，尝试重连的策略
     * @return
     * @throws GW_BluetoothException
     */
    static GW_BluetoothConnectionImpl open(UUID sppuuid, BluetoothDevice connectedDevice, GW_RetryPolicy policy, GW_BluetoothConnection.BluetoothConnectionListener sppConnectListener, GW_Connection[] sppConnectPolicyArray) throws GW_BluetoothException {

        if(sppuuid == null){
            throw new GW_BluetoothException("sppuuid == null");
        }

        Log.i(TAG, "[devybt btconnection] open sppuuid = " + sppuuid);

        if(connectedDevice == null){
            throw new GW_BluetoothException("connectedDevice == null");
        }

        GW_BluetoothConnectionImpl sppConnection = new GW_BluetoothConnectionImpl(sppuuid,connectedDevice,sppConnectPolicyArray,policy,sppConnectListener);
        return sppConnection;
    }
}
