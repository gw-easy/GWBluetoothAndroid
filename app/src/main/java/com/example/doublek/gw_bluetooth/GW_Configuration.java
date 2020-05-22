package com.example.doublek.gw_bluetooth;

import com.example.doublek.gw_bluetooth.GW_Message.GW_BluetoothMessageDispatcher;
import com.example.doublek.gw_bluetooth.GW_Message.GW_BluetoothMessageHandler;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

public class GW_Configuration {

    public static final GW_Configuration DEFAULT = new Builder()
            .setDebug(true)
            .setSupport(true)
            .setConnectionMode(GW_Bluetooth.ConnectionMode.BLUETOOTH_WIREDHEADSET_SPEAKER)
            .build();

    private boolean debug;
    private boolean isSupport;
    private boolean debugThread;
    private GW_BluetoothMessageHandler<?> messageDispatcher;
    private boolean isSupportScoDaemon = true;
    private int companyid = GW_BluetoothUtils.UNKNOW;
    private GW_Bluetooth.ConnectionMode connectionMode = GW_Bluetooth.ConnectionMode.BLUETOOTH_WIREDHEADSET_SPEAKER;
    private int forceTypes;
    private GW_Configuration(){}

    public boolean isDebug() {
        return debug;
    }

    public int getCompanyId(){
        return this.companyid;
    }

    public int getForceTypes(){
        return forceTypes;
    }

    public static class Builder {

        private boolean debug;
        private boolean isSupportBMS;
        private boolean debugThread;
        private int companyid;
        private GW_BluetoothMessageHandler<?> messageDispatcher;
        private GW_Bluetooth.ConnectionMode connectionMode;
        private int forceTypes;

        public Builder(){
        }

        public Builder setForceTypes(int forceTypes){
            this.forceTypes = forceTypes;
            return this;
        }

        public Builder setCompanyId(int companyid) {
            this.companyid = companyid;
            return this;
        }

        public Builder setConnectionMode(GW_Bluetooth.ConnectionMode mode) {
            this.connectionMode = mode;
            return this;
        }

        public Builder setDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder setSupport(boolean isSupport) {
            this.isSupportBMS = isSupport;
            return this;
        }

        public Builder setDebugThread(boolean debugThread) {
            this.debugThread = debugThread;
            return this;
        }

        public <DataType> Builder setMessageDispatcher(GW_BluetoothMessageHandler<DataType> dispatch){
            messageDispatcher = dispatch;
            return this;
        }

        public GW_Configuration build(){
            GW_Configuration configuration = new GW_Configuration();

            configuration.debug = this.debug;
            configuration.debugThread = this.debugThread;
            configuration.isSupport = this.isSupportBMS;
            configuration.messageDispatcher = this.messageDispatcher;
            configuration.companyid = this.companyid;
            configuration.connectionMode = this.connectionMode;
            configuration.forceTypes = this.forceTypes;

            if(configuration.messageDispatcher == null){
                configuration.messageDispatcher = GW_BluetoothMessageDispatcher.getDispatcher();
            }

            return configuration;
        }

    }

    public <DataType> GW_BluetoothMessageHandler<DataType> getDispatcher(){
        return (GW_BluetoothMessageHandler<DataType>) this.messageDispatcher;
    }
}
