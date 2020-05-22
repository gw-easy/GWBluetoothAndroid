package com.example.doublek.gw_bluetooth;

public class GW_AudioDevice {

    private String name;

//    选择优先级高的模式
    public static final GW_AudioDevice SBP = new GW_AudioDevice("select_by_priority");
//    A2DP全名是Advanced Audio Distribution Profile，高质量音频数据传输的协议 单向传输--蓝牙
    public static final GW_AudioDevice A2DP = new GW_AudioDevice("a2dp");
//    SCO主要用于同步话音传送，双链路-双向传输 --蓝牙
    public static final GW_AudioDevice SCO = new GW_AudioDevice("sco");
//    有线耳机
    public static final GW_AudioDevice WIREDHEADSET = new GW_AudioDevice("wiredheadset");
//    扬声器
    public static final GW_AudioDevice SPEAKER = new GW_AudioDevice("speaker");
    private GW_AudioDevice(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
