package com.example.doublek.gw_bluetooth.GW_Hfp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileServiceTemplate;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUuid;

public class GW_HeadsetProfileService extends GW_BluetoothProfileServiceTemplate implements GW_BluetoothHeadsetProfileService{

    private GW_BluetoothHeadsetProfileService.BluetoothHeadsetAudioStateListener audioStateListener;

    //    文件类型
    public static final int PROFILE = BluetoothProfile.HEADSET;

//    hfp配置uuid
    public static final ParcelUuid[] UUIDS = {
            GW_BluetoothUuid.HSP,
            GW_BluetoothUuid.Handsfree,
    };

    public GW_HeadsetProfileService() {
        super(PROFILE);
    }

    @Override
    public void registerAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis) {
        audioStateListener = lis;
    }

    @Override
    public void unregisterAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis) {
        audioStateListener = null;
    }

    @Override
    public boolean isAudioConnected(BluetoothDevice device) {
        if(realService == null) return false;
        return ((BluetoothHeadset) realService).isAudioConnected(device);
    }

    @Override
    public boolean disconnectAudio() {
        if(realService == null) return false;
        return ((BluetoothHeadset) realService).disconnectAudio();
    }

    @Override
    public boolean connectAudio() {
        if(realService == null) return false;
        return ((BluetoothHeadset) realService).connectAudio();
    }

    @Override
    public int getAudioState(BluetoothDevice device) {
        if(realService == null) return BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
        return ((BluetoothHeadset) realService).getAudioState(device);
    }

    @Override
    public boolean isAudioOn() {
        if(realService == null) return false;
        return ((BluetoothHeadset) realService).isAudioOn();
    }
}
