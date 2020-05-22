package com.example.doublek.gw_bluetooth.GW_Message;

public interface GW_BluetoothMessageHandler<DataType>{
    public void handle(GW_BluetoothMessage<DataType> bluetoothMessage);
}
