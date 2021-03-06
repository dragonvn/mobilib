package com.datdo.mobilib.util;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Tiny but very useful. Every instance of this class is a queue which you can push any task into it and wait for them to complete one after one.
 * Task is defined by implementing <code>MblSerializer#Task</code> interface.
 * Task is always invoked in main thread, then it can split processing to another thread itself if needed.
 *
 * Sample code:
 *
 * {@code
 *      MblSerializer s = new MblSerializer();
 *
 *      s.run(new MblSerializer.Task() {
 *          @Override
 *          public void run(Runnable finishCallback) {
 *              // run task 1 in async thread
 *              MblUtils.executeOnAsyncThread(new Runnable() {
 *                  // ... do something here
 *
 *                  // finally, invoke callback
 *                  finishCallback.run();
 *              });
 *          }
 *      });
 *
 *      s.run(new MblSerializer.Task() {
 *          @Override
 *          public void run(Runnable finishCallback) {
 *              // run task 2 in async thread
 *              MblUtils.executeOnAsyncThread(new Runnable() {
 *                  // ... do something here
 *
 *                  // finally, invoke callback
 *                  finishCallback.run();
 *              });
 *          }
 *      });
 *
 *      // ... next task/action comes here
 * }
 * </pre>
 */
public class MblSerializer {

    /**
     * Interface to define a task.
     */
    public static interface Task {
        public void run(Runnable finishCallback);
    }

    private final List<Task>    mTasks              = new ArrayList<Task>();
    private boolean             mIsRunning          = false;
    private final Runnable      mFinishCallback     = new Runnable() {
        @Override
        public void run() {
            mIsRunning = false;
            runNextTask();
        }
    };

    private void runNextTask() {
        // post to main thread to prevent StackOverFlow
        MblUtils.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                synchronized (MblSerializer.this) {
                    if (!mIsRunning && !mTasks.isEmpty()) {
                        mIsRunning = true;
                        mTasks.remove(0).run(mFinishCallback);
                    }
                }
            }
        });
    }

    /**
     * <pre>
     * Add a task to queue.
     * Task is run immediately if no task is running, otherwise wait for its turn.
     * </pre>
     */
    public void run(Task task) {
        synchronized (this) {
            mTasks.add(task);
            runNextTask();
        }
    }

    /**
     * Cancel a specific task.
     * @return true if task exists in queue
     */
    public boolean cancel(Task task) {
        synchronized (this) {
            return mTasks.remove(task);
        }
    }

    /**
     * Cancel all tasks.
     */
    public void cancelAll() {
        synchronized (this) {
            mTasks.clear();
        }
    }
}
