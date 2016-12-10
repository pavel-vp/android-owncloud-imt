package com.mobile_me.imtv_player.service.tasks;

import android.os.Handler;
import android.util.SparseArray;

import com.mobile_me.imtv_player.util.CustomExceptionHandler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by pasha on 8/13/16.
 */
public class AsyncTaskManager2 {

    public interface IAsyncTaskListener {
        void onBackroundTaskComplete(int taskId, Runnable task);
    }

    private static AsyncTaskManager2 instance = new AsyncTaskManager2();

    public static AsyncTaskManager2 getInstance() {
        return instance;
    }

    private AtomicInteger lastOwnerId = new AtomicInteger(0);

    public int generateNewOwnerId() {
        return lastOwnerId.incrementAndGet();
    }

    private volatile AtomicInteger lastTaskId = new AtomicInteger(0);

    private int generateNewTaskId() {
        return lastTaskId.incrementAndGet();
    }

    private SparseArray<IAsyncTaskListener> listeners = new SparseArray<>();

    public void registerListener(int ownerId, IAsyncTaskListener listener) {
        synchronized (listeners) {
            listeners.put(ownerId, listener);
        }
    }

    public void unRegisterListener(int ownerId) {
        synchronized (listeners) {
            listeners.remove(ownerId);
        }
    }

    private Executor executor = Executors.newCachedThreadPool();

    public int runTask(final int ownerId, final Runnable task) {
        final int taskId = this.generateNewTaskId();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (Exception e) {
                    CustomExceptionHandler.logException("asynk task error ", e);
                }
                result(ownerId, taskId, task);
            }
        });
        return taskId;
    }

    Handler handler =  new Handler();

    private void result(final int ownerId, final int taskId, final Runnable task) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                runInUIThread(ownerId, taskId, task);
            }
        });
    }

    private void runInUIThread(final int ownerId, final int taskId, final Runnable task) {
        IAsyncTaskListener listener;
        synchronized (listeners) {
            listener = listeners.get(ownerId);
        }
        if (listener != null) {
            listener.onBackroundTaskComplete(taskId, task);
        }
    }


}
