package com.mobile_me.imtv_player.ui;

import android.app.Activity;
import android.os.Bundle;

import com.mobile_me.imtv_player.service.tasks.AsyncTaskManager2;

/**
 * Created by pasha on 8/13/16.
 */
public abstract class AbstractBaseActivity extends Activity {
    private static final String OWNER_ID_KEY = "owner_id";

    protected int ownerId = 0;

    private AsyncTaskManager2.IAsyncTaskListener bgTaskListener = new AsyncTaskManager2.IAsyncTaskListener() {
        @Override
        public void onBackroundTaskComplete(int taskId, Runnable task) {
            //hideProgressDialog(taskId);
            AbstractBaseActivity.this.onBackgroundTaskComplete(taskId, task);
        }
    };

    protected void onBackgroundTaskComplete(int taskId, Runnable task) {
        //for overriding in descendants
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            ownerId = savedInstanceState.getInt(OWNER_ID_KEY, 0);
        }
        if (ownerId == 0) {
            ownerId = AsyncTaskManager2.getInstance().generateNewOwnerId();
        }
        AsyncTaskManager2.getInstance().registerListener(ownerId, this.bgTaskListener);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        AsyncTaskManager2.getInstance().unRegisterListener(ownerId);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE"); // dont know why
        outState.putInt(OWNER_ID_KEY, ownerId);
        super.onSaveInstanceState(outState);
    }

    /**
     * Запускает задачу в фоновом потоке. Результат возвращается через метод onBackgroundTaskComplete.
     * Результат будет возвращен даже после перезапуска Activity при изменении конфигурации.
     * Диалог прогресса показываться не будет
     * @param task - запускаемая задача
     * @return taskId - идентфикатор запущенной задачи. Может использоваться для отслеживания нужной задачи,
     * если запускается много однотипных задач
     */
    protected int runTaskInBackgroundNoDialog(Runnable task) {
        int taskId = AsyncTaskManager2.getInstance().runTask(ownerId, task);
        return taskId;
    }

}
