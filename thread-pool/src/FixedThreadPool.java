import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.*;

public class FixedThreadPool {

    private static final int MAX_THREADS = 32;

    private static final long MAX_IDLE_MILLS = 1000;

    // linked list is used as queue. in queue exactly two operations are
    // frequently needed:  adding at tail and removing from head. sequential
    // access of elements is not concern in queue.

    final TaskQueue taskQueue = new TaskQueue();

    final int nThreads;

    final LinkedList<Thread> activeThreads = new LinkedList<>();

    final Lock activeThreadsLock = new ReentrantLock();

//    final ThreadData<Boolean> cancelFlag = new ThreadData<>(false);

    boolean stopped = false;

    public FixedThreadPool(int nThreads) {
        this.nThreads = Math.min(MAX_THREADS, nThreads);
    }

    public void execute(Callable<?> task) {
        if (stopped) {
            throw new IllegalStateException("Pool was already stopped, can not accept more task");
        }
        // step1 push the task to the queue
        pushTask(task);

        // step2 start a thread
        runTask();
    }

    private void pushTask(Callable<?> task) {
        taskQueue.push(task);
    }

    private Callable<?> popTask() throws InterruptedException {
        return taskQueue.pop(MAX_IDLE_MILLS);
    }

    private void runTask() {
        try {
            activeThreadsLock.lock();
            if (activeThreads.size() < nThreads) {
                Thread thread = createNewThread();
                activeThreads.add(thread);
                thread.start();
            }
        }
        finally {
            activeThreadsLock.unlock();
        }
    }

    private Thread createNewThread() {
        return new Thread(this::run);
    }

    private void removeThread(Thread thread) {
        try {
            activeThreadsLock.lock();
            System.out.println("removing thread: "+thread.getName());
            activeThreads.remove(thread);
//            cancelFlag.remove(thread);
        }
        finally {
            activeThreadsLock.unlock();
        }
    }

    private void run() {
        while (true) {
            try {

//                if (cancelFlag.get()) {
//                    break;
//                }

                // pop a task and run the task in a thread
                Callable<?> task = popTask();
                if (null == task) {
                    break;
                }
                try {
                    task.call();
                }
                catch (Exception ex) {
                    System.out.println("task raised exception: "+ex.getMessage());
                }
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
                break;
            }
        }
        removeThread(Thread.currentThread());
    }

    /**
     * Similarities: stopAll and stopAllNow is when called any
     * subsequent call to execute will throw IllegalStateException
     * Difference: stopAll let all the pending tasks in queue to execute;
     * but stopAllNow does wait till all pending tasking to be completed
     */

    public void stopAll() {
        stopped = true;
    }

    public List<Callable<?>> stopAllNow() {
        stopped = true;
        List<Callable<?>> tasks = taskQueue.empty();
        markThreadsCanceled();
        return tasks;
    }

    private void markThreadsCanceled() {
        List<Thread> copy;
        try {
            activeThreadsLock.lock();
            copy = new ArrayList<>(activeThreads);
        }
        finally {
            activeThreadsLock.unlock();
        }
        for (Thread t : copy) {
            t.interrupt(); // why I need to call this?
//            cancelFlag.set(t,true);
        }
    }
}
