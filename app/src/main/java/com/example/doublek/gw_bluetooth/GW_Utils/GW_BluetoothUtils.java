package com.example.doublek.gw_bluetooth.GW_Utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_A2dp.GW_A2dpProfileService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothRuntimeException;
import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_Connection;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HeadsetProfileService;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

@SuppressLint("MissingPermission")
public class GW_BluetoothUtils {

    public static final int UNKNOW = -1000;
    public static final String EMPTY_STRING = "";

    public static final Runnable EMPTY_TASK = new Runnable() {
        @Override
        public void run() {
        }
    };

//    创建线程工厂
    public static ThreadFactory createThreadFactory(final String name){
        return new ThreadFactory() {
            int count = 0;
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,name+"-"+(++count));
            }
        };
    }

//    获取蓝牙连接状态
    public static GW_BluetoothConnection.State getBluetoothSocketState(BluetoothSocket socket){
        if(socket==null) return GW_BluetoothConnection.State.UNKNOW;
        try {
            java.lang.reflect.Field f = socket.getClass().getDeclaredField("mSocketState");

            f.setAccessible(true);

            Object obj = f.get(socket);

            if(obj!=null){
                return GW_BluetoothConnection.State.valueOf(obj.toString());
            }

        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return GW_BluetoothConnection.State.UNKNOW;
    }

    //    打印蓝牙设备信息
    public static void dumpBluetoothDevice(String tag, BluetoothDevice bluetoothDevice) {
        dumpBluetoothDevice(tag,bluetoothDevice,false);
    }

    @SuppressLint("MissingPermission")
    public static void dumpBluetoothDevice(String tag, BluetoothDevice bluetoothDevice, boolean isDumpUUIDS) {
        if(bluetoothDevice!=null){

            Log.i(tag, "[devybt device] [ dumpBluetoothDevice enter ]");

            Log.i(tag, "[devybt device] =============================getName = " + bluetoothDevice.getName());
            Log.i(tag, "[devybt device] getDeviceBondState = " + getDeviceBondState(bluetoothDevice.getBondState()));
            Log.i(tag, "[devybt device] getAddress = " + bluetoothDevice.getAddress());
            Log.i(tag, "[devybt device] getBluetoothClass = " + getBluetoothClassString(bluetoothDevice.getBluetoothClass()));

            boolean isSupportA2DP = GW_Bluetooth.isSupportA2DP(bluetoothDevice);
            boolean isSuuportHFP = GW_Bluetooth.isSupportHFP(bluetoothDevice);

            Log.i(tag, "[devybt device] isSupportA2DP = " + isSupportA2DP);
            Log.i(tag, "[devybt device] isSuuportHFP = " + isSuuportHFP);


            if(isSuuportHFP){
                GW_BluetoothProfileService.ProfileConnectionState connectionState = GW_Bluetooth.HFP.getConnectionState(bluetoothDevice);
                boolean isAudioConnected = GW_Bluetooth.HFP.isAudioConnected(bluetoothDevice);
                int priority = GW_Bluetooth.getPriority(GW_HeadsetProfileService.PROFILE,bluetoothDevice);

                Log.i(tag, "[devybt device] HFP connectionState = " + connectionState);
                Log.i(tag, "[devybt device] HFP isAudioConnected = " + isAudioConnected);
                Log.i(tag, "[devybt device] HFP priority = " + GW_BluetoothUtils.getDevicePriority(priority));

            }

            if(isSupportA2DP){
                GW_BluetoothProfileService.ProfileConnectionState connectionState = GW_Bluetooth.getConnectionState(GW_A2dpProfileService.PROFILE, bluetoothDevice);
                boolean isA2dpPlaying = GW_Bluetooth.A2DP.isA2dpPlaying(bluetoothDevice);
                int priority = GW_Bluetooth.getPriority(GW_A2dpProfileService.PROFILE,bluetoothDevice);

                Log.i(tag, "[devybt device] A2DP connectionState = " + connectionState);
                Log.i(tag, "[devybt device] A2DP isA2dpPlaying = " + isA2dpPlaying);
                Log.i(tag, "[devybt device] A2DP priority = " + GW_BluetoothUtils.getDevicePriority(priority));

            }


            if(isDumpUUIDS){
                ParcelUuid[] parcelUuidArray = bluetoothDevice.getUuids();

                if(parcelUuidArray!=null){
                    for(int i = 0;i < parcelUuidArray.length;i++) {
                        Log.i(tag, "[devybt device] getUuids index("+i+") = " + parcelUuidArray[i].toString());
                    }
                }
            }

            Log.i(tag, "[devybt device] [ dumpBluetoothDevice exit ]");
        } else {
            Log.i(tag, "[devybt device] [ dumpBluetoothDevice enter ,  bluetoothDevice null]");
        }
    }

//    打印蓝牙连接状态
    public static void dumpBluetoothConnection(String tag,GW_Connection connection) {

        if(connection!=null){

            Log.i(tag, "[devybt connection] dumpBluetoothConnection enter --------------");

            Log.i(tag, "[devybt connection] isConnected = " + connection.isConnected());
            Log.i(tag, "[devybt connection] getTimeout = " + connection.getTimeout());
            Log.i(tag, "[devybt connection] getState = " + connection.getState());

            dumpBluetoothDevice(tag, connection.getBluetoothDevice(), false);

            Log.i(tag, "[devybt connection] dumpBluetoothConnection exit --------------");
        }

    }

//    打印蓝牙sco状态
    public static void dumpBluetoothScoStateInfos(String tag,Intent intent){
        if(intent == null || tag == null) return ;
        int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
        int previousState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1);
//        当前状态
        String stateStr = GW_BluetoothUtils.getScoStateStringFromAudioManager(state);
//        之前状态
        String preStateStr = GW_BluetoothUtils.getScoStateStringFromAudioManager(previousState);

        Log.i(tag, "[devybt sco] pre state = " + preStateStr + " , current state" + stateStr);

    }

//    打印系统选择的状态
    public static void dumpBluetoothSystemSwitchStateInfos(String tag,Intent intent){
        if(intent == null || tag == null) return ;
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
        int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);

        String stateStr = GW_BluetoothUtils.getBluetoothSwitchStateString(state);
        String preStateStr = GW_BluetoothUtils.getBluetoothSwitchStateString(previousState);

        Log.i(tag, "[devybt connect switch] pre state = " + preStateStr + " , current state = " + stateStr);

    }

//    打印配置连接属性

    public static void dumpProfileConnectionMap(String TAG, LinkedHashMap<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> map){
        for(Iterator<Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState>> iter = map.entrySet().iterator(); iter.hasNext();){

            Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> item = iter.next();

            final BluetoothDevice bluetoothDevice = item.getKey();
            GW_BluetoothProfileService.ProfileConnectionState state = item.getValue();

            boolean isSupportHFP = GW_Bluetooth.isSupportHFP(bluetoothDevice);
            boolean isSupportA2dp = GW_Bluetooth.isSupportA2DP(bluetoothDevice);

            Log.i(TAG, "allProfileConnectionState device name = " + bluetoothDevice.getName() + " , state = " + state + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);

        }
    }

//    打印连接信息
    public static void dumpBluetoothConnectionInfos(String tag,Intent intent){
        if(intent == null) return ;
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
        int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, -1);
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        String stateStr = GW_BluetoothUtils.getConnectionStateString(state);
        String preStateStr = GW_BluetoothUtils.getConnectionStateString(previousState);

        String deviceName = "unkown device";
        if (device != null) {
            deviceName = device.getName();
        }
        Log.i(tag, "[devybt connect] pre state = " + preStateStr + " , current state = " + stateStr + " , device = " + deviceName);
    }


//    获取头事件
    public static String getHeadsetEventTypeString(int cmdType) {

        String cmdTypeString = "UNKNOW";
        switch (cmdType) {
            case BluetoothHeadset.AT_CMD_TYPE_ACTION:
                cmdTypeString = "AT_CMD_TYPE_ACTION";
                break;
            case BluetoothHeadset.AT_CMD_TYPE_BASIC:
                cmdTypeString = "AT_CMD_TYPE_BASIC";
                break;
            case BluetoothHeadset.AT_CMD_TYPE_READ:
                cmdTypeString = "AT_CMD_TYPE_READ";
                break;
            case BluetoothHeadset.AT_CMD_TYPE_SET:
                cmdTypeString = "AT_CMD_TYPE_SET";
                break;
            case BluetoothHeadset.AT_CMD_TYPE_TEST:
                cmdTypeString = "AT_CMD_TYPE_TEST";
                break;
        }

        return cmdTypeString;
    }

//    获取蓝牙开关状态
    public static String getBluetoothSwitchStateString(int state){
        String stateStr = "";
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                stateStr = "STATE_TURNING_ON";
                break;
            case BluetoothAdapter.STATE_ON:
                stateStr = "STATE_ON";
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                stateStr = "STATE_TURNING_OFF";
            case BluetoothAdapter.STATE_OFF:
                stateStr = "STATE_OFF";
            default:
                break;
        }
        return stateStr;
    }

//    获取sco-audio连接状态
    public static String getScoStateStringFromAudioManager(int state) {
        String stateStr = "UNKNOW";
        switch (state) {
            case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                stateStr = "SCO_AUDIO_STATE_CONNECTING";
                break;
            case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                stateStr = "SCO_AUDIO_STATE_CONNECTED";
                break;
            case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                stateStr = "SCO_AUDIO_STATE_DISCONNECTED";
                break;
            case AudioManager.SCO_AUDIO_STATE_ERROR:
                stateStr = "SCO_AUDIO_STATE_ERROR";
                break;
        }
        return stateStr;
    }


    //    获取蓝牙设备类型
    public static String getBluetoothClassString(BluetoothClass btClass){
        if (btClass != null) {
            switch (btClass.getMajorDeviceClass()) {
                case BluetoothClass.Device.Major.COMPUTER:
                    return "COMPUTER";

                case BluetoothClass.Device.Major.PHONE:
                    return "PHONE";

                case BluetoothClass.Device.Major.PERIPHERAL:
                    return "PERIPHERAL";

                case BluetoothClass.Device.Major.IMAGING:
                    return "IMAGING";

                default:
                    return "UNKNOW";
            }
        } else {
            return "UNKNOW2";
        }
    }
//获取蓝牙设备绑定状态
    public static String getDeviceBondState(int mode) {

        String modeStr = null;
        switch (mode) {
            case BluetoothDevice.BOND_NONE:
                modeStr = "BOND_NONE";
                break;
            case BluetoothDevice.BOND_BONDING:
                modeStr = "BOND_BONDING";
                break;
            case BluetoothDevice.BOND_BONDED:
                modeStr = "BOND_BONDED";
                break;
            default:
                modeStr = "mode("+mode+") MODE_???????";
                break;
        }
        return modeStr;
    }

//    获取蓝牙配置类型
    public static String getProfileString(int profile){
        switch (profile) {
            case BluetoothProfile.A2DP:
                return "A2DP";
            case BluetoothProfile.HEADSET:
                return "HEADSET";
            case BluetoothProfile.HEALTH:
                return "HEALTH";
            default:
                return "UNKNOW";
        }
    }

//    获取设备优先级
    public static String getDevicePriority(int priority) {

        String result = null;
        switch (priority) {
            case BluetoothProfile.PRIORITY_AUTO_CONNECT:
                result = "PRIORITY_AUTO_CONNECT";
                break;
            case BluetoothProfile.PRIORITY_ON:
                result = "PRIORITY_ON";
                break;
            case BluetoothProfile.PRIORITY_OFF:
                result = "PRIORITY_OFF";
                break;
            case BluetoothProfile.PRIORITY_UNDEFINED:
                result = "PRIORITY_UNDEFINED";
                break;
            default:
                result = "UNKNOW";
                break;
        }
        return result;
    }

//    获取sco连接状态
    public static String getScoStateStringFromHeadsetProfile(int state) {
        String stateStr = "";
        switch (state) {
            case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                stateStr = "CONNECTED";
                break;
            case BluetoothHeadset.STATE_AUDIO_CONNECTING:
                stateStr = "CONNECTING";
                break;
            case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                stateStr = "DISCONNECTED";
                break;
            default:
                stateStr = "UNKNOW";
                break;
        }
        return stateStr;
    }

    //    判断是否为null
    public static void ifNullThrowException(Object...objects){
        for(int i = 0;i < objects.length;i++){
            if(objects[i] == null){
                throw new GW_BluetoothRuntimeException("method parametors is null");
            }
        }
    }


//    获取蓝牙设备连接管理器状态
    public static String getConnectionStateString(int state) {
        String stateStr = "UNKNOW";
        switch (state) {
            case BluetoothAdapter.STATE_CONNECTING:
                stateStr = "STATE_CONNECTING";
                break;
            case BluetoothAdapter.STATE_CONNECTED:
                stateStr = "STATE_CONNECTED";
                break;
            case BluetoothAdapter.STATE_DISCONNECTING:
                stateStr = "STATE_DISCONNECTING";
                break;
            case BluetoothAdapter.STATE_DISCONNECTED:
                stateStr = "STATE_DISCONNECTED";
                break;
        }
        return stateStr;
    }

    public static void close(Closeable closeable){
        if(closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
