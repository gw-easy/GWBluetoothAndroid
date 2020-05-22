package com.example.doublek.gw_bluetooth.GW_Connection;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BaseBluetoothStateChangedListener;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_StateInformation;
import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

public class GW_BluetoothDeviceConnectionService extends GW_BluetoothService {

    private static String TAG = GW_BluetoothDeviceConnectionService.class.getSimpleName();

    private GW_BluetoothConnectionStateListener mConnectionStateListener = GW_BluetoothConnectionStateListener.EMTPY;
    private final BluetoothDeviceConnectionStateListenerImpl stateListenerImpl = new BluetoothDeviceConnectionStateListenerImpl();

    public GW_BluetoothDeviceConnectionService(){
    }

    @Override
    public boolean init() throws GW_BluetoothException {
        // TODO Auto-generated method stub
        super.init();
        stateListenerImpl.startListener();
        return true;

    }

    @Override
    public boolean destory() {
        super.destory();
        stateListenerImpl.stopListener();
        mConnectionStateListener = null;
        return true;
    }


    class BluetoothDeviceConnectionStateListenerImpl extends GW_BaseBluetoothStateChangedListener {

        public BluetoothDeviceConnectionStateListenerImpl(){
        }

        @Override
        public boolean onChanged(GW_StateInformation stateInformation){

            if(GW_Bluetooth.isDebugable()){

                int connectionState = GW_Bluetooth.getConnectionState();
                String getConnectionStateString = GW_BluetoothUtils.getConnectionStateString(connectionState);

                Log.i(TAG, "[devybt connect] getConnectionStateString = " + getConnectionStateString);

                GW_BluetoothUtils.dumpBluetoothConnectionInfos(TAG, stateInformation.getIntent());
            }

            int currentState = stateInformation.getCurrentState();

            switch (currentState) {
                case BluetoothAdapter.STATE_CONNECTING:
                    mConnectionStateListener.onConnecting(stateInformation);
                    break;
                case BluetoothAdapter.STATE_CONNECTED:
                    mConnectionStateListener.onConnected(stateInformation);
                    break;
                case BluetoothAdapter.STATE_DISCONNECTING:
                    mConnectionStateListener.onDisconnecting(stateInformation);
                    break;
                case BluetoothAdapter.STATE_DISCONNECTED:
                    mConnectionStateListener.onDisconnected(stateInformation);
                    break;

                default:
                    break;
            }

            return true;
        }

        @Override
        public String[] actions(){
            return new String[]{BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED};
        }

    }
}
