package com.example.doublek.gw_bluetooth.GW_Message;

public class GW_BluetoothMessageReceiver<DataType> {
    public abstract boolean onReceive(GW_BluetoothMessage<DataType> message);
}
