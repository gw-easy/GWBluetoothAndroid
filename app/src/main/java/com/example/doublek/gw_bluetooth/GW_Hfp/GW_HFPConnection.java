package com.example.doublek.gw_bluetooth.GW_Hfp;

import android.bluetooth.BluetoothDevice;

import com.example.doublek.gw_bluetooth.GW_Connection.GW_AbstractBluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.util.UUID;

public class GW_HFPConnection extends GW_AbstractBluetoothConnection<GW_HFPConnectionImpl> {

    private int companyid;
    volatile boolean isConnected = false;

    public GW_HFPConnection(){
        this(GW_BluetoothUtils.UNKNOW);
    }

    public GW_HFPConnection(int companyid) {
        this(null,null,companyid);
    }

    public GW_HFPConnection(UUID sppuuid, BluetoothDevice connectedDevice, int companyid) {
        super(sppuuid, connectedDevice);
        this.companyid = companyid;
    }

    public void setCompanyid(int companyid) {
        this.companyid = companyid;
    }

}
