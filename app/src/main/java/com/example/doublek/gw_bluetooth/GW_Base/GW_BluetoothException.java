package com.example.doublek.gw_bluetooth.GW_Base;

public class GW_BluetoothException extends Exception{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public GW_BluetoothException(String message, Throwable cause) {
        super(message, cause);
    }

    public GW_BluetoothException(String message) {
        super(message);
    }

    public GW_BluetoothException(Throwable cause) {
        super(cause);
    }
}
