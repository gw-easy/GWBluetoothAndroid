package com.example.doublek.gw_bluetooth.GW_Base;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class GW_BluetoothProfileServiceTemplate implements GW_BluetoothProfileService{
//    标记
    protected final String TAG = GW_BluetoothProfileServiceTemplate.class.getSimpleName();

//    监听蓝牙连接状态改变
    protected BluetoothProfileConnectionStateChangedListener profileConnectionStateListener = BluetoothProfileConnectionStateChangedListener.EMPTY;

    protected GW_BluetoothProfileService decorater = GW_BluetoothProfileService.EMPTY;

    //    蓝牙接口类型
    protected int profileType;

    protected ConnectionStateListenerArgs listenerArgs;
//    系统蓝牙配置接口
    protected BluetoothProfile realService;
//    是否请求
    protected volatile boolean requesting = false;

//    蓝牙配置服务状态监听
    private BluetoothProfileServiceStateListener headsetServiceListener = BluetoothProfileServiceStateListener.EMPTY;
    @Override
    public boolean init() throws GW_BluetoothException {
        requestProfileService();
        return true;
    }

    @Override
    public boolean destory() {
        if(realService!=null){
            GW_Bluetooth.closeBluetoothProfile(profileType, realService);
        }
        return true;
    }

    public GW_BluetoothProfileServiceTemplate(int profileType){
        this.profileType = profileType;
        listenerArgs = make();
    }

    public GW_BluetoothProfileServiceTemplate(int profileType,GW_BluetoothProfileService decorater) {
        this(profileType);
        this.decorater = decorater == null ? GW_BluetoothProfileService.EMPTY : decorater;
    }

    protected ConnectionStateListenerArgs make() {
        return ConnectionStateListenerArgs.EMPTY;
    }

//    连接状态监听接口
    public interface ConnectionStateListenerArgs {

        public static ConnectionStateListenerArgs EMPTY = new ConnectionStateListenerArgs() {
            @Override
            public String extraPreState() {
                return GW_BluetoothUtils.EMPTY_STRING;
            }

            @Override
            public String extraNewState() {
                return GW_BluetoothUtils.EMPTY_STRING;
            }

            @Override
            public String extraDevice() {
                return GW_BluetoothUtils.EMPTY_STRING;
            }

            @Override
            public String action() {
                return GW_BluetoothUtils.EMPTY_STRING;
            }
        };

        public String action();
        public String extraNewState();
        public String extraPreState();
        public String extraDevice();
    }

    protected void requestProfileService(){

        if(requesting) {
            Log.i(TAG, "[devybt connect] requesting profile("+profileType+") service");
            return;
        }

        Log.i(TAG, "[devybt connect] start request profile("+profileType+") service");

        requesting = true;
        GW_Bluetooth.getProfileService(profileType, serviceListener);
    }

    BluetoothProfile.ServiceListener serviceListener = new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {

            String profileString = GW_BluetoothUtils.getProfileString(profile);
            Log.i(TAG, "[devybt connect] onServiceDisconnected profile = " + profileString);

            requesting = false;
            realService = null;

            onRealServiceDisconnected();

        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            final String profileString = GW_BluetoothUtils.getProfileString(profile);
            Log.i(TAG, "[devybt connect] onServiceConnected profile = " + profileString);
            requesting = false;
            realService = proxy;

            onRealServiceConnected();

        }
    };


//    取消蓝牙
    protected void onRealServiceDisconnected() {
        Log.i(TAG, "[devybt connect] onRealServiceDisconnected enter , listener impl = " + profileConnectionStateListenerImpl);
        if(profileConnectionStateListenerImpl!=null){
            GW_Bluetooth.getContext().unregisterReceiver(profileConnectionStateListenerImpl);
            profileConnectionStateListenerImpl = null;
        }
    }

    protected void onRealServiceConnected() {

        Log.i(TAG, "[devybt connect] onRealServiceConnected enter");
        if(!TextUtils.isEmpty(listenerArgs.action())) {
            profileConnectionStateListenerImpl = new ProfileConnectionStateListenerImpl(profileType);
            GW_Bluetooth.getContext().registerReceiver(profileConnectionStateListenerImpl,new IntentFilter(listenerArgs.action()));
        }

        if(headsetServiceListener!=null){
            headsetServiceListener.onServiceReady(profileType,this);
        }

    }

    protected ProfileConnectionStateListenerImpl profileConnectionStateListenerImpl;

    private class ProfileConnectionStateListenerImpl extends BroadcastReceiver {

        private int profile;

        public ProfileConnectionStateListenerImpl(int profile){
            this.profile = profile;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            final String profileString = GW_BluetoothUtils.getProfileString(profile);

            int newState = intent.getIntExtra(listenerArgs.extraNewState(), -1);
            int previousState = intent.getIntExtra(listenerArgs.extraPreState(), -1);

            Log.i(TAG, "[devybt connect] onServiceConnected profile = " + profileString + " , registerReceiver connection state changed");

            Log.i(TAG, "[devybt connect] onServiceConnected profile = " + profileString + " , newState = " + GW_BluetoothUtils.getConnectionStateString(newState) + " , pre state = " + GW_BluetoothUtils.getConnectionStateString(previousState));

            BluetoothDevice device = intent.getParcelableExtra(listenerArgs.extraDevice());

            GW_BluetoothUtils.dumpBluetoothDevice("BluetoothProfileServiceDecorater", device);

            BluetoothProfileConnectionStateChangedListener lis = getListener();

            if(lis!=null){
                if(newState == BluetoothHeadset.STATE_CONNECTED) {
                    lis.onConnected(profile, newState, previousState, device);
                } else if(newState == BluetoothHeadset.STATE_DISCONNECTED) {
                    lis.onDisconnected(profile, newState, previousState, device);
                }

            }

        }
    }


    protected BluetoothProfileConnectionStateChangedListener getListener(){
        return profileConnectionStateListener;
    }

//    获取连接状态
    @Override
    public ProfileConnectionState getConnectionState(BluetoothDevice device) {
        if(realService == null) return ProfileConnectionState.DISCONNECTED;

        int state = realService.getConnectionState(device);

        return ProfileConnectionState.toState(state);
    }

//    获取优先级
    @Override
    public int getPriority(BluetoothDevice device) {
        if(realService == null) return BluetoothProfile.PRIORITY_OFF;

        return getPriority2(device, realService);
    }

//    设置优先级
    @Override
    public boolean setPriority(int priority, BluetoothDevice device) {
        if(realService == null) return false;

        return setPriority2(priority, device, realService);
    }

    @Override
    public boolean disconnect(BluetoothDevice device) {
        if(realService == null) return false;
        return disconnect2(device, realService);
    }

    @Override
    public boolean connect(BluetoothDevice device) {
        if(realService == null) return false;
        return connect2(device, realService);
    }

    @Override
    public List<BluetoothDevice> getConnectedBluetoothDeviceList() {
        if(realService == null) return new ArrayList<BluetoothDevice>(0);
        return realService.getConnectedDevices();
    }

    @Override
    public List<BluetoothDevice> getConnectedBluetoothDeviceList(String deviceName) {
        if(realService == null) return new ArrayList<BluetoothDevice>(0);

        List<BluetoothDevice> bluetoothDevices = getConnectedBluetoothDeviceList();

        List<BluetoothDevice> result = new ArrayList<BluetoothDevice>();

        for(BluetoothDevice device : bluetoothDevices){
            if(device.getName().equals(deviceName)){

                result.add(device);
            }
        }

        return result;
    }

    @Override
    public void registerProfileConnectionStateChangedListener(GW_BluetoothProfileService.BluetoothProfileConnectionStateChangedListener lis) {
        if(this.decorater!=null){
            this.decorater.registerProfileConnectionStateChangedListener(lis);
        }

        this.profileConnectionStateListener = (lis == null ? BluetoothProfileConnectionStateChangedListener.EMPTY : lis);
    }

    @Override
    public void unregisterProfileConnectionStateChangedListener(GW_BluetoothProfileService.BluetoothProfileConnectionStateChangedListener lis) {

    }

    @Override
    public void registerBluetoothProfileServiceListener(BluetoothProfileServiceStateListener lis) {

    }

    @Override
    public void unregisterBluetoothProfileServiceListener(BluetoothProfileServiceStateListener lis) {

    }

    protected boolean disconnect2(BluetoothDevice device, BluetoothProfile profile) {
        if(this.profileType == BluetoothProfile.A2DP) {

            BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;

            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothA2dp.class.getMethod("disconnect",
                        BluetoothDevice.class);
                Object result = connectMethod.invoke(bluetoothA2dp, device);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if(this.profileType == BluetoothProfile.HEADSET) {
            BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothHeadset.class.getMethod("disconnect",
                        BluetoothDevice.class);
                Object result = connectMethod.invoke(bluetoothHeadset, device);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    protected boolean connect2(BluetoothDevice device,BluetoothProfile profile){
        if(this.profileType == BluetoothProfile.A2DP) {

            BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothA2dp.class.getMethod("connect",
                        BluetoothDevice.class);
                Object result = connectMethod.invoke(bluetoothA2dp, device);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if(this.profileType == BluetoothProfile.HEADSET) {
            BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothHeadset.class.getMethod("connect",
                        BluetoothDevice.class);
                Object result = connectMethod.invoke(bluetoothHeadset, device);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    protected boolean setPriority2(int priority, BluetoothDevice device,BluetoothProfile profile){
        if(this.profileType == BluetoothProfile.A2DP) {

            BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothA2dp.class.getMethod("setPriority",
                        BluetoothDevice.class,int.class);
                Object result = connectMethod.invoke(bluetoothA2dp, device,priority);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if(this.profileType == BluetoothProfile.HEADSET) {
            BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothHeadset.class.getMethod("setPriority",
                        BluetoothDevice.class,int.class);
                Object result = connectMethod.invoke(bluetoothHeadset, device,priority);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    protected int getPriority2(BluetoothDevice device,BluetoothProfile profile){
        if(this.profileType == BluetoothProfile.A2DP) {

            BluetoothA2dp bluetoothA2dp = (BluetoothA2dp) profile;
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothA2dp.class.getMethod("getPriority",
                        BluetoothDevice.class,int.class);
                Object result = connectMethod.invoke(bluetoothA2dp, device);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if(this.profileType == BluetoothProfile.HEADSET) {
            BluetoothHeadset bluetoothHeadset = (BluetoothHeadset) profile;
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），进行连接。
                Method connectMethod =BluetoothHeadset.class.getMethod("getPriority",
                        BluetoothDevice.class,int.class);
                Object result = connectMethod.invoke(bluetoothHeadset, device);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }
}
