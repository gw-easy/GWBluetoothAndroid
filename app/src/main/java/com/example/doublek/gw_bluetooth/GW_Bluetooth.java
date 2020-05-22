package com.example.doublek.gw_bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.example.doublek.gw_bluetooth.GW_A2dp.GW_A2dpProfileService;
import com.example.doublek.gw_bluetooth.GW_A2dp.GW_BluetoothA2dpProfileService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothAdapterService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothAdapterStateListener;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothRuntimeException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothThread;
import com.example.doublek.gw_bluetooth.GW_Base.GW_Executor;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionException;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionTimeoutException;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothDeviceConnectionService;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_Connection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_DefaultRetryPolicy;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_BluetoothHeadsetProfileService;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HeadsetProfileService;
import com.example.doublek.gw_bluetooth.GW_Message.GW_BluetoothMessageDispatcher;
import com.example.doublek.gw_bluetooth.GW_Message.GW_BluetoothMessageReceiver;
import com.example.doublek.gw_bluetooth.GW_SCO.GW_BluetoothSCOService;
import com.example.doublek.gw_bluetooth.GW_SPP.GW_SPPBluetoothMessageParser;
import com.example.doublek.gw_bluetooth.GW_SPP.GW_SPPConnectionSecurePolicy;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUuid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SuppressLint("MissingPermission")
public class GW_Bluetooth extends GW_BluetoothService {

    static final String TAG = GW_Bluetooth.class.getSimpleName();

    /**
	 * 系统电话来电时强制尝试使用蓝牙
	 */
    public static final int FORCE_TYPE_PHONE_RING = 0x001;
    /**
     * 系统电话通话中强制尝试使用蓝牙
     */
    public static final int FORCE_TYPE_PHONE_INCALL = 0x002;
    /**
     * 系统电话通话后挂断强制尝试使用蓝牙
     */
    public static final int FORCE_TYPE_PHONE_INCALL_TO_IDLE = 0x004;

    private static GW_SPPBluetoothMessageParser<?> sSppBluetoothMessageParser = GW_SPPBluetoothMessageParser.DEFAULT;

    //    蓝牙管理器服务
    private GW_BluetoothAdapterService mBluetoothAdapterService;
//    蓝牙a2dp服务接口
    private GW_BluetoothA2dpProfileService mA2dpProfileService;
//    蓝牙服务map
    private final HashMap<Integer,GW_BluetoothProfileService> mProfileServiceMapping = new HashMap<Integer,GW_BluetoothProfileService>();

    private Interceptor mInterceptor = Interceptor.EMPTY;

    //    蓝牙headset服务接口
    private GW_BluetoothHeadsetProfileService mHeadsetProfileService;

    private Context mContext;

//    配置
    private GW_Configuration mConfiguration = GW_Configuration.DEFAULT;
    private GW_Executor mBluetoothExecutor;
    private GW_BluetoothSCOService mBluetoothScoService;
    private GW_BluetoothDeviceConnectionService mBluetoothConnectService;
    private GW_AudioService mAudioService;
    private GW_TelephonyService mTelephonyService;


    /**
     * 监听HFP|SPP连接的状态
     */
    private BluetoothProtocolConnectionStateListener mProtocolConnectionStateListener = BluetoothProtocolConnectionStateListener.EMTPY;
    //    蓝牙单例
    static class OkBluetoothInstanceHolder {
        static GW_Bluetooth sInstance = new GW_Bluetooth();
    }

    static GW_Bluetooth getInstance(){
        return OkBluetoothInstanceHolder.sInstance;
    }

    public static Context getContext(){
        return getInstance().mContext;
    }

    public static void init(Context context){
        init(context,GW_Configuration.DEFAULT);
    }

//    是否初始化
    public static boolean isReady(){
        return getInstance().mContext != null;
    }
    /**
     *
     * @param context
     * @param config 配置内容
     */
    public static void init(Context context,GW_Configuration config){
        if(context == null){
            throw new IllegalArgumentException("init error , arg context is null");
        }

        if(Looper.myLooper() != Looper.getMainLooper()){
            throw new IllegalArgumentException("init must be in main thread");
        }

        getInstance().mContext = context.getApplicationContext();
        getInstance().mConfiguration = config == null ? GW_Configuration.DEFAULT : config;

        Log.i(TAG, getConfiguration().toString());

        getInstance().initServices();
        bindConnectionHelper();
    }

    public void initServices(){
        mBluetoothExecutor = GW_BluetoothThread.get();

        mBluetoothScoService = new GW_BluetoothSCOService();

        mBluetoothConnectService = new GW_BluetoothDeviceConnectionService();
        mBluetoothAdapterService = new GW_BluetoothAdapterService();

        mHeadsetProfileService = new GW_HeadsetProfileService();
        mA2dpProfileService = new GW_A2dpProfileService();
        mAudioService = new GW_AudioService();
        mTelephonyService = new GW_TelephonyService();

        mProfileServiceMapping.put(GW_HeadsetProfileService.PROFILE, mHeadsetProfileService);
        mProfileServiceMapping.put(GW_A2dpProfileService.PROFILE, mA2dpProfileService);

        try {
            mBluetoothAdapterService.init();
            mBluetoothConnectService.init();
            mBluetoothScoService.init();
            mA2dpProfileService.init();
            mHeadsetProfileService.init();
            mAudioService.init();
            mTelephonyService.init();
        } catch (GW_BluetoothException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean destory() {
        check();

        mBluetoothAdapterService.destory();
        mBluetoothConnectService.destory();
        mBluetoothScoService.destory();
        mHeadsetProfileService.destory();
        mA2dpProfileService.destory();
        mAudioService.destory();
        mTelephonyService.destory();
        return true;
    }

//    检查是否有context
    static void check(){
        if(getInstance().mContext == null){
            throw new GW_BluetoothRuntimeException("please invoke BMS#init");
        }
    }

    static boolean bindConnectionHelper(){
        GW_ConnectionHelper connectionHelper = GW_ConnectionHelper.getHelper();
        registerBluetoothProfileServiceStateListener(GW_HeadsetProfileService.PROFILE,connectionHelper);
        registerProfileConnectionStateChangedListener(connectionHelper,new int[]{GW_HeadsetProfileService.PROFILE,GW_A2dpProfileService.PROFILE});
        registerBluetoothAdapterStateChangedListener(connectionHelper.getBluetoothAdapterStateListener());
        HFP.registerAudioStateChangedListener(connectionHelper);
        return true;
    }

//    注册蓝牙配置服务状态
    public static void registerBluetoothProfileServiceStateListener(int profile,GW_BluetoothProfileService.BluetoothProfileServiceStateListener serviceStateListener) {
        getBluetoothProfileService(profile).registerBluetoothProfileServiceListener(serviceStateListener);
    }

//    注册蓝牙配置服务连接状态
    public static void registerProfileConnectionStateChangedListener(GW_BluetoothProfileService.BluetoothProfileConnectionStateChangedListener lis, int... profiles) {

        for(int profile : profiles) {
            getBluetoothProfileService(profile).registerProfileConnectionStateChangedListener(lis);
        }

    }

//    注册蓝牙设备状态
    public static void registerBluetoothAdapterStateChangedListener(GW_BluetoothAdapterStateListener adapterStateListener) {
        getInstance().mBluetoothAdapterService.setBluetoothAdapterStateListener(adapterStateListener);
    }

    public static <DataType> void registerBluetoothMessageReceiver(GW_BluetoothMessageReceiver<DataType> receiver, GW_BluetoothConnection.Protocol...protocols) {
        GW_BluetoothMessageDispatcher<DataType> dispatcherImpl = (GW_BluetoothMessageDispatcher<DataType>) GW_BluetoothMessageDispatcher.getDispatcher();

        for(GW_BluetoothConnection.Protocol protocol : protocols){
            dispatcherImpl.registerBluetoothMessageReceiver(protocol,receiver);
        }

    }

    public static <DataType> void unregisterMessageReceiver(GW_BluetoothMessageReceiver<DataType> receiver,GW_BluetoothConnection.Protocol...protocols) {
        GW_BluetoothMessageDispatcher<DataType> dispatcherImpl = (GW_BluetoothMessageDispatcher<DataType>) GW_BluetoothMessageDispatcher.getDispatcher();
        for(GW_BluetoothConnection.Protocol protocol : protocols){
            dispatcherImpl.unregisterBluetoothMessageReceiver(protocol,receiver);
        }
    }

    /**
     * 打开蓝牙
     */
    public static void enableBluetooth(){
        getInstance().mBluetoothAdapterService.enable();
    }
    /**
     * 系统蓝牙开关是否打开
     * @return true:打开
     */
    public static boolean isBluetoothEnable(){
        return getInstance().mBluetoothAdapterService.isEnable();
    }

    /**
     * 断开蓝牙
     */
    public static void disableBluetooth(){
        getInstance().mBluetoothAdapterService.disable();
    }

    /**
     * 是否在通话
     * @return
     */
    public static boolean isPhoneCalling(){
        return getInstance().mTelephonyService.isPhoneCalling();
    }

    public static boolean hasForcePhoneRing(){
        return (getConfiguration().getForceTypes() & FORCE_TYPE_PHONE_RING) != 0;
    }

    public static boolean hasForcePhoneIncall(){
        return (getConfiguration().getForceTypes() & FORCE_TYPE_PHONE_INCALL) != 0;
    }

    public static boolean hasForcePhoneIdle(){
        return (getConfiguration().getForceTypes() & FORCE_TYPE_PHONE_INCALL_TO_IDLE) != 0;
    }

    static GW_AudioService getAudioService() {
        return getInstance().mAudioService;
    }

    public static boolean isWiredHeadsetOn(){
        return getAudioService().isWiredHeadsetOn();
    }

    public static boolean isBluetoothScoOn(){
        return getAudioService().isBluetoothScoOn();
    }

    public static boolean isSpeakerphoneOn(){
        return getAudioService().isSpeakerphoneOn();
    }

    public static boolean isBluetoothA2dpOn(){
        return getAudioService().isBluetoothA2dpOn();
    }

    static boolean connectAudioInternal(GW_AudioDevice audioDevice){
        return getAudioService().connectAudio(audioDevice);
    }

    public static void setAudioMode(int mode) {
        getAudioService().setAudioMode(mode);
    }

    public static int getAudioMode(){
        return getAudioService().getAudioMode();
    }

    public static boolean isSupportSPP(BluetoothDevice bluetoothDevice) {

        if(bluetoothDevice == null) return false;

        ParcelUuid[] parcelUuid = bluetoothDevice.getUuids();

        return GW_BluetoothUuid.containsAnyUuid(parcelUuid, new ParcelUuid[]{new ParcelUuid(GW_BluetoothConnection.DEFAULT_UUID)});
    }

    //媒体音频
    public static boolean isSupportA2DP(BluetoothDevice bluetoothDevice){

        if(bluetoothDevice == null) return false;

        ParcelUuid[] parcelUuid = bluetoothDevice.getUuids();

        return GW_BluetoothUuid.containsAnyUuid(parcelUuid, GW_A2dpProfileService.SINK_UUIDS);
    }

    //手机音频
    public static boolean isSupportHFP(BluetoothDevice bluetoothDevice){

        if(bluetoothDevice == null) return false;

         ParcelUuid[] parcelUuid = bluetoothDevice.getUuids();

        return GW_BluetoothUuid.containsAnyUuid(parcelUuid, GW_HeadsetProfileService.UUIDS);
    }

    //    是否支持debug
    public static boolean isDebugable(){
        return getInstance().mConfiguration.isDebug();
    }

    public static <DataType> GW_SPPBluetoothMessageParser<DataType> getSppMessageParser(){
        return ((GW_SPPBluetoothMessageParser<DataType>)GW_Bluetooth.sSppBluetoothMessageParser);
    }

    public static void startSco(){
        check();
        getInstance().mBluetoothScoService.startSco();
    }

    public static void stopSco(){
        check();
        getInstance().mBluetoothScoService.stopSco();
    }

//    配置信息
    public static GW_Configuration getConfiguration(){
        return getInstance().mConfiguration;
    }
//    获取蓝牙配置服务
    public static boolean getProfileService(final int profileParam,final BluetoothProfile.ServiceListener serviceListener){
        return getInstance().mBluetoothAdapterService.getProfileService(profileParam, serviceListener);
    }

//    关闭蓝牙接口
    public static void closeBluetoothProfile(int profile,BluetoothProfile bluetoothProfile){
        getInstance().mBluetoothAdapterService.closeBluetoothProfile(profile,bluetoothProfile);
    }

    public enum ConnectionMode {
        /**
         * 只要连接上蓝牙设备,将仅走蓝牙设备
         */
        BLUETOOTH_ONLY,
        /**
         * 在连接上蓝牙设备之后,可以走三种设备(蓝牙|扬声器|听筒)
         */
        BLUETOOTH_WIREDHEADSET_SPEAKER

    }



//    尝试重新连接
    static void tryRecoveryAudioConnection(GW_ConnectionHelper.Event event){
        GW_ConnectionHelper.getHelper().tryRecoveryAudioConnection(event);
    }

    public static BluetoothProtocolConnectionStateListener getBluetoothProtocolConnectionStateListener(){
        return getInstance().mProtocolConnectionStateListener;
    }

//    获取连接状态
    public static int getConnectionState() {
        return getInstance().mBluetoothAdapterService.getConnectionState();
    }

//    获取连接状态
    public static GW_BluetoothProfileService.ProfileConnectionState getConnectionState(int profile, final BluetoothDevice device) {

        GW_BluetoothUtils.ifNullThrowException(device);

        return getBluetoothProfileService(profile).getConnectionState(device);
    }

    static GW_BluetoothProfileService getBluetoothProfileService(int profile) {
        GW_BluetoothProfileService service = getInstance().mProfileServiceMapping.get(profile);
        if(service == null)
            throw new GW_BluetoothRuntimeException("not support profile("+GW_BluetoothUtils.getProfileString(profile)+")");
        return service;
    }



    /**
     * 当HFP|SPP连接断开或者已连接时通知客户端,通过{@link Protocol}区分类型
     * @author wei.deng
     *
     */
    public static abstract class BluetoothProtocolConnectionStateListener {

        public static final BluetoothProtocolConnectionStateListener EMTPY = new BluetoothProtocolConnectionStateListener() {
        };

        public void onDisconnected(GW_BluetoothConnection.Protocol protocol, BluetoothDevice device){}
        public void onConnected(GW_BluetoothConnection.Protocol protocol, BluetoothDevice device){}
    }

    public static class A2DP {
        /**
         * 强制将系统A2DP连接断开，基本等同于在系统设置中取消媒体音频复选框的效果
         */
        public static boolean disconnect(final BluetoothDevice device){
            return getBluetoothProfileService(GW_A2dpProfileService.PROFILE).disconnect(device);
        }

        public static boolean connect(final BluetoothDevice device){
            return getBluetoothProfileService(GW_A2dpProfileService.PROFILE).connect(device);
        }

        public static boolean isA2dpPlaying(BluetoothDevice device) {
            return getInstance().mA2dpProfileService.isA2dpPlaying(device);
        }
    }

    public static class HFP {

//        获取连接状态
        // HSP ------------------------------------------------------------------
        public static GW_BluetoothProfileService.ProfileConnectionState getConnectionState(final BluetoothDevice device) {

            GW_BluetoothUtils.ifNullThrowException(device);

            return GW_Bluetooth.getConnectionState(GW_HeadsetProfileService.PROFILE,device);
        }

//        是否连接
        public static boolean isConnected(final BluetoothDevice device) {

            GW_BluetoothUtils.ifNullThrowException(device);

            return isConnected(getConnectionState(device));
        }

        public static int getPriority(final BluetoothDevice device) {

            GW_BluetoothUtils.ifNullThrowException(device);

            return GW_Bluetooth.getPriority(GW_HeadsetProfileService.PROFILE, device);
        }

        /**
         * 是否是sco连接
         */
        public static boolean isAudioConnected(final BluetoothDevice device) {

            GW_BluetoothUtils.ifNullThrowException(device);

            return getInstance().mHeadsetProfileService.isAudioConnected(device);
        }

        public static boolean isAudioConnected(){
            List<BluetoothDevice> connectedDevices = getConnectedBluetoothDeviceList();

            for(BluetoothDevice device : connectedDevices){
                if(isAudioConnected(device)){
                    return true;
                }
            }

            return false;
        }

        /**
         * 强制系统HFP进行连接，基本等同于在系统设置中勾选手机音频复选框的效果
         * 在调用连接之前，建议先{@link GW_Bluetooth disconnect(final BluetoothDevice device)},然后在执行。
         */
        public static boolean connect(final BluetoothDevice device){
            return GW_Bluetooth.connect(GW_HeadsetProfileService.PROFILE,device);
        }
        /**
         * 强制将系统HFP连接断开，基本等同于在系统设置中取消手机音频复选框的效果
         */
        public static boolean disconnect(final BluetoothDevice device){
            return GW_Bluetooth.disconnect(GW_HeadsetProfileService.PROFILE,device);
        }


        public static boolean connectAudio(){
            return getInstance().mHeadsetProfileService.connectAudio();
        }

        public static boolean disconnectAudio(){
            return getInstance().mHeadsetProfileService.disconnectAudio();
        }

//        获取连接蓝牙列表
        public static List<BluetoothDevice> getConnectedBluetoothDeviceList(){
            return GW_Bluetooth.getConnectedBluetoothDeviceList(GW_HeadsetProfileService.PROFILE);
        }

//        获取sco连接设备
        public static BluetoothDevice getAudioConnectedDevice(){
            List<BluetoothDevice> list = getConnectedBluetoothDeviceList();
            for(BluetoothDevice device : list){
                if(isAudioConnected(device)){
                    return device;
                }
            }
            return null;
        }

        public static List<BluetoothDevice> getConnectedBluetoothDeviceList(final String deviceName){

            GW_BluetoothUtils.ifNullThrowException(deviceName);

            return GW_Bluetooth.getConnectedBluetoothDeviceList(GW_HeadsetProfileService.PROFILE,deviceName);
        }

//        获取sco连接状态
        public static int getAudioState(final BluetoothDevice device){
            GW_BluetoothUtils.ifNullThrowException(device);
            return getInstance().mHeadsetProfileService.getAudioState(device);
        }

//        sco开启状态
        public static boolean isAudioOn(){
            return getInstance().mHeadsetProfileService.isAudioOn();
        }

//        判断是否连接
        public static boolean isConnected(GW_BluetoothProfileService.ProfileConnectionState state){
            switch (state) {
                case CONNECTED:
                case CONNECTED_NO_MEDIA:
                    return true;
                case DISCONNECTED:
                case CONNECTED_NO_PHONE:
                case CONNECTED_NO_PHONE_AND_MEIDA:
                    return false;
            }

            return false;
        }

//        注册sco连接状态监听
        public static void registerAudioStateChangedListener(GW_BluetoothHeadsetProfileService.BluetoothHeadsetAudioStateListener lis) {
            getInstance().mHeadsetProfileService.registerAudioStateChangedListener(lis);
        }
//        取消sco监听
        public void unregisterAudioStateChangedListener(GW_BluetoothHeadsetProfileService.BluetoothHeadsetAudioStateListener lis){
            getInstance().mHeadsetProfileService.unregisterAudioStateChangedListener(lis);
        }

//        判断是否连接
        public static boolean hasConnectedDevice(){

            LinkedHashMap<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> map = GW_Bluetooth.getAllProfileConnectionState();

            for(Iterator<Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState>> iter = map.entrySet().iterator(); iter.hasNext();){

                Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> item = iter.next();

                final BluetoothDevice connectedBluetoothDevice = item.getKey();
                GW_BluetoothProfileService.ProfileConnectionState state = item.getValue();

                boolean isSupportHFP = GW_Bluetooth.isSupportHFP(connectedBluetoothDevice);

                if(isSupportHFP && (state == GW_BluetoothProfileService.ProfileConnectionState.CONNECTED || state == GW_BluetoothProfileService.ProfileConnectionState.CONNECTED_NO_MEDIA)){
                    return true;
                }
            }
            return false;
        }

//        获取第一个连接的设备
        public static BluetoothDevice getFristConnectedDevice(){

            LinkedHashMap<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> map = GW_Bluetooth.getAllProfileConnectionState();

            for(Iterator<Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState>> iter = map.entrySet().iterator(); iter.hasNext();){

                Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> item = iter.next();

                final BluetoothDevice connectedBluetoothDevice = item.getKey();
                GW_BluetoothProfileService.ProfileConnectionState state = item.getValue();

                boolean isSupportHFP = GW_Bluetooth.isSupportHFP(connectedBluetoothDevice);

                if(isSupportHFP && (state == GW_BluetoothProfileService.ProfileConnectionState.CONNECTED || state == GW_BluetoothProfileService.ProfileConnectionState.CONNECTED_NO_MEDIA)){
                    return connectedBluetoothDevice;
                }
            }
            return null;
        }
    }

//    获取所有设备的链接状态
    @SuppressLint("MissingPermission")
    public static LinkedHashMap<BluetoothDevice,GW_BluetoothProfileService.ProfileConnectionState> getAllProfileConnectionState(){

        final int[] profiles = new int[]{GW_HeadsetProfileService.PROFILE,GW_A2dpProfileService.PROFILE};

        List<BluetoothDevice> bluetoothDevices = getConnectedBluetoothDeviceList(profiles);

        //获取支持headset与a2dp的所有已连接的设备
        //headset的设备可能只能获取一个，因为当前只能有一个处于正在的已连接状态，其余都是CONNECTED_NO_PHONE状态

        try {
            LinkedHashMap<BluetoothDevice,GW_BluetoothProfileService.ProfileConnectionState> result = new LinkedHashMap<BluetoothDevice,GW_BluetoothProfileService.ProfileConnectionState>();


            for (int j = 0; j < bluetoothDevices.size(); j++) {

                boolean profileConnected = false;
                boolean a2dpNotConnected = false;
                boolean headsetNotConnected = false;

                BluetoothDevice bluetoothDevice = bluetoothDevices.get(j);

                boolean isSupportHFP = GW_Bluetooth.isSupportHFP(bluetoothDevice);
                boolean isSupportA2DP = GW_Bluetooth.isSupportA2DP(bluetoothDevice);

                for (int i = 0; i < profiles.length; i++) {

                    int profileInt = profiles[i];
                    GW_BluetoothProfileService profileService = getBluetoothProfileService(profileInt);

                    Log.i(TAG, "bluetoothProfile = "+ profileService);

                    boolean isA2dpProfile = false;
                    boolean isHfpProfile = false;

                    if (profileInt == GW_A2dpProfileService.PROFILE) {
                        isA2dpProfile = true;
                    } else if (profileInt == GW_HeadsetProfileService.PROFILE) {
                        isHfpProfile = true;
                    }

//                    判断是否支持a2dp或者hfp
                    if((isA2dpProfile && isSupportA2DP) || (isSupportHFP && isHfpProfile)) {
                        GW_BluetoothProfileService.ProfileConnectionState connectionStatus = profileService.getConnectionState(bluetoothDevice);

                        Log.i(TAG,"connectionStatus = "+ connectionStatus + " , deviceName = "+ bluetoothDevice.getName());

                        switch (connectionStatus) {
                            case CONNECTING:
                            case CONNECTED:
                                profileConnected = true;
                                break;

                            case DISCONNECTED:
                                if (profileInt == GW_A2dpProfileService.PROFILE) {
                                    a2dpNotConnected = true;
                                } else if (profileInt == GW_HeadsetProfileService.PROFILE) {
                                    headsetNotConnected = true;
                                }
                                break;
                        }
                    }

                    if (profileService instanceof GW_HeadsetProfileService) {
                        GW_HeadsetProfileService bluetoothHeadset = (GW_HeadsetProfileService) profileService;

                        int audioState = bluetoothHeadset.getAudioState(bluetoothDevice);
                        String scoStateString = GW_BluetoothUtils.getScoStateStringFromHeadsetProfile(audioState);

                        boolean isAudioConnected = bluetoothHeadset.isAudioConnected(bluetoothDevice);

                        //android 4.1 不存在这个方法，需要适配
                        boolean isAudioOn = bluetoothHeadset.isAudioOn();

                        Log.i(TAG, "isAudioConnected = " + isAudioConnected + " , scoStateString = " + scoStateString + " , isAudioOn = " + isAudioOn);
                    }

                }
                GW_BluetoothProfileService.ProfileConnectionState currentState = GW_BluetoothProfileService.ProfileConnectionState.DISCONNECTED;
                if (profileConnected) {
                    if (a2dpNotConnected && headsetNotConnected) {
                        Log.i(TAG, bluetoothDevice.getName() + " Connected (no phone or media)");
                        currentState = GW_BluetoothProfileService.ProfileConnectionState.CONNECTED_NO_PHONE_AND_MEIDA;
                    } else if (a2dpNotConnected) {
                        Log.i(TAG, bluetoothDevice.getName() + " Connected (no media)");
                        currentState = GW_BluetoothProfileService.ProfileConnectionState.CONNECTED_NO_MEDIA;
                    } else if (headsetNotConnected) {
                        Log.i(TAG, bluetoothDevice.getName() + " Connected (no phone)");
                        currentState = GW_BluetoothProfileService.ProfileConnectionState.CONNECTED_NO_PHONE;
                    } else {
                        Log.i(TAG, bluetoothDevice.getName() + " Connected");
                        currentState = GW_BluetoothProfileService.ProfileConnectionState.CONNECTED;
                    }
                }

                result.put(bluetoothDevice, currentState);

            }
            return result;
        } finally {
        }

    }

    public static int getPriority(int profile,final BluetoothDevice device) {

        GW_BluetoothUtils.ifNullThrowException(device);

        return getBluetoothProfileService(profile).getPriority(device);
    }


    public static boolean setPriority(int profile,final int priority,final BluetoothDevice device){

        GW_BluetoothUtils.ifNullThrowException(device);

        return getBluetoothProfileService(profile).setPriority(priority, device);
    }

//    连接
    public static boolean connect(int profile,final BluetoothDevice device){
        return getBluetoothProfileService(profile).connect(device);
    }
//  断开连接
    public static boolean disconnect(int profile,final BluetoothDevice device){
        return getBluetoothProfileService(profile).disconnect(device);
    }
//    获取连接设备列表
    public static List<BluetoothDevice> getConnectedBluetoothDeviceList(int profile){
        return getBluetoothProfileService(profile).getConnectedBluetoothDeviceList();
    }
//    获取指定名称的设备列表
    public static List<BluetoothDevice> getConnectedBluetoothDeviceList(int profile,final String deviceName){
        GW_BluetoothUtils.ifNullThrowException(deviceName);
        return getBluetoothProfileService(profile).getConnectedBluetoothDeviceList(deviceName);
    }
    public static List<BluetoothDevice> getConnectedBluetoothDeviceList(int... profiles){

        Set<BluetoothDevice> result = new HashSet<BluetoothDevice>();

        for(int profile : profiles){
            result.addAll(getConnectedBluetoothDeviceList(profile));
        }

        return new ArrayList<>(result);
    }


    public static void setInterceptor(Interceptor interceptorImpl){
        getInstance().mInterceptor = (interceptorImpl!=null ? interceptorImpl : Interceptor.EMPTY);
    }

    public static Interceptor getInterceptor(){
        return getInstance().mInterceptor;
    }

    /**
     * 拦截器
     * @author wei.deng
     */
    public static abstract class Interceptor {

        public static final Interceptor EMPTY = new Interceptor() {
        };

        public boolean beforeRunConnectTask(){
            return false;
        }

        /**
         * 系统电话中
         * @return
         */
        public boolean systemPhoneCalling(){
            return false;
        }

        public boolean beforeConnectBluetoothProfile(){
            return false;
        }

        /**
         * 连接音频设备之前
         */
        public boolean beforeConnect(GW_AudioDevice audioDevice){
            return false;
        }


    }




}
