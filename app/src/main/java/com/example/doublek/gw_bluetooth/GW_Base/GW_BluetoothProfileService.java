package com.example.doublek.gw_bluetooth.GW_Base;

import android.bluetooth.BluetoothDevice;

import java.util.List;

public interface GW_BluetoothProfileService extends GW_BluetoothServiceLifecycle{


//    获取连接状态
    public ProfileConnectionState getConnectionState(final BluetoothDevice device);

//    获取优先级
    public int getPriority(final BluetoothDevice device);
//    设置优先级
    public boolean setPriority(final int priority, final BluetoothDevice device);

    /**
     * 断开连接
     */
    public boolean disconnect(final BluetoothDevice device) ;
    /**
     * 连接
     */
    public boolean connect(final BluetoothDevice device) ;

//    获取蓝牙列表
    public List<BluetoothDevice> getConnectedBluetoothDeviceList();
//    获取指定姓名的蓝牙列表
    public List<BluetoothDevice> getConnectedBluetoothDeviceList(final String deviceName);

    //    注册蓝牙配置服务连接状态
    public void registerProfileConnectionStateChangedListener(BluetoothProfileConnectionStateChangedListener lis);
    public void unregisterProfileConnectionStateChangedListener(BluetoothProfileConnectionStateChangedListener lis);


    //    注册蓝牙配置服务状态
    public void registerBluetoothProfileServiceListener(BluetoothProfileServiceStateListener lis);
    public void unregisterBluetoothProfileServiceListener(BluetoothProfileServiceStateListener lis);


    //    蓝牙状态监听者
    public interface BluetoothProfileServiceStateListener {
        public static final BluetoothProfileServiceStateListener EMPTY = new BluetoothProfileServiceStateListener(){
            @Override
            public void onServiceReady(int profile,GW_BluetoothProfileService service) {
            }
        };
        public void onServiceReady(int profile, GW_BluetoothProfileService service);
    }

    public interface BluetoothProfileConnectionStateChangedListener {

        public static final BluetoothProfileConnectionStateChangedListener EMPTY = new BluetoothProfileConnectionStateChangedListener() {
            @Override
            public void onDisconnected(int profile, int newState, int preState,
                                       BluetoothDevice device) {
            }
            @Override
            public void onConnected(int profile, int newState, int preState,
                                    BluetoothDevice device) {
            }
        };

        public void onConnected(int profile, int newState, int preState, BluetoothDevice device);
        public void onDisconnected(int profile, int newState, int preState, BluetoothDevice device);
    }

    /**
     * 设备连接状态
     */
    public enum ProfileConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        CONNECTED_NO_PHONE,
        CONNECTED_NO_MEDIA,
        CONNECTED_NO_PHONE_AND_MEIDA;

        public static ProfileConnectionState toState(int state) {

            ProfileConnectionState[] states = ProfileConnectionState.values();

            for(ProfileConnectionState item : states){
                if(item.ordinal() == state){
                    return item;
                }
            }

            return ProfileConnectionState.DISCONNECTED;
        }

        public static boolean isConnected(ProfileConnectionState state) {

            switch (state) {
                case CONNECTED:
                case CONNECTED_NO_PHONE:
                case CONNECTED_NO_MEDIA:
                case CONNECTED_NO_PHONE_AND_MEIDA:
                    return true;
            }

            return false;
        }
    }

    public interface BluetoothProfileConnectionStateChangedListener {

        public static final BluetoothProfileConnectionStateChangedListener EMPTY = new BluetoothProfileConnectionStateChangedListener() {
            @Override
            public void onDisconnected(int profile, int newState, int preState,
                                       BluetoothDevice device) {
            }
            @Override
            public void onConnected(int profile, int newState, int preState,
                                    BluetoothDevice device) {
            }
        };

        public void onConnected(int profile, int newState, int preState, BluetoothDevice device);
        public void onDisconnected(int profile, int newState, int preState, BluetoothDevice device);
    }


    public static final GW_BluetoothProfileService EMPTY = new GW_BluetoothProfileService() {


        @Override
        public ProfileConnectionState getConnectionState(BluetoothDevice device) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ProfileConnectionState getCurrentConnectionState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<BluetoothDevice> getConnectedBluetoothDeviceList() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<BluetoothDevice> getConnectedBluetoothDeviceList(
                String deviceName) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void registerProfileConnectionStateChangedListener(GW_BluetoothProfileService.BluetoothProfileConnectionStateChangedListener lis) {

        }

        @Override
        public void unregisterProfileConnectionStateChangedListener(GW_BluetoothProfileService.BluetoothProfileConnectionStateChangedListener lis) {

        }

        @Override
        public boolean disconnect(BluetoothDevice device) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean connect(BluetoothDevice device) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int getPriority(BluetoothDevice device) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean setPriority(int priority, BluetoothDevice device) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void registerBluetoothProfileServiceListener(
                BluetoothProfileServiceStateListener lis) {

        }

        @Override
        public void unregisterBluetoothProfileServiceListener(
                BluetoothProfileServiceStateListener lis) {

        }

        @Override
        public boolean init() throws GW_BluetoothException {
            return false;
        }

        @Override
        public boolean destory() {
            return false;
        }
    };
}
