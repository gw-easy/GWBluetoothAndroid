package com.example.doublek.gw_bluetooth.GW_Connection;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;

public class GW_BluetoothConnectionException extends GW_BluetoothException {
    public GW_BluetoothConnectionException(String message) {
        super(message);
    }

    public GW_BluetoothConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public GW_BluetoothConnectionException(Throwable cause) {
        super(cause);
    }

    private static final long serialVersionUID = 1L;
}
