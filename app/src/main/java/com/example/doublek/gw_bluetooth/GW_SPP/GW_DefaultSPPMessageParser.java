package com.example.doublek.gw_bluetooth.GW_SPP;

import android.bluetooth.BluetoothDevice;

import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Message.GW_BluetoothMessage;

public class GW_DefaultSPPMessageParser implements GW_SPPBluetoothMessageParser<String> {
    final StringBuilder readMessage = new StringBuilder();

    @Override
    public GW_BluetoothMessage<String>[] parse(byte[] buffer, int readCount, GW_BluetoothConnection.Protocol protocol, BluetoothDevice device) {
        String readed = new String(buffer, 0, readCount);
        readMessage.append(readed);
        if (readed.contains("\n")) {
            GW_BluetoothMessage<String> message = GW_BluetoothMessage.obtain(new String(readMessage.toString()), protocol);
            readMessage.setLength(0);
            return new GW_BluetoothMessage[]{message};
        }
        return null;
    }
}
