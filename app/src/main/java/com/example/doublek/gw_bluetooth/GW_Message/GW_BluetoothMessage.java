package com.example.doublek.gw_bluetooth.GW_Message;

import android.bluetooth.BluetoothDevice;

import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;

import java.util.UUID;

public class GW_BluetoothMessage<DataType> {
    private String id;

    private long time;
    private DataType dataBody;
    private GW_BluetoothConnection.Protocol protocol;
    private BluetoothDevice bluetoothDevice;

    public GW_BluetoothMessage(){
        this.id = UUID.randomUUID().toString();
    }

    public static <DataType> GW_BluetoothMessage<DataType> obtain(DataType data,GW_BluetoothConnection.Protocol protocol){
        return obtain(System.currentTimeMillis(), data,protocol);
    }

    public static <DataType> GW_BluetoothMessage<DataType> obtain(long time,DataType data,GW_BluetoothConnection.Protocol protocol){
        GW_BluetoothMessage<DataType> message = new GW_BluetoothMessage<DataType>();
        message.time = time;
        message.dataBody = data;
        message.protocol = protocol;
        return message;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public long getTime(){
        return time;
    }

    public DataType getBodyData(){
        return dataBody;
    }

    public GW_BluetoothConnection.Protocol getProtocol(){
        return protocol;
    }
}
