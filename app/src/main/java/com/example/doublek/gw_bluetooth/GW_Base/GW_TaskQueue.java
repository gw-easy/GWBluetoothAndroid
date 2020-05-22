package com.example.doublek.gw_bluetooth.GW_Base;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.doublek.gw_bluetooth.GW_Base.GW_BluetoothThread.getHandler;

public class GW_TaskQueue extends HandlerThread {

    private Handler mHandler;
    final ConcurrentHashMap<Runnable, TaskWrapper> taskMapping = new ConcurrentHashMap<Runnable, TaskWrapper>();

    public GW_TaskQueue(String name) {
        this(name,null);
    }

    public GW_TaskQueue(String name,Handler.Callback callback) {
        super(name, android.os.Process.THREAD_PRIORITY_DEFAULT);
        ensureThreadLocked(callback);
    }

    void ensureThreadLocked(Handler.Callback callback) {
        start();
        mHandler = new Handler(getLooper(),callback);
    }
    //    提交任务
    public void submitTask(Runnable task){
        TaskWrapper taskWrapper = TaskWrapper.create(task, this);
        taskMapping.put(task, taskWrapper);
        getHandler().post(taskWrapper);
    }

    public void submitTask(Runnable task,long delayMillis){
        TaskWrapper taskWrapper = TaskWrapper.create(task, this);
        taskMapping.put(task, taskWrapper);
        getHandler().postDelayed(taskWrapper, delayMillis);
    }

    void remove(TaskWrapper taskWrapper){
        getHandler().removeCallbacks(taskWrapper);
        if(taskWrapper.taskImpl!=null){
            taskMapping.remove(taskWrapper.taskImpl);
            taskWrapper.taskImpl = null;
        }
    }

    final static class TaskWrapper implements Runnable {

        Runnable taskImpl;
        GW_TaskQueue taskQueue;

        private TaskWrapper(Runnable runnable,GW_TaskQueue queue){
            taskImpl = runnable;
            this.taskQueue = queue;
        }

        public static TaskWrapper create(Runnable runnable,GW_TaskQueue queue){
            TaskWrapper taskWrapper = new TaskWrapper(runnable,queue);
            return taskWrapper;
        }

        @Override
        public void run() {

            try {
                if(taskImpl!=null){
                    taskImpl.run();
                }
            } finally {
                taskQueue.remove(this);
            }

        }
    }

//    移除任务
    public void removeTasks(Runnable ...tasks){

        for (int i = 0; i < tasks.length; i++) {
            Runnable task = tasks[i];
            if(task == null) continue;
            TaskWrapper taskWrapper = taskMapping.remove(task);
            if(taskWrapper!=null){
                taskWrapper.taskImpl = null;
                getHandler().removeCallbacks(taskWrapper);
            }
        }
    }

//    移除所有任务
    public void removeAllTasks(){

        for(Iterator<Map.Entry<Runnable, TaskWrapper>> iter = taskMapping.entrySet().iterator(); iter.hasNext();){

            Map.Entry<Runnable, TaskWrapper> item = iter.next();

            TaskWrapper taskWrapper = item.getValue();
            taskWrapper.taskImpl = null;
            getHandler().removeCallbacks(taskWrapper);

        }

        taskMapping.clear();

    }
}
