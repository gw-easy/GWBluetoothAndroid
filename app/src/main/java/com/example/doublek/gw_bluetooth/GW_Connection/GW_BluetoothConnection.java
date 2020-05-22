package com.example.doublek.gw_bluetooth.GW_Connection;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import com.example.doublek.gw_bluetooth.GW_A2dp.GW_A2dpProfileService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HFPConnection;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HeadsetProfileService;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GW_BluetoothConnection {

    public static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * 连接状态
     * @author wei.deng
     */
    public enum State {
        UNKNOW,
        INIT,
        CONNECTED,
        LISTENING,
        CLOSED,
    }
    /**
     * 支持的协议
     * @author wei.deng
     *
     */
    public static class Protocol {
        private String name;

        public static final Protocol A2DP = new Protocol("a2dp");

        public static final Protocol HFP = new Protocol("hfp");

        public static final Protocol SPP = new Protocol("spp");

        public static final Protocol[] ALL = new Protocol[]{A2DP,HFP,SPP};

        public Protocol(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Protocol getProtocol(ParcelUuid uuid){
            throw new RuntimeException("stub");
        }

        public static Protocol getProtocol(int profile){
            if(profile == GW_HeadsetProfileService.PROFILE) {
                return Protocol.HFP;
            } else if(profile == GW_A2dpProfileService.PROFILE) {
                return Protocol.A2DP;
            }
            return Protocol.SPP;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            Protocol p1 = (Protocol) obj;
            return p1.getName().equals(this.name);
        }

    }

    /**
     * 连接监听器
     * @author wei.deng
     */
    public static abstract class BluetoothConnectionListener {
        public void onConnected(GW_Connection connection) {
        }

        public void onDisconnected(GW_Connection connection) {
        }
    }

    /**
     * sppconnection builder
     * @author wei.deng
     */
    public static class Builder {
        private BluetoothDevice connectedDevice;
        private UUID serviceUUID;
        private GW_RetryPolicy connectionRetryPolicy;
        private BluetoothConnectionListener connectListener;
        private long connectionTimeout;
        private int companyid = GW_BluetoothUtils.UNKNOW;
        final List<GW_Connection> connectPolicyList = new ArrayList<GW_Connection>();

        public Builder(){
        }

        public Builder setConnectedDevice(BluetoothDevice connectedDevice) {
            this.connectedDevice = connectedDevice;
            return this;
        }

        public Builder setCompanyid(int companyid) {
            this.companyid = companyid;
            return this;
        }

        public Builder setConnectionUUID(UUID sppUUID) {
            this.serviceUUID = sppUUID;
            return this;
        }

        public Builder setConnectionRetryPolicy(GW_RetryPolicy connectionRetryPolicy) {
            this.connectionRetryPolicy = connectionRetryPolicy;
            return this;
        }

        public Builder addDefaultConnectionRetryPolicy(){
            this.connectionRetryPolicy = new GW_DefaultRetryPolicy();
            return this;
        }

        public Builder setConnectionListener(BluetoothConnectionListener lis) {
            this.connectListener = lis;
            return this;
        }

        public Builder addConnectionPolicy(GW_Connection connectPolicy) {
            if(!this.connectPolicyList.contains(connectPolicy)) {
                this.connectPolicyList.add(connectPolicy);
            }
            return this;
        }

        public Builder setConnectionTimeout(long timeout){
            this.connectionTimeout = timeout;
            return this;
        }

        public GW_Connection build() throws GW_BluetoothException {
            if(connectPolicyList.size()>0){
                for (GW_Connection connection : connectPolicyList) {
                    if(connection instanceof GW_AbstractBluetoothConnection<?>) {
                        GW_AbstractBluetoothConnection<?> abstractBluetoothConnection = (GW_AbstractBluetoothConnection<?>) connection;
                        abstractBluetoothConnection.connectedDevice = this.connectedDevice;
                        abstractBluetoothConnection.sppuuid = this.serviceUUID;
                        abstractBluetoothConnection.setTimeout(connectionTimeout);

                        if(abstractBluetoothConnection instanceof GW_HFPConnection){
                            GW_HFPConnection hfpConnection = (GW_HFPConnection) abstractBluetoothConnection;
                            hfpConnection.setCompanyid(this.companyid);
                        }

                    }
                }
            }
            return GW_BluetoothConnectionImpl.open(this.serviceUUID, this.connectedDevice, this.connectionRetryPolicy, this.connectListener,this.connectPolicyList.toArray(new GW_Connection[]{}));
        }

    }
}
