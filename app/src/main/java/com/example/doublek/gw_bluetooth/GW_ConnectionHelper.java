package com.example.doublek.gw_bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothAdapterStateListener;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothProfileService;
import com.example.doublek.gw_bluetooth.GW_Base.GW_StateInformation;
import com.example.doublek.gw_bluetooth.GW_Base.GW_TaskQueue;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionException;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnectionTimeoutException;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_Connection;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_DefaultRetryPolicy;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_BluetoothHeadsetProfileService;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HFPConnection;
import com.example.doublek.gw_bluetooth.GW_Hfp.GW_HeadsetProfileService;
import com.example.doublek.gw_bluetooth.GW_SPP.GW_SPPConnectionSecurePolicy;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 负责维护蓝牙设备间相关连接(SCO/HFP/SPP) <br>
 * 执行者：{@link ConnectionProcessor}   <br>
 * 提交者：{@link ConnectionHelper#buildBluetoothConnection()} <br>
 */
@SuppressLint("MissingPermission")
public class GW_ConnectionHelper implements GW_BluetoothProfileService.BluetoothProfileConnectionStateChangedListener,
        GW_BluetoothProfileService.BluetoothProfileServiceStateListener ,GW_BluetoothHeadsetProfileService.BluetoothHeadsetAudioStateListener {

    private static final String TAG = GW_ConnectionHelper.class.getSimpleName();

    private static final GW_ConnectionHelper sDefault = new GW_ConnectionHelper();

    static GW_AudioDevice gUserTouchConnectAudioDevice = GW_AudioDevice.SBP;

    final BluetoothAdapterStateListenerImpl bluetoothAdapterStateListenerImpl = new BluetoothAdapterStateListenerImpl();

    static BluetoothDevice sLastConnectBluetoothDevice = null;

    static ExecutorService recoveryExecutor = Executors.newCachedThreadPool(GW_BluetoothUtils.createThreadFactory("bt.runtime.helper-recovery"));

    /**
     * 建立SCO连接异常,连接一直处于{@link BluetoothProfile#STATE_CONNECTING}
     */
    private Runnable scoConnectionExceptionTask = GW_BluetoothUtils.EMPTY_TASK;

    /**
     * 所有需要尝试建立SCO|HFP|SPP连接的任务都需要提交到此队列中
     */
    final GW_TaskQueue connectionTaskQueue;
    /**
     * 负责观察SCO|HFP|SPP连接的正确性
     */
    final GW_TaskQueue watchDogQueue;

    final GW_TaskQueue connectionBuilderQueue;

    /**
     * 与蓝牙设备间的 HFP 连接
     */
    GW_Connection hfpConnection;
    /**
     * 与蓝牙设备间的 SPP 连接
     * 注意：部分设备在建立连接时，如果之前已建立，则需要先断开 {@link Connection#disconnect()}
     */
    GW_Connection sppConnection;
    /**
     * 所有需要尝试建立SCO|HFP|SPP连接的任务都需要延迟2S进入队列
     * 与系统SCO|HFP超时时间相同
     */
    static final long TRY_BUILD_CONNECTION_DELAY_TIME = 2*1000;
    /**
     * 与系统超时时间相同,sco超时时间(sco一直停留在connecting状态)
     */
    static final long SCO_AUDIO_CONNECT_TIMEOUT = 3*1000;

    /**
     * 建立HFP连接超时时间
     */
    static final long HFP_CONNECTION_TIMEOUT = 2*1000;
    /**
     * 建立SPP连接超时时间
     */
    static final long SPP_CONNECTION_TIMEOUT = 3*1000;

    public GW_ConnectionHelper() {
    }

    /**
     * 当应用第一次启动并连接蓝牙服务成功后会触发
     * 当系统蓝牙从关闭->打开后触发
     */
    @Override
    public void onServiceReady(int profile, GW_BluetoothProfileService service) {

        Log.i(TAG, "onServiceReady enter , profile("+GW_BluetoothUtils.getProfileString(profile)+")");

        if(profile == GW_HeadsetProfileService.PROFILE) {
            tryConnectBluetooth();
        }

    }

    /**
     * 系统HFP已建立连接
     */
    @Override
    public void onConnected(int profile, int newState, int preState,BluetoothDevice bluetoothDevice) {
        GW_BluetoothConnection.Protocol protocol = GW_BluetoothConnection.Protocol.getProtocol(profile);
        buildConnection(GW_HeadsetProfileService.PROFILE == profile ? Event.HFP_CONNECTED : Event.A2DP_CONNECTED, protocol,bluetoothDevice);
        connectedNotifier(protocol,bluetoothDevice);
    }

    /**
     * 系统HFP连接断开
     */
    @Override
    public void onDisconnected(int profile, int newState, int preState,
                               BluetoothDevice bluetoothDevice) {
        GW_BluetoothConnection.Protocol protocol = GW_BluetoothConnection.Protocol.getProtocol(profile);
        buildConnection(GW_HeadsetProfileService.PROFILE == profile ? Event.HFP_DISCONNECTED : Event.A2DP_DISCONNECTED, protocol, bluetoothDevice);
        disconnectedNotifier(protocol, bluetoothDevice);
    }

//    绑定连接
    private void buildConnection(Event event,GW_BluetoothConnection.Protocol protocol){
        buildConnection(event, protocol,null,TRY_BUILD_CONNECTION_DELAY_TIME,GW_AudioDevice.SBP);
    }

    private void buildConnection(Event event, GW_BluetoothConnection.Protocol protocol, BluetoothDevice bluetoothDevice){
        buildConnection(event, protocol,bluetoothDevice,TRY_BUILD_CONNECTION_DELAY_TIME,GW_AudioDevice.SBP);
    }

    /**
     * 尝试构建SCO|HFP|SPP等连接,但AudioDeivce为UNKNOW,根据当前终端设备所连接的音频设备的优先级来决定
     *
     * @param event
     * @param protocol
     * @param bluetoothDevice
     * @param delay 延迟多长时间开始连接
     * @param audioDevice
     */
    private void buildConnection(Event event, GW_BluetoothConnection.Protocol protocol, BluetoothDevice bluetoothDevice, long delay, GW_AudioDevice audioDevice){
        connectionBuilderQueue.submitTask(new ConnectionProcessSubmitter(event, bluetoothDevice, audioDevice, protocol, delay));
    }

    public static GW_ConnectionHelper getHelper(){
        return sDefault;
    }

//    尝试连接蓝牙
@SuppressLint("MissingPermission")
void tryConnectBluetooth(){

        LinkedHashMap<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> map = GW_Bluetooth.getAllProfileConnectionState();

        Log.i(TAG, "tryConnect device mapping size = " + map.size());

        for(Iterator<Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState>> iter = map.entrySet().iterator(); iter.hasNext();){

            Map.Entry<BluetoothDevice, GW_BluetoothProfileService.ProfileConnectionState> item = iter.next();

            final BluetoothDevice connectedBluetoothDevice = item.getKey();
            GW_BluetoothProfileService.ProfileConnectionState state = item.getValue();

            Log.i(TAG, "tryConnect device device = " + connectedBluetoothDevice.getName() + " , state = " + state);

            boolean isSupportHFP = GW_Bluetooth.isSupportHFP(connectedBluetoothDevice);

            if(isSupportHFP && (state == GW_BluetoothProfileService.ProfileConnectionState.CONNECTED || state == GW_BluetoothProfileService.ProfileConnectionState.CONNECTED_NO_MEDIA)){
                onConnected(GW_HeadsetProfileService.PROFILE, state.ordinal(), state.ordinal(), connectedBluetoothDevice);
                break;
            }
        }
    }


    private void connectedNotifier(GW_BluetoothConnection.Protocol protocol, BluetoothDevice device){
        GW_Bluetooth.getBluetoothProtocolConnectionStateListener().onConnected(protocol,device);
    }

    private void disconnectedNotifier(GW_BluetoothConnection.Protocol protocol, BluetoothDevice device){
        GW_Bluetooth.getBluetoothProtocolConnectionStateListener().onDisconnected(protocol,device);
    }

    @Override
    public void onAudioConnected(BluetoothDevice bluetoothDevice, GW_BluetoothHeadsetProfileService service) {
        Log.i(TAG, "hfp audio onConnected("+bluetoothDevice.getName()+")");
        connectionTaskQueue.removeTasks(scoConnectionExceptionTask);
    }

    @Override
    public void onAudioDisconnected(BluetoothDevice bluetoothDevice, GW_BluetoothHeadsetProfileService service) {
        Log.i(TAG, "hfp audio onDisconnected("+bluetoothDevice.getName()+")");
        connectionTaskQueue.removeTasks(scoConnectionExceptionTask);

        if(GW_Bluetooth.isPhoneCalling()){
            buildConnection(Event.PHONECALL_SCO_DISCONNECTED, GW_BluetoothConnection.Protocol.HFP);
        } else {
            buildConnection(Event.SCO_DISCONNECTED, GW_BluetoothConnection.Protocol.HFP);
        }
    }

    @Override
    public void onAudioConnecting(BluetoothDevice bluetoothDevice, GW_BluetoothHeadsetProfileService service) {
        Log.i(TAG, "hfp audio onConnecting("+bluetoothDevice.getName()+")");
        connectionTaskQueue.removeTasks(scoConnectionExceptionTask);
        connectionTaskQueue.submitTask(scoConnectionExceptionTask = new ScoConnectionExceptionTask(bluetoothDevice), SCO_AUDIO_CONNECT_TIMEOUT);
    }

    /**
     * 以下定义为触发构建音频连接({@link ConnectionHelper#buildBluetoothConnection()})的事件
     * @author wei.deng
     */
    public enum Event {

        UNKNOW,

        /**
         * 当HFP连接成功之后，通过此事件的定义开始触发构建音频
         */
        HFP_CONNECTED,
        HFP_DISCONNECTED,
        A2DP_CONNECTED,
        A2DP_DISCONNECTED,
        /**
         * 当系统电话状态从INCALL -> IDLE时
         */
        PHONECALL_INCALL_TO_IDLE,
        /**
         * 检测音频状态
         */
        WATCH_DOG,
        /**
         * 用户通过UI主动触发切换音频设备
         */
        USER_INTERFACE,
        USER_INTERFACE_QUIT,
        HEADSET_OR_OTHERDEVICE_RECOVERY,
        /**
         * sco 断开
         */
        SCO_DISCONNECTED,
        PHONECALL_SCO_DISCONNECTED
    }


    public GW_BluetoothAdapterStateListener getBluetoothAdapterStateListener(){
        return bluetoothAdapterStateListenerImpl;
    }

    class BluetoothAdapterStateListenerImpl extends GW_BluetoothAdapterStateListener {

        @Override
        public void onClosed(GW_StateInformation stateInformation) {
            super.onClosed(stateInformation);
            sLastConnectBluetoothDevice = null;
        }

    }

    public void tryRecoveryAudioConnection(){
        Log.i(TAG, "tryRecoveryAudioConnection enter");
        tryRecoveryAudioConnection(Event.WATCH_DOG);
    }

    public void tryRecoveryAudioConnection(Event event){
        watchDogQueue.removeAllTasks();
        watchDogQueue.submitTask(new WatchDogTask(event), TRY_BUILD_CONNECTION_DELAY_TIME);
    }

    class WatchDogTask implements Runnable {

        private Event event;

        public WatchDogTask(Event event){
            this.event = event;
        }

        @Override
        public void run() {
            submitTryRecoveryConnectionTask(event);
        }
    }

    private void submitTryRecoveryConnectionTask(Event event){
        BluetoothDevice device = GW_Bluetooth.HFP.getFristConnectedDevice();
        buildConnection(event, GW_BluetoothConnection.Protocol.HFP, device);
    }

    class BluetoothAdapterStateListenerImpl extends GW_BluetoothAdapterStateListener {

        @Override
        public void onClosed(GW_StateInformation stateInformation) {
            super.onClosed(stateInformation);
            sLastConnectBluetoothDevice = null;
        }
    }

    class ConnectionProcessSubmitter implements Runnable {

        private Event event = Event.UNKNOW;
        private BluetoothDevice bluetoothDevice;
        private GW_AudioDevice audioDevice = GW_AudioDevice.SBP;
        private GW_BluetoothConnection.Protocol protocol = GW_BluetoothConnection.Protocol.HFP;
        private long delay = HFP_CONNECTION_TIMEOUT;

        public ConnectionProcessSubmitter(Event event, BluetoothDevice bluetoothDevice,
                                          GW_AudioDevice audioDevice, GW_BluetoothConnection.Protocol protocol, long delay) {
            this.event = (event == null ? Event.UNKNOW : event);
            this.bluetoothDevice = bluetoothDevice;
            this.audioDevice = (audioDevice == null ? GW_AudioDevice.SBP : audioDevice);
            this.protocol = (protocol == null ? GW_BluetoothConnection.Protocol.HFP : protocol);
            this.delay = delay;
        }

        @Override
        public void run() {

            GW_BluetoothUtils.dumpBluetoothDevice(TAG, bluetoothDevice);
            Log.i(TAG, "buildBluetoothConnection event = "+ event +", delay time = " + delay + " , param userTouch " + audioDevice + " , pre userTouch " + gUserTouchConnectAudioDevice + " , protocol = " + protocol);

            //为什么还需要处理A2dp的profile连接？
            //如果当前设备处于正常模式下即：speaker开启，而当A2dp连接成功后,A2dp开启,那么会走蓝牙设备,所以当A2dp连接成功之后,也需要检查音频连接并关闭A2dp

            if(GW_BluetoothConnection.Protocol.HFP == protocol || GW_BluetoothConnection.Protocol.A2DP == protocol) {

                switch (event) {
                    case A2DP_CONNECTED:
                    case A2DP_DISCONNECTED:
                    case HFP_CONNECTED:
                    case HFP_DISCONNECTED:
                    case HEADSET_OR_OTHERDEVICE_RECOVERY:
                    case PHONECALL_INCALL_TO_IDLE:
                    case SCO_DISCONNECTED:
                    case PHONECALL_SCO_DISCONNECTED:
                    case WATCH_DOG:
                        audioDevice = gUserTouchConnectAudioDevice;
                        break;
                    case USER_INTERFACE:
                        gUserTouchConnectAudioDevice = audioDevice;
                        break;
                    case UNKNOW:
                    case USER_INTERFACE_QUIT:
                        gUserTouchConnectAudioDevice = GW_AudioDevice.SBP;
                        break;
                }

                Log.i(TAG, "submitTask(ConnectionProcessor), event " + event + " , submit audioDevice = " + audioDevice);
                connectionTaskQueue.removeAllTasks();
                connectionTaskQueue.submitTask(new ConnectionProcessor(bluetoothDevice,audioDevice),delay);

            }

        }

    }

    /**
     * 负责建立SCO/HFP/SPP,同时尝试恢复与当前设备环境相符的音频状态<br>
     *
     * <p>为什么不以构造方法传递的蓝牙设备对象为准来构建HFP与SPP连接？</p>
     * 由于ConnectionProcessor接收来自A2dp入口，所以<br>
     * 当A设备的HFP连接建立成功之后延迟提交任务到队列，而此时B设备的A2dp进入，会将HFP连接任务从队列中移除<br>
     * 如果以构造方法的设备为准，那么将会根据B设备来建立HFP与SPP，如果B不支持SPP或HFP,则将无法建立。
     * 但是此时支持HFP与SPP的A设备已经连接.<br>
     *
     * <p>以上情况是多蓝牙设备间多次切换导致</p>
     *
     * 所以任何一个协议连接建立完成之后，都实时去获取当前终端连接情况，保证HFP与SPP连接的正确创建。<br>
     *
     * @author wei.deng
     */
    class ConnectionProcessor implements Runnable {

        private GW_AudioDevice audioDevice = GW_AudioDevice.SBP;

        public ConnectionProcessor(BluetoothDevice bluetoothDevice,GW_AudioDevice audioDevice){
            this.audioDevice = (audioDevice!=null ? audioDevice : GW_AudioDevice.SBP);
        }

        public void run(){
            try {

                GW_Bluetooth.Interceptor interceptor = GW_Bluetooth.getInterceptor();

                if(interceptor.beforeRunConnectTask()){
                    Log.i(TAG, "ConnectTask intercepted");
                    return ;
                }

                Log.i(TAG, "start connect " + audioDevice);

                if(GW_AudioDevice.SBP == audioDevice){

                    //根据终端当前已连接的音频设备优先级来决定走哪个音频设备
                    tryConnect(interceptor);

                } else if(GW_AudioDevice.SPEAKER == audioDevice){

                    tryConnectSpeaker();

                } else if(GW_AudioDevice.SCO == audioDevice){

                    if(GW_Bluetooth.isBluetoothEnable() && GW_Bluetooth.HFP.hasConnectedDevice()){
                        tryConnectBluetooth(interceptor);
                    }

                } else if(GW_AudioDevice.WIREDHEADSET == audioDevice){

                    tryConnectWiredHeadset();

                } else if(GW_AudioDevice.A2DP == audioDevice){

                    tryConnectA2dp();
                }

            } finally {

            }
        }

        public GW_AudioDevice getAudioDevice(){
            return audioDevice;
        }

        private void tryConnect(GW_Bluetooth.Interceptor interceptor){
            if(GW_Bluetooth.isPhoneCalling()){ //系统电话是否连接中

                if(interceptor.systemPhoneCalling()){
                    Log.i(TAG, "SystemEvent intercepted");
                    return ;
                }

                disconnectAllProfiles();

                if(GW_Bluetooth.hasForcePhoneRing() && GW_Bluetooth.hasForcePhoneIncall() && GW_Bluetooth.isBluetoothEnable() && GW_Bluetooth.HFP.hasConnectedDevice()) {
                    tryConnectSco();
                }
            } else if(GW_Bluetooth.isBluetoothEnable() && GW_Bluetooth.HFP.hasConnectedDevice()){ //是否存在可用的蓝牙设备

                tryConnectBluetooth(interceptor);

            } else if(GW_Bluetooth.isWiredHeadsetOn()){ //有线耳机是否插入

                tryConnectWiredHeadset();

            }  else {  //正常设备环境

                tryConnectSpeaker();
            }
        }


        private void tryConnectBluetooth(GW_Bluetooth.Interceptor interceptor){

            BluetoothDevice bluetoothDevice = GW_Bluetooth.HFP.getFristConnectedDevice();

            if(interceptor.beforeConnectBluetoothProfile()){
                Log.i(TAG, "ConnectBluetoothProfile event intercepted");
                sLastConnectBluetoothDevice = bluetoothDevice;
                return ;
            } else {
                if(bluetoothDevice!=null){

                    Log.i(TAG, "current device("+bluetoothDevice.getName()+") , last connect deivce("+(sLastConnectBluetoothDevice!=null?sLastConnectBluetoothDevice.getName():"none")+")");

                    if(sLastConnectBluetoothDevice!=null && !(sLastConnectBluetoothDevice.equals(bluetoothDevice))) {
                        disconnectAllProfiles();
                    }

                    tryConnectHfp(bluetoothDevice);

                    //建立SPP连接可能需要花费一些时间,失败后会进行重连或重启系统HFP连接后建立SPP连接
                    tryConnectSpp(bluetoothDevice);

                    sLastConnectBluetoothDevice = bluetoothDevice;
                }
            }
            tryConnectSco();
        }


        private void tryConnectSco() {
            dumpAudioEnv(GW_AudioDevice.SCO);
            GW_Bluetooth.connectAudioInternal(GW_AudioDevice.SCO);
        }

        private void tryConnectWiredHeadset() {
            dumpAudioEnv(GW_AudioDevice.WIREDHEADSET);
            GW_Bluetooth.connectAudioInternal(GW_AudioDevice.WIREDHEADSET);
            disconnectAllProfiles();
        }

        private void tryConnectSpeaker() {
            dumpAudioEnv(GW_AudioDevice.SPEAKER);
            GW_Bluetooth.connectAudioInternal(GW_AudioDevice.SPEAKER);
            disconnectAllProfiles();
            GW_BluetoothUtils.dumpProfileConnectionMap(TAG,GW_Bluetooth.getAllProfileConnectionState());
        }

        private void tryConnectA2dp() {
            dumpAudioEnv(GW_AudioDevice.A2DP);
            GW_Bluetooth.connectAudioInternal(GW_AudioDevice.A2DP);
        }

        private void dumpAudioEnv(GW_AudioDevice audioDevice){
            boolean isAudioConnected = GW_Bluetooth.HFP.isAudioConnected();
            boolean isBluetoothScoOn = GW_Bluetooth.isBluetoothScoOn();
            boolean isBluetoothA2dpOn = GW_Bluetooth.isBluetoothA2dpOn();
            boolean isSpeakerphoneOn = GW_Bluetooth.isSpeakerphoneOn();
            Log.i(TAG, "tryBuildAudioConnection("+audioDevice.getName()+") isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);
        }

        private boolean isHFPConnected(){
            return (hfpConnection!=null && hfpConnection.isConnected());
        }

        private boolean isSPPConnected(){
            return (sppConnection!=null && sppConnection.isConnected());
        }

        private boolean tryConnectHfp(BluetoothDevice bluetoothDevice) {
            String deviceName = bluetoothDevice != null ? bluetoothDevice.getName() : "none";

            boolean isHFPConnected = isHFPConnected();
            Log.i(TAG, "start build hfp connection("+isHFPConnected+") , deviceName = " + deviceName);
            if(!isHFPConnected) {
                connectHFP(bluetoothDevice);
                return true;
            }
            return false;
        }

        private boolean tryConnectSpp(BluetoothDevice bluetoothDevice) {

            String deviceName = bluetoothDevice != null ? bluetoothDevice.getName() : "none";

            boolean isSupportSPP = GW_Bluetooth.isSupportSPP(bluetoothDevice);
            boolean isSPPConnected = isSPPConnected();
            Log.i(TAG, "start build spp("+isSupportSPP+") connection("+isSPPConnected+"), deviceName = " + deviceName);
            if(isSupportSPP && !isSPPConnected){
                Log.i(TAG, "start build spp connection");
                connectSPP(bluetoothDevice,true);
                return true;
            }
            return false;
        }

        private void connectHFP(final BluetoothDevice bluetoothDevice){
            try{
                Log.i(TAG, "connectHFP enter");
                new GW_BluetoothConnection().Builder()
                        .setConnectionUUID(GW_HeadsetProfileService.UUIDS[0].getUuid())
                        .setConnectedDevice(bluetoothDevice)
                        .setConnectionTimeout(HFP_CONNECTION_TIMEOUT)
                        .setCompanyid(GW_Bluetooth.getConfiguration().getCompanyId())
                        .addConnectionPolicy(new GW_HFPConnection())
                        .setConnectionListener(new GW_BluetoothConnection.BluetoothConnectionListener() {

                            @Override
                            public void onConnected(GW_Connection connection) {
                                Log.i(TAG, "HFP connected, start receive message");
                                hfpConnection = connection;
                                GW_BluetoothUtils.dumpBluetoothConnection(TAG,connection);
                            }

                            @Override
                            public void onDisconnected(GW_Connection connection) {
                                Log.i(TAG, "HFP onDisconnected");
                            }
                        })
                        .build()
                        .connect();
            } catch (GW_BluetoothConnectionException e) {
                e.printStackTrace();
                Log.i(TAG, "connectHFP exception("+(e!=null ? e.getMessage() : "none")+")");
            } catch (GW_BluetoothConnectionTimeoutException e) {
                e.printStackTrace();
                Log.i(TAG, "connectHFP exception("+(e!=null ? e.getMessage() : "none")+")");
            } catch (GW_BluetoothException e) {
                e.printStackTrace();
                Log.i(TAG, "connectHFP exception("+(e!=null ? e.getMessage() : "none")+")");
            }

        }

        private boolean connectSPP(final BluetoothDevice bluetoothDevice,final boolean isSupportRebuildHfpConnection) {
            try {

                Log.i(TAG, "connectSPP enter");

                new GW_BluetoothConnection().Builder()
                        .setConnectionUUID(GW_BluetoothConnection.DEFAULT_UUID)
                        .setConnectedDevice(bluetoothDevice)
                        .setConnectionTimeout(SPP_CONNECTION_TIMEOUT)
                        .setConnectionRetryPolicy(new GW_DefaultRetryPolicy(1))
                        .addConnectionPolicy(new GW_SPPConnectionSecurePolicy())
                        .setConnectionListener(new GW_BluetoothConnection.BluetoothConnectionListener() {

                            @Override
                            public void onConnected(GW_Connection connection) {
                                Log.i(TAG, "SPP connected, start receive message");
                                sppConnection = connection;
                                connectedNotifier(GW_BluetoothConnection.Protocol.SPP,bluetoothDevice);
                            }

                            @Override
                            public void onDisconnected(GW_Connection connection) {
                                Log.i(TAG, "SPP Disconnected");
                                disconnectedNotifier(GW_BluetoothConnection.Protocol.SPP,bluetoothDevice);
                            }
                        })
                        .build()
                        .connect();

                return true;
            } catch (GW_BluetoothConnectionException e) {
                e.printStackTrace();
                Log.i(TAG, "connectSPP exception("+(e!=null ? e.getMessage() : "none")+")");
                if(isSupportRebuildHfpConnection){
                    rebuildHfpConnection(bluetoothDevice);
                }
            } catch (GW_BluetoothConnectionTimeoutException e) {
                e.printStackTrace();
                Log.i(TAG, "connectSPP exception("+(e!=null ? e.getMessage() : "none")+")");
                if(isSupportRebuildHfpConnection){
                    rebuildHfpConnection(bluetoothDevice);
                }
            } catch (GW_BluetoothException e) {
                e.printStackTrace();
                Log.i(TAG, "connectSPP exception("+(e!=null ? e.getMessage() : "none")+")");
            }

            return false;
        }
        /**
         * 尝试断开系统HFP连接进行恢复<br>
         * 什么情况下出现这种情况?<br>
         * 当SPP连接维护的socket出现错误，在建立一次新的连接时没有正确断开上次连接，可能会导致当前连接建立失败,是一种补救方法。<br>
         * 同时尝试恢复SPP需要同步在当前任务队列完成，使后续任务得到正确的状态<br>
         * @param device
         */
        private void rebuildHfpConnection(final BluetoothDevice device){

            if(!GW_Bluetooth.isBluetoothEnable()) return ;

            boolean isConnected = GW_Bluetooth.HFP.isConnected(device);

            Log.i(TAG, "rebuildHfpConnection device isConnected = " + isConnected + " , deivce name = " + device.getName());

            if(isConnected){

                boolean disconnectResult = GW_Bluetooth.HFP.disconnect(device);
                int priority = GW_Bluetooth.HFP.getPriority(device);
                Log.i(TAG, "rebuildHfpConnection disconnect device result = " + disconnectResult + " , priority = " + GW_BluetoothUtils.getDevicePriority(priority));

                int count = 0;
                int maxConnectCount = 3;

                while(true){
                    try {
                        boolean connectResult = GW_Bluetooth.HFP.connect(device);
                        Log.i(TAG, "rebuildHfpConnection while item connectResult = " + connectResult + " , priority = " + GW_BluetoothUtils.getDevicePriority(priority));
                        try {
                            Thread.sleep((1*1000 + (count * 200)));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        isConnected = GW_Bluetooth.HFP.isConnected(device);
                        if(GW_Bluetooth.isBluetoothEnable() && (isConnected || (count >= maxConnectCount))){
                            Log.i(TAG, "rebuildHfpConnection break loop("+count+")");
                            break;
                        }

                        ++count;
                    } catch(Throwable e){
                        break;
                    }

                }

                BluetoothDevice bluetoothDevice = GW_Bluetooth.HFP.getFristConnectedDevice();
                if(bluetoothDevice!=null && GW_Bluetooth.isSupportSPP(bluetoothDevice)) {
                    if(!connectSPP(device,false)){

                    }
                }

            } else {

                BluetoothDevice bluetoothDevice = GW_Bluetooth.HFP.getFristConnectedDevice();
                if(bluetoothDevice!=null && GW_Bluetooth.isSupportSPP(bluetoothDevice)) {
                    connectSPP(bluetoothDevice,true);
                }

            }

        }

        private void disconnectAllProfiles(){
            Log.i(TAG, "disconnectAllProfiles enter");
            disconnectHFP();
            disconnectSPP();
        }

        private void disconnectHFP(){
            if(hfpConnection!=null){
                Log.i(TAG, "disconnect hfp connection, stop receive message");
                hfpConnection.disconnect();
                hfpConnection = null;
            }
        }

        private void disconnectSPP(){
            if(sppConnection!=null){
                Log.i(TAG, "disconnect spp connection, stop receive message");
                sppConnection.disconnect();
                sppConnection = null;
            }
        }

    }

    abstract class AbstractExceptionTask implements Runnable{

        protected BluetoothDevice mDevice;
        protected volatile boolean isStop = false;
        protected Thread recoveryThread;
        static final  int DEFAULT_TASK_TIMEOUT = 10; //seconds

        public AbstractExceptionTask(BluetoothDevice bluetoothDevice) {
            this.mDevice = bluetoothDevice;
        }

        public long timeout(){
            return 0L;
        }

        public abstract Runnable task();

        @Override
        public void run() {

            long timeout = timeout();
            Runnable taskImpl = task();
            if(timeout == 0){
                taskImpl.run();
                return ;
            }

            try {
                Future<?> future = recoveryExecutor.submit(taskImpl);
                future.get(timeout, TimeUnit.SECONDS);
                mDevice = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
                Log.i(TAG, "start recovery Timeout");
                isStop = true;
                if(recoveryThread!=null){
                    recoveryThread.interrupt();
                    recoveryThread = null;
                }
            }

        }

    }

    /**
     * sco 一直处于{@link BluetoothHeadset#STATE_AUDIO_CONNECTING}状态下，则认为发生异常
     * @author wei.deng
     */
    class ScoConnectionExceptionTask extends AbstractExceptionTask {

        public ScoConnectionExceptionTask(BluetoothDevice bluetoothDevice) {
            super(bluetoothDevice);
        }

        @Override
        public long timeout() {
            return 10;
        }

        @Override
        public Runnable task() {
            return new Runnable() {

                @Override
                public void run() {

                    if(mDevice!=null){

                        int audioState = GW_Bluetooth.HFP.getAudioState(mDevice);
                        GW_BluetoothProfileService.ProfileConnectionState connectionState = GW_Bluetooth.HFP.getConnectionState(mDevice);
                        int priority = GW_Bluetooth.getPriority(GW_HeadsetProfileService.PROFILE,mDevice);

                        Log.i(TAG, "AudioExceptionTask run , bt isenable = "+ GW_Bluetooth.isBluetoothEnable() + ", audioState = " + GW_BluetoothUtils.getScoStateStringFromHeadsetProfile(audioState) + " , hfp connection state = " + connectionState + " ,priority = " + GW_BluetoothUtils.getDevicePriority(priority));

                        if(GW_Bluetooth.isBluetoothEnable() && audioState == BluetoothHeadset.STATE_AUDIO_CONNECTING && GW_BluetoothProfileService.ProfileConnectionState.isConnected(connectionState)) {

                            Log.i(TAG, "start recovery#2");

                            recoveryThread = Thread.currentThread();

                            connectionTaskQueue.removeAllTasks();

                            //在当前任务线程(ConnectionThread)阻塞运行(推迟所有任务)，保证当前线程所在线程队列的其他任务可以正确获取蓝牙状态

                            while(!isStop && GW_Bluetooth.isBluetoothEnable()){
                                Log.i(TAG, "start recovery#2 disableBluetooth");
                                GW_Bluetooth.disableBluetooth();

                                try {
                                    Thread.sleep(1500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }

                            while(!isStop && !GW_Bluetooth.isBluetoothEnable()){
                                Log.i(TAG, "start recovery#2 enableBluetooth");
                                GW_Bluetooth.enableBluetooth();

                                try {
                                    Thread.sleep(1500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }


                            if(priority == BluetoothHeadset.PRIORITY_AUTO_CONNECT) {

                                GW_BluetoothProfileService.ProfileConnectionState state = GW_Bluetooth.HFP.getConnectionState(mDevice);

                                Log.i(TAG, "start recovery#2 check device("+mDevice.getName()+") new state("+state+")");

                                while(!isStop && !GW_BluetoothProfileService.ProfileConnectionState.isConnected(state)) {
                                    state = GW_Bluetooth.HFP.getConnectionState(mDevice);

                                    Log.i(TAG, "start recovery#2 check device("+mDevice.getName()+") new state = " + state);

                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                Log.i(TAG, "start recovery check device("+mDevice.getName()+") final state = " + state);
                            }


                            if(isStop && !GW_Bluetooth.isBluetoothEnable()) {
                                GW_Bluetooth.enableBluetooth();
                            }

                            Log.i(TAG, "start recovery#2 completed");

                        }

                    }

                }
            };
        }

    }
}
