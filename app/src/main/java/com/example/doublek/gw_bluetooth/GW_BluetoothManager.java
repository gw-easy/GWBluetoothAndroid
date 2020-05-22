package com.example.doublek.gw_bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileService;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionException;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionTimeoutException;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_Connection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_DefaultRetryPolicy;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HFPConnection;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HeadsetProfileService;
import com.example.doublek.gw_bluetooth.GW_SPP.GW_SPPConnectionSecurePolicy;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;

/**
 * 简介
 *
 * hfp：蓝牙控制手机通话功能的协议，让蓝牙设备可以控制电话，如接听、挂断、拒接、语音拨号等，拒接、语音拨号要视蓝牙耳机及电话是否支持。
 * spp：蓝牙串口协议，负责蓝牙间socket传输
 *
 */


@SuppressLint("MissingPermission")
public class GW_BluetoothManager {
    //    蓝牙连接
    private static GW_Connection bluetoothConnection;

    static final String TAG = GW_BluetoothManager.class.getSimpleName();

//    发现当前系统hfp蓝牙设备
    public static BluetoothDevice currentDisconnectSystemHFPBluetoothDevice;

//    播放器模式
    private static int MEIDA_PLAYER_STREAM = AudioManager.STREAM_VOICE_CALL;

    /**
     * 打印消息
     * @param message 消息
     */
    private static void showToast(String message){
        handler.sendMessage(Message.obtain(handler, 0, message));
    }

    static Handler handler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            String message = msg.obj.toString();

            Toast.makeText(GW_Bluetooth.getContext(), message, Toast.LENGTH_SHORT).show();
            return true;
        }
    });

    /**
     * 连接第一个设备的spp
     */
    public static void blueConnectSPP(){
        List<BluetoothDevice> list = GW_Bluetooth.HFP.getConnectedBluetoothDeviceList();

        if(list!=null && list.size() == 0){
            showToast("not found connected bt devices");
            return ;
        }
        final BluetoothDevice bluetoothDevice = list.get(0);
        connectspp(bluetoothDevice,true);
    }

    /**
     * 连接指定蓝牙spp
     * @param bluetoothDevice 蓝牙设备
     * @param isSupportRestartSystemHfp 是否支持但spp连接失败的时候，连接hfp
     */
    public static void connectspp(final BluetoothDevice bluetoothDevice,final boolean isSupportRestartSystemHfp){

//		disconnectspp();

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    new GW_BluetoothConnection().Builder()
                            .setConnectionUUID(GW_BluetoothConnection.DEFAULT_UUID)
//								.setConnectionUUID(UUID.randomUUID())
                            .setConnectedDevice(bluetoothDevice)
                            .setConnectionTimeout(3*1000)
//								.setConnectionRetryPolicy(new DefaultRetryPolicy())
//								.addConnectionPolicy(new SPPConnectionInsecurePolicy())
                            .addConnectionPolicy(new GW_SPPConnectionSecurePolicy())
//								.addConnectionPolicy(new SppConnectionLoopChannelPolicy())
                            .setConnectionRetryPolicy(new GW_DefaultRetryPolicy(1))
                            .setConnectionListener(new GW_BluetoothConnection.BluetoothConnectionListener() {

                                @Override
                                public void onConnected(GW_Connection connection) {
                                    Log.i(TAG, "SPP Connected");
                                    bluetoothConnection = connection;
                                    showToast("SPP Connected");
                                }

                                @Override
                                public void onDisconnected(GW_Connection connection) {
                                    Log.i(TAG, "SPP Disconnected");
                                    showToast("SPP Disconnected");
                                }

                            })
                            .build()
                            .connect();


                    Log.i(TAG, "start SPP connect , isSupportRestartSystemHfp = " + isSupportRestartSystemHfp);

                } catch (GW_BluetoothConnectionException e) {
                    e.printStackTrace();
                    Log.i(TAG, "SPP BluetoothConnectionException = " + e.getMessage());
                    showToast("SPP BluetoothConnectionException = " + e.getMessage());
//                    加入连接spp失败，自动连接hfp
                    if(isSupportRestartSystemHfp) {
                        syncRebuildHfpConnection(bluetoothDevice);
                    }

                } catch (GW_BluetoothConnectionTimeoutException e) {
                    e.printStackTrace();
                    Log.i(TAG, "SPP BluetoothConnectionException = " + e.getMessage());
                    showToast("SPP  BluetoothConnectionTimeoutException");
                } catch (GW_BluetoothException e) {
                    e.printStackTrace();
                    Log.i(TAG, "SPP BluetoothConnectionException = " + e.getMessage());
                    showToast("SPP BluetoothException");
                }
            }
        }).start();

    }

//    连接hfp
    private static void syncRebuildHfpConnection(final BluetoothDevice device){

        if(!GW_Bluetooth.isBluetoothEnable()) return ;

        boolean isConnected = GW_Bluetooth.HFP.isConnected(device);

        Log.i(TAG, "syncReconnectSystemHFP isConnected = " + isConnected + " , deivce name = " + device.getName());

        if(isConnected){

            boolean disconnectResult = GW_Bluetooth.HFP.disconnect(device);
            int priority = GW_Bluetooth.HFP.getPriority(device);
            Log.i(TAG, "disconnect = " + disconnectResult + " , priority = " + GW_BluetoothUtils.getDevicePriority(priority));

            int count = 0;
            int maxConnectCount = 3;

            while(true){
                try {
                    boolean connectResult = GW_Bluetooth.HFP.connect(device);
                    Log.i(TAG, "while item connectResult = " + connectResult + " , priority = " + GW_BluetoothUtils.getDevicePriority(priority));
                    try {
                        Thread.sleep((1*1000 + (count * 200)));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    isConnected = GW_Bluetooth.HFP.isConnected(device);
                    if(GW_Bluetooth.isBluetoothEnable() && (isConnected || (count >= maxConnectCount))){
                        break;
                    }

                    ++count;
                } catch(Throwable e){
                    break;
                }

            }

            BluetoothDevice bluetoothDevice = GW_Bluetooth.HFP.getFristConnectedDevice();
            if(bluetoothDevice!=null && GW_Bluetooth.isSupportSPP(bluetoothDevice)) {
                connectspp(device,false);
            }

        } else {

            BluetoothDevice bluetoothDevice = GW_Bluetooth.HFP.getFristConnectedDevice();
            if(bluetoothDevice!=null && GW_Bluetooth.isSupportSPP(bluetoothDevice)) {
                connectspp(bluetoothDevice,true);
            }

        }

    }

//    断开spp连接
    public static void disconnectspp(){
        if(bluetoothConnection != null){
            bluetoothConnection.disconnect();
            bluetoothConnection = null;
        }
    }

    /**
     * 连接hfp 默认连接第一个hfp
     */
    public static void connecthfp(){

        List<BluetoothDevice> list = GW_Bluetooth.HFP.getConnectedBluetoothDeviceList();

        if(list!=null && list.size() == 0){
            showToast("not found connected bt devices");
            return ;
        }

        final BluetoothDevice bluetoothDevice = list.get(0);

        connecthfp(bluetoothDevice);

    }

    /**
     * 连接hfp 指定连接蓝牙
     * @param bluetoothDevice 蓝牙设备
     *
     */

    public static void connecthfp(final BluetoothDevice bluetoothDevice){
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    new GW_BluetoothConnection().Builder()
                            .setConnectionUUID(GW_HeadsetProfileService.UUIDS[0].getUuid())
                            .setConnectedDevice(bluetoothDevice)
                            .addConnectionPolicy(new GW_HFPConnection(85))
                            .setConnectionListener(new GW_BluetoothConnection.BluetoothConnectionListener() {

                                @Override
                                public void onConnected(GW_Connection connection) {
                                    Log.i(TAG, "HFP onConnected");
                                    showToast("HFP Connected");
                                    bluetoothConnection = connection;

                                    GW_BluetoothUtils.dumpBluetoothConnection(TAG,connection);

                                }

                                @Override
                                public void onDisconnected(GW_Connection connection) {
                                    Log.i(TAG, "HFP onDisconnected");
                                    showToast("HFP Disconnected");
                                }

                            })
                            .build()
                            .connect();
                } catch (GW_BluetoothConnectionException e) {
                    e.printStackTrace();
                } catch (GW_BluetoothConnectionTimeoutException e) {
                    e.printStackTrace();
                } catch (GW_BluetoothException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 断开hfp连接
     */
    public static void disconnectBlutoothConnection(){
        if(bluetoothConnection!=null){
            bluetoothConnection.disconnect();
            GW_BluetoothUtils.dumpBluetoothConnection(TAG, bluetoothConnection);
            bluetoothConnection = null;
        }
    }

    /**
     * hfp 是否连接sco
     * @param isConnect
     * @return
     */
    public static boolean bluetoothHFPConnectSco(boolean isConnect){
        boolean result;
        if (isConnect){
            result = GW_Bluetooth.HFP.connectAudio();
        }else {
            result = GW_Bluetooth.HFP.disconnectAudio();
        }
        return result;
    }


    /**
     * AudioManager sco
     * @param isStart 是否开始sco
     */
    public static void bluetoothAudioManagerStartSco(boolean isStart){
        if (isStart){
            GW_Bluetooth.startSco();
        }else {
            GW_Bluetooth.stopSco();
        }

    }

    /**
     * 连接系统hfp
     */
    private void connectSystemHFP() {
        if(currentDisconnectSystemHFPBluetoothDevice!=null){
            boolean isSupportHFP = GW_Bluetooth.isSupportHFP(currentDisconnectSystemHFPBluetoothDevice);
            boolean isSupportA2dp = GW_Bluetooth.isSupportA2DP(currentDisconnectSystemHFPBluetoothDevice);

            Log.i(TAG, "current disconnect device name = " + currentDisconnectSystemHFPBluetoothDevice.getName() + " , state = " + currentDisconnectSystemHFPBluetoothDevice.getBondState() + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);

            boolean result = GW_Bluetooth.HFP.connect(currentDisconnectSystemHFPBluetoothDevice);

            Log.i(TAG, "connect system hfp result = " + result);

            return;
        }
    }

//    取消发现系统hfp
    private void disconnectSystemHFP() {
        LinkedHashMap<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> map = GW_Bluetooth.getAllProfileConnectionState();

        for(Iterator<Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState>> iter = map.entrySet().iterator(); iter.hasNext();){

            Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> item = iter.next();

            final BluetoothDevice bluetoothDevice = item.getKey();
            GW_BluetoothProfileService.ProfileConnectionState state = item.getValue();

            boolean isSupportHFP = GW_Bluetooth.isSupportHFP(bluetoothDevice);
            boolean isSupportA2dp = GW_Bluetooth.isSupportA2DP(bluetoothDevice);

            Log.i(TAG, "device name = " + bluetoothDevice.getName() + " , state = " + state + ", isSupportHFP = " + isSupportHFP + " , isSupportA2dp = " + isSupportA2dp);


            if(isSupportHFP) {
                currentDisconnectSystemHFPBluetoothDevice = bluetoothDevice;
                boolean result = GW_Bluetooth.HFP.disconnect(bluetoothDevice);
                Log.i(TAG, "disconnect system hfp result = " + result);
            }
        }
    }

    /**
     * sco开关
     * @param isOn
     */
    public static void bluetoothScoOn(boolean isOn){
        AudioManager am = getAudioManager();
        am.setBluetoothScoOn(isOn);
    }

    /**
     * 是否开启扬声器
     * @param isOn
     */
    public static void openSpeakerOn(boolean isOn){
        AudioManager am = getAudioManager();
        am.setSpeakerphoneOn(isOn);
    }

    /**
     * 设置声音类型 AudioManager
     * @param audioManagerMode  播放模式 默认AudioManager.STREAM_VOICE_CALL 语音电话
     *                   STREAM_MUSIC 手机音乐
     *                   STREAM_ALARM：手机闹铃
     *                   STREAM_RING：电话铃声
     *                   STREAM_SYSTEAM：手机系统
     *                   STREAM_DTMF：音调
     *                   STREAM_NOTIFICATION：系统提示
     *                   FLAG_SHOW_UI,显示进度条
     *                   PLAY_SOUND:播放声音
     *                   STREAM_BLUETOOTH_SCO:蓝牙sco模式（hide）
     */
    public static void setAudioManagerMode(int audioManagerMode){

        MEIDA_PLAYER_STREAM = audioManagerMode;
    }

    /**
     * 设置通话模式
     * @param mode AudioManager.MODE_NORMAL（正常模式，即在没有铃音与电话的情况）
     *             MODE_RINGTONE（铃响模式）
     *             MODE_IN_CALL（接通电话模式）
     *             MODE_IN_COMMUNICATION（通话模式）
     */
    public static void setAudioMode(int mode) {
        AudioManager am = getAudioManager();
        am.setMode(mode);
    }

    public static AudioManager getAudioManager(){
        return (AudioManager) GW_Bluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
    }



}
