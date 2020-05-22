package com.example.doublek.gw_bluetooth;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothException;
import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothService;

public class GW_TelephonyService extends GW_BluetoothService {
    private static final String TAG = GW_Bluetooth.TAG;

    private boolean isListenPhoneState = true;

    private TelephonyManager mTelephonyManager;
    /**
     * 负责监听系统电话状态，当系统电话挂断，则尝试提交恢复音频等连接的请求
     */
    private volatile PhoneStateListenerImpl phoneStateListenerImpl;
    @Override
    public boolean init() throws GW_BluetoothException {
        super.init();
        mTelephonyManager = (TelephonyManager) GW_Bluetooth.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        listenPhoneState();

        return true;
    }

    @Override
    public boolean destory() {
        super.destory();
        unlistenPhoneState();
        return true;
    }

    /**
     * 是否在通话
     * @return
     */
    public boolean isPhoneCalling() {
        int callState = mTelephonyManager.getCallState();
        if(callState == TelephonyManager.CALL_STATE_OFFHOOK || callState == TelephonyManager.CALL_STATE_RINGING){
            return true;
        }
        return false;
    }

    private void listenPhoneState() {
        if(isListenPhoneState && phoneStateListenerImpl == null){
            phoneStateListenerImpl = new PhoneStateListenerImpl();
            mTelephonyManager.listen(phoneStateListenerImpl,PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void unlistenPhoneState() {
        if(isListenPhoneState && phoneStateListenerImpl != null){
            mTelephonyManager.listen(phoneStateListenerImpl,PhoneStateListener.LISTEN_NONE);
            phoneStateListenerImpl = null;
        }
    }

    class PhoneStateListenerImpl extends PhoneStateListener {

        volatile boolean called = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i(TAG, "[TelephonyService] call idle");
//                    空闲状态
                    try {
                        if(called && GW_Bluetooth.hasForcePhoneIdle()){
                            GW_Bluetooth.tryRecoveryAudioConnection(GW_ConnectionHelper.Event.PHONECALL_INCALL_TO_IDLE);
                        }
                    } finally {
                        called = false;
                    }

                    break;
//                    响铃
                case TelephonyManager.CALL_STATE_RINGING:
//                    接电话或者主动打电话
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(TAG, "[TelephonyService] system phone calling");
                    called = true;
                    break;
            }
        }
    }
}
