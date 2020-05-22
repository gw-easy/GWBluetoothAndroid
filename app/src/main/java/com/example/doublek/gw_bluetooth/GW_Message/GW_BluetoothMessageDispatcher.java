package com.example.doublek.gw_bluetooth.GW_Message;

import com.example.doublek.gw_bluetooth.GW_Bluetooth;
import com.example.doublek.gw_bluetooth.GW_Connection.GW_BluetoothConnection;
import com.example.doublek.gw_bluetooth.GW_Utils.GW_BluetoothUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GW_BluetoothMessageDispatcher<DataType> implements GW_BluetoothMessageHandler{
    static GW_BluetoothMessageHandler<?> instance;

    final ConcurrentHashMap<GW_BluetoothConnection.Protocol,List<GW_BluetoothMessageReceiver<DataType>>> receivers = new ConcurrentHashMap<GW_BluetoothConnection.Protocol,List<GW_BluetoothMessageReceiver<DataType>>>();

    static ExecutorService dispatcherThreadPool = Executors.newSingleThreadExecutor(GW_BluetoothUtils.createThreadFactory("bt.runtime.message-dispatcher"));


    public static <DataType> GW_BluetoothMessageHandler<DataType> getDispatcher(){
        if(instance == null){
            instance = new GW_BluetoothMessageHandler<DataType>();
        }

    }

    class HandleTask implements Runnable {

        GW_BluetoothMessage<DataType> message;

        public HandleTask(GW_BluetoothMessage<DataType> message){
            this.message = message;
        }

        @Override
        public void run() {

            List<GW_BluetoothMessageReceiver<DataType>> list = receivers.get(message.getProtocol());

            if(list!=null){
                int size = list.size();

                for(int i = 0;i < size;i++){
                    GW_BluetoothMessageReceiver<DataType> receive = list.get(i);
                    if(receive!=null){
                        receive.onReceive(message);
                    }
                }
            }

        }
    }

    public void handle(GW_BluetoothMessage<DataType> message) {
        dispatcherThreadPool.submit(new HandleTask(message));
    }

    public static <DataType> void dispatch(GW_BluetoothMessage<DataType> message){
        GW_BluetoothMessageHandler<DataType> dispatcher =  GW_Bluetooth.getConfiguration().getDispatcher();
        dispatcher.handle(message);
    }

    public void registerBluetoothMessageReceiver(GW_BluetoothConnection.Protocol protocol, GW_BluetoothMessageReceiver<DataType> receiver) {

        List<GW_BluetoothMessageReceiver<DataType>> list = receivers.get(protocol);

        if(list == null){
            list = Collections.synchronizedList(new ArrayList<GW_BluetoothMessageReceiver<DataType>>());
        }

        if(!list.contains(receiver)){
            list.add(receiver);
        }

        receivers.put(protocol, list);
    }

    public void unregisterBluetoothMessageReceiver(GW_BluetoothConnection.Protocol protocol, GW_BluetoothMessageReceiver<DataType> receiver) {
        List<GW_BluetoothMessageReceiver<DataType>> list = receivers.get(protocol);
        list.remove(receiver);
    }
}

