package com.example.doublek.gw_bluetooth;

import android.content.Context;
import android.media.AudioManager;
import android.os.RemoteException;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothRuntimeException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothService;

public class GW_AudioService extends GW_BluetoothService {

    private AudioManager mAudioManager;

    private AudioModeModifier mAudioModeModifier = AudioModeModifier.DEFAULT;

    private static final String TAG = GW_Bluetooth.TAG;


    /**
     * 实现音频模式的修改
     */
    public interface AudioModeModifier {
        public static final AudioModeModifier DEFAULT = new AudioModeModifier() {
            @Override
            public void modify(int audioMode) {

                int current = GW_Bluetooth.getAudioMode();

                if(current != audioMode){
                    GW_Bluetooth.setAudioMode(audioMode);
                }

            }
        };
        public void modify(int audioMode);
    }

    @Override
    public boolean init() throws GW_BluetoothException {

        super.init();
        mAudioManager = (AudioManager) GW_Bluetooth.getContext().getSystemService(Context.AUDIO_SERVICE);
        return true;
    }

    @Override
    public boolean destory() {
        super.destory();
        mAudioManager = null;
        return true;
    }

    public void setAudioMode(int mode) {
        mAudioManager.setMode(mode);
    }

    public int getAudioMode(){
        return mAudioManager.getMode();
    }

    public boolean isBluetoothScoOn(){
        return mAudioManager.isBluetoothScoOn();
    }

    public void setSpeakerphoneOn(boolean on){
        mAudioManager.setSpeakerphoneOn(on);
    }

    public boolean isBluetoothA2dpOn(){
        boolean result = mAudioManager.isBluetoothA2dpOn();
        return result;
    }

    public void setBluetoothA2dpOn(boolean on){
        mAudioManager.setBluetoothA2dpOn(on);
    }

//    是否是有线耳机
    public boolean isWiredHeadsetOn(){
        return mAudioManager.isWiredHeadsetOn();
    }

    public boolean isSpeakerphoneOn(){
        return mAudioManager.isSpeakerphoneOn();
    }

    public boolean connectAudio(GW_AudioDevice audioDevice){

        if(audioDevice == null) throw new GW_BluetoothRuntimeException("audioDevice null");

        GW_Bluetooth.Interceptor interceptor = GW_Bluetooth.getInterceptor();

        if(interceptor.beforeConnect(audioDevice)) {
            Log.i(TAG, "[AudioService] connectAudio "+audioDevice + " intercepted");
            return false;
        }

        if(GW_AudioDevice.SBP == audioDevice){

        } else if(GW_AudioDevice.SPEAKER == audioDevice){

            tryConnectSpeaker();

        } else if(GW_AudioDevice.SCO == audioDevice){

            tryConnectSco();

        } else if(GW_AudioDevice.WIREDHEADSET == audioDevice){

            tryConnectWiredHeadset();

        } else if(GW_AudioDevice.A2DP == audioDevice){

            tryConnectSpeaker();

        }

        return true;
    }

//    连接扬声器
    void tryConnectSpeaker() {
        boolean isPhoneCalling = GW_Bluetooth.isPhoneCalling();
        boolean isAudioConnected = GW_Bluetooth.HFP.isAudioConnected();
        boolean isBluetoothScoOn = isBluetoothScoOn();
        boolean isBluetoothA2dpOn = isBluetoothA2dpOn();
        boolean isSpeakerphoneOn = isSpeakerphoneOn();
        if(!isPhoneCalling){
            mAudioModeModifier.modify(AudioManager.MODE_NORMAL);
        }
        Log.i(TAG, "[AudioService] tryConnectSpeaker isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);
        if(isAudioConnected) {
            outofBluetoothScoEnv();
        }
        if(isBluetoothScoOn()){
            setBluetoothScoOn(false);
        }
        if(!isSpeakerphoneOn) {
            setSpeakerphoneOn(true);
        }

    }

    /**
     * 尝试连接SCO
     */
    void tryConnectSco() {
        boolean isPhoneCalling = GW_Bluetooth.isPhoneCalling();
        boolean isAudioConnected = GW_Bluetooth.HFP.isAudioConnected();
        boolean isBluetoothScoOn = isBluetoothScoOn();
        boolean isBluetoothA2dpOn = isBluetoothA2dpOn();
        boolean isSpeakerphoneOn = isSpeakerphoneOn();

        Log.i(TAG, "[AudioService] tryConnectSco isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);
        if(!isAudioConnected) {
            if(!isPhoneCalling) {
                mAudioModeModifier.modify(AudioManager.MODE_IN_COMMUNICATION);
            }
            setSpeakerphoneOn(false);
            setBluetoothA2dpOn(false);
            setBluetoothScoOn(false);
            if(isPhoneCalling) {
                GW_Bluetooth.HFP.connectAudio();
            } else {
                GW_Bluetooth.startSco();
            }
        } else {

            if(isBluetoothA2dpOn){
                setBluetoothA2dpOn(false);
            }
            if(isSpeakerphoneOn){
                setSpeakerphoneOn(false);
            }
            if(!isBluetoothScoOn) {
                setBluetoothScoOn(true);
            }

        }

    }

    /**
     * 尝试连接有线耳机
     */
    void tryConnectWiredHeadset() {

        boolean isPhoneCalling = GW_Bluetooth.isPhoneCalling();

        boolean isAudioConnected = GW_Bluetooth.HFP.isAudioConnected();

        boolean isBluetoothScoOn = isBluetoothScoOn();
        boolean isBluetoothA2dpOn = isBluetoothA2dpOn();
        boolean isSpeakerphoneOn = isSpeakerphoneOn();
        if(!isPhoneCalling){
            mAudioModeModifier.modify(AudioManager.MODE_IN_COMMUNICATION);
        }
        Log.i(TAG, "[AudioService] tryConnectWiredHeadset isAudioConnected = " + isAudioConnected + " , isBluetoothScoOn = " + isBluetoothScoOn + " , isBluetoothA2dpOn = " + isBluetoothA2dpOn + " , isSpeakerphoneOn = " + isSpeakerphoneOn);

        if(isAudioConnected){
            outofBluetoothScoEnv();
            setSpeakerphoneOn(false);
        } else {
            if(isBluetoothA2dpOn()){
                setBluetoothA2dpOn(false);
            }
            if(isBluetoothScoOn()){
                setBluetoothScoOn(false);
            }
            if(isSpeakerphoneOn()){
                setSpeakerphoneOn(false);
            }
        }
    }

    /**
     * 退出SCO
     * @return
     */
    private boolean outofBluetoothScoEnv(){
        setBluetoothA2dpOn(false);
        boolean result = GW_Bluetooth.HFP.disconnectAudio();
        setBluetoothScoOn(false);
        return result;
    }

    public void setBluetoothScoOn(boolean on){
        mAudioManager.setBluetoothScoOn(on);
    }
}
