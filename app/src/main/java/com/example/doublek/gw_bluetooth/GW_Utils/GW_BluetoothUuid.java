package com.example.doublek.gw_bluetooth.GW_Utils;

import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;

import com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothRuntimeException;

import java.util.Arrays;
import java.util.HashSet;

//各蓝牙类型的uuid
public class GW_BluetoothUuid {
    public static final ParcelUuid AudioSink =
            ParcelUuid.fromString("0000110B-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AudioSource =
            ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AdvAudioDist =
            ParcelUuid.fromString("0000110D-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid HSP =
            ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid HSP_AG =
            ParcelUuid.fromString("00001112-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid Handsfree =
            ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid Handsfree_AG =
            ParcelUuid.fromString("0000111F-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AvrcpController =
            ParcelUuid.fromString("0000110E-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid AvrcpTarget =
            ParcelUuid.fromString("0000110C-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid ObexObjectPush =
            ParcelUuid.fromString("00001105-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Hid =
            ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid Hogp =
            ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb");
    public static final ParcelUuid PANU =
            ParcelUuid.fromString("00001115-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid NAP =
            ParcelUuid.fromString("00001116-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid BNEP =
            ParcelUuid.fromString("0000000f-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid PBAP_PCE =
            ParcelUuid.fromString("0000112e-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid PBAP_PSE =
            ParcelUuid.fromString("0000112f-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MAP =
            ParcelUuid.fromString("00001134-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MNS =
            ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid MAS =
            ParcelUuid.fromString("00001132-0000-1000-8000-00805F9B34FB");
    public static final ParcelUuid SAP =
            ParcelUuid.fromString("0000112D-0000-1000-8000-00805F9B34FB");

    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

    /** Length of bytes for 16 bit UUID */
    public static final int UUID_BYTES_16_BIT = 2;
    /** Length of bytes for 32 bit UUID */
    public static final int UUID_BYTES_32_BIT = 4;
    /** Length of bytes for 128 bit UUID */
    public static final int UUID_BYTES_128_BIT = 16;

    public static final ParcelUuid[] RESERVED_UUIDS = {
            AudioSink, AudioSource, AdvAudioDist, HSP, Handsfree, AvrcpController, AvrcpTarget,
            ObexObjectPush, PANU, NAP, MAP, MNS, MAS, SAP};

    /**
     * Returns true if there any common ParcelUuids in uuidA and uuidB.
     *
     * @param uuidA - List of ParcelUuids
     * @param uuidB - List of ParcelUuids
     *
     */
    public static boolean containsAnyUuid(ParcelUuid[] uuidA, ParcelUuid[] uuidB) {
        if (uuidA == null && uuidB == null) return true;

        if (uuidA == null) {
            return uuidB.length == 0 ? true : false;
        }

        if (uuidB == null) {
            return uuidA.length == 0 ? true : false;
        }

        HashSet<ParcelUuid> uuidSet = new HashSet<ParcelUuid> (Arrays.asList(uuidA));
        for (ParcelUuid uuid: uuidB) {
            if (uuidSet.contains(uuid)) return true;
        }
        return false;
    }




}
