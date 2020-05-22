package com.example.doublek.gw_bluetooth.GW_A2dp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileServiceTemplate;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothRuntimeException;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUuid;

public class GW_A2dpProfileService extends GW_BluetoothProfileServiceTemplate implements GW_BluetoothA2dpProfileService{
    public static final int PROFILE = BluetoothProfile.A2DP;

    public GW_A2dpProfileService() {
        super(PROFILE);
    }

    public GW_A2dpProfileService(GW_BluetoothProfileService decorater) {
        super(PROFILE,decorater);
    }

    public GW_A2dpProfileService(int profileType) {
        super(profileType);
        throw new GW_BluetoothRuntimeException("not support");
    }

    @Override
    public boolean isA2dpPlaying(BluetoothDevice device) {
        if(realService == null) return false;
        return ((BluetoothA2dp)realService).isA2dpPlaying(device);
    }

    //a2dp 配置uuid
    public static final ParcelUuid[] SINK_UUIDS = {
            GW_BluetoothUuid.AudioSink,
            GW_BluetoothUuid.AdvAudioDist,
    };



}
