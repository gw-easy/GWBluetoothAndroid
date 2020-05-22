package com.example.doublek.gw_bluetooth.GW_SCO;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BaseBluetoothStateChangedListener;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_StateInformation;
import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionStateListener;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

public class GW_BluetoothSCOService extends GW_BluetoothService {

    private static final String TAG = GW_BluetoothSCOService.class.getSimpleName();

    private GW_BluetoothConnectionStateListener mScoStateListener = GW_BluetoothConnectionStateListener.EMTPY;
    //    sco状态监听
    private final ScoStateListenerImpl stateListenerImpl = new ScoStateListenerImpl();


    public GW_BluetoothSCOService(){
    }

    @Override
    public boolean init() throws GW_BluetoothException {
        super.init();
        stateListenerImpl.startListener();
        return true;
    }

    @Override
    public boolean destory() {
        super.destory();
        stateListenerImpl.stopListener();
        mScoStateListener = null;
        return true;
    }

    private class ScoStateListenerImpl extends GW_BaseBluetoothStateChangedListener {

        public ScoStateListenerImpl(){
        }

        @Override
        public boolean onChanged(GW_StateInformation information) {

            if(GW_Bluetooth.isDebugable()){
                GW_BluetoothUtils.dumpBluetoothScoStateInfos(TAG, information.getIntent());
            }

            int currentState = information.getCurrentState();

            switch (currentState) {
                case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                    mScoStateListener.onConnecting(information);
                    break;
                case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                    mScoStateListener.onConnected(information);
                    break;
                case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                    mScoStateListener.onDisconnected(information);
                    break;
                case AudioManager.SCO_AUDIO_STATE_ERROR:
                    mScoStateListener.onError(information);
                    break;
                default:
                    break;
            }

            return true;
        }


        @Override
        public String[] actions() {
            return new String[]{AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED};
        }


    }

    AudioManager getAudioManager(){
        return (AudioManager) GW_Bluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
    }

    public void startSco(){
        AudioManager am = getAudioManager();
        boolean isBluetoothScoOn = am.isBluetoothScoOn();

        Log.i(TAG, "[devybt sco] startTryConnectSco startSco enter , isBluetoothScoOn = " + isBluetoothScoOn);
        if(!am.isBluetoothScoOn()) {
            am.setBluetoothScoOn(true);
            am.startBluetoothSco();
        }
        Log.i(TAG, "[devybt sco] startTryConnectSco startSco exit");
    }

    public void stopSco(){
        AudioManager am = getAudioManager();
        am.stopBluetoothSco();
        am.setBluetoothScoOn(false);
    }
}
