package com.example.doublek.gw_bluetooth.GW_Connection;

import android.bluetooth.BluetoothDevice;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

public interface GW_Connection<TransactCore extends Closeable> {

    public long getTimeout();
    public void connect() throws GW_BluetoothConnectionException , GW_BluetoothConnectionTimeoutException , GW_BluetoothException;
    public void connect(UUID sppuuid,BluetoothDevice connectedDevice) throws GW_BluetoothConnectionException , GW_BluetoothConnectionTimeoutException , GW_BluetoothException;
    public boolean isConnected();

    UUID getUuid();

    public BluetoothDevice getBluetoothDevice();
    public GW_BluetoothConnection.State getState();
    public void disconnect();
    public static GW_Connection<?> EMPTY = new GW_Connection() {

        @Override
        public long getTimeout() {
            return -1;
        }

        @Override
        public void connect() throws GW_BluetoothConnectionException,
                GW_BluetoothConnectionTimeoutException, GW_BluetoothException {
            throw new GW_BluetoothConnectionException("empty");
        }

        @Override
        public void connect(UUID sppuuid, BluetoothDevice connectedDevice)
                throws GW_BluetoothConnectionException,
                GW_BluetoothConnectionTimeoutException, GW_BluetoothException {
            throw new GW_BluetoothConnectionException("empty");
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void cancel() {
        }

        @Override
        public void reset() {
        }

        @Override
        public Closeable getCore() {
            return new Closeable() {

                @Override
                public void close() throws IOException {
                }
            };
        }

        @Override
        public UUID getUuid() {
            return UUID.randomUUID();
        }

        @Override
        public BluetoothDevice getBluetoothDevice() {
            return null;
        }

        @Override
        public GW_BluetoothConnection.State getState() {
            return GW_BluetoothConnection.State.UNKNOW;
        }
    };
}
