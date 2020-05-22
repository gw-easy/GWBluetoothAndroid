package com.example.doublek.gw_bluetooth.GW_Base;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;

import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionStateListener;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("MissingPermission")
public class GW_BluetoothAdapterService extends GW_BluetoothService{
    private static String TAG = GW_BluetoothAdapterService.class.getSimpleName();

    private GW_BluetoothAdapterStateListener mAdapterStateListener = GW_BluetoothAdapterStateListener.EMPTY;

    private final BluetoothAdapterStateListenerImpl mAdapterStateListenerImpl = new BluetoothAdapterStateListenerImpl();

    public GW_BluetoothAdapterService(){
    }

    @Override
    public boolean init() throws GW_BluetoothException {
        super.init();
        mAdapterStateListenerImpl.startListener();
        return true;
    }

    @Override
    public boolean destory() {
        super.destory();
        mAdapterStateListenerImpl.stopListener();
        mAdapterStateListener = null;
        return true;
    }

    /**
     * 打开蓝牙
     */

    public void enable(){
        BluetoothAdapter.getDefaultAdapter().enable();
    }

    /**
     * 系统蓝牙开关是否打开
     * @return true:打开
     */
    public boolean isEnable(){
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    /**
     * 断开蓝牙
     * @return
     */
    public boolean disable(){
        return BluetoothAdapter.getDefaultAdapter().disable();
    }

    public void setBluetoothAdapterStateListener(GW_BluetoothAdapterStateListener lis){
        this.mAdapterStateListener = (lis!=null ? lis : GW_BluetoothAdapterStateListener.EMPTY);
    }

//    获取连接状态
    public int getConnectionState() {

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        try {
            Method m = adapter.getClass().getDeclaredMethod("getConnectionState", null);

            try {
                Object result = m.invoke(adapter, null);

                if(result!=null){
                    return Integer.valueOf(result.toString());
                }

            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                e.printStackTrace();
            }

        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }

        return BluetoothAdapter.STATE_DISCONNECTED;
    }

    public boolean getProfileService(final int profileParam,final BluetoothProfile.ServiceListener serviceListener){

        boolean result = BluetoothAdapter.getDefaultAdapter().getProfileProxy(GW_Bluetooth.getContext(), new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceDisconnected(int profile) {

                if(serviceListener!=null){
                    serviceListener.onServiceDisconnected(profile);
                }

            }

            public void onServiceConnected(int profile, BluetoothProfile proxy) {

                if(serviceListener!=null){
                    serviceListener.onServiceConnected(profile, proxy);
                }

            }
        }, profileParam);

        return result;
    }

//    关闭蓝牙配置
    public void closeBluetoothProfile(int profile,BluetoothProfile bluetoothProfile) {
        if(bluetoothProfile==null) return ;
        BluetoothAdapter.getDefaultAdapter().closeProfileProxy(profile, bluetoothProfile);
    }

    private class BluetoothAdapterStateListenerImpl extends GW_BaseBluetoothStateChangedListener {

        public BluetoothAdapterStateListenerImpl() {
        }

        @Override
        public boolean onChanged(GW_StateInformation information) {

            if(GW_Bluetooth.isDebugable()) {
                GW_BluetoothUtils.dumpBluetoothSystemSwitchStateInfos(TAG, information.getIntent());
            }

            int currentState = information.getCurrentState();

            switch (currentState) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    mAdapterStateListener.onOpening(information);
                    break;
                case BluetoothAdapter.STATE_ON:
                    mAdapterStateListener.onOpened(information);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    mAdapterStateListener.onCloseing(information);
                    break;
                case BluetoothAdapter.STATE_OFF:
                    mAdapterStateListener.onClosed(information);
                    break;
                default:
                    break;
            }

            return true;
        }

        @Override
        public String[] actions() {
            return new String[]{BluetoothAdapter.ACTION_STATE_CHANGED};
        }

    }
}
