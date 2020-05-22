package com.example.doublek.gw_bluetooth.GW_Connection;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;

public class GW_BluetoothConnectionTimeoutException extends GW_BluetoothException {
    private static final long serialVersionUID = 1L;
    public GW_BluetoothConnectionTimeoutException(String message) {
        super(message);
    }

    public GW_BluetoothConnectionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
