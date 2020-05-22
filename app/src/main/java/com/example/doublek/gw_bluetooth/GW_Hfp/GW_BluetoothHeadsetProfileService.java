package com.example.doublek.gw_bluetooth.GW_Hfp;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.RestrictTo;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileService;

public interface GW_BluetoothHeadsetProfileService extends GW_BluetoothProfileService {

//    sco监听
    public interface BluetoothHeadsetAudioStateListener {
        public void onAudioConnected(BluetoothDevice bluetoothDevice, GW_BluetoothHeadsetProfileService service);
        public void onAudioDisconnected(BluetoothDevice bluetoothDevice, GW_BluetoothHeadsetProfileService service);
        public void onAudioConnecting(BluetoothDevice bluetoothDevice, GW_BluetoothHeadsetProfileService service);
    }

//    注册sco监听
    public void registerAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis);
//    取消sco监听
    public void unregisterAudioStateChangedListener(BluetoothHeadsetAudioStateListener lis);

//    sco是否连接上
    public boolean isAudioConnected(final BluetoothDevice device);

//    sco取消连接
    public boolean disconnectAudio();

//    连接sco
    public boolean connectAudio();

//    获取sco状态
    public int getAudioState(final BluetoothDevice device);

//    是否是sco模式
    public boolean isAudioOn();
}
