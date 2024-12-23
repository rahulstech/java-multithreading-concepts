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

    boolean stopped = false;

    public FixedThreadPool(int nThreads) {
        this.nThreads = Math.min(MAX_THREADS, nThreads);
    }

    public TaskControl<?> execute(Callable<?> task) {
        if (stopped) {
            throw new IllegalStateException("Pool was already stopped, can not accept more task");
        }
        // step1 push the task to the queue
        TaskControl<?> control = pushTask(task);

        // step2 start a thread
        runTask();

        return control;
    }

    private <T> TaskControl<T> pushTask(Callable<T> task) {
        TaskControlImpl<T> control = new TaskControlImpl<>(task);
        taskQueue.push(control);
        return control;
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
        }
        finally {
            activeThreadsLock.unlock();
        }
    }

    private void run() {
        while (true) {
            try {
                // pop a task and run the task in a thread
                TaskControlImpl<?> task = (TaskControlImpl<?>) popTask();
                if (null == task) {
                    break;
                }
                task.execute();
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
            t.interrupt(); // why? = otherwise thread may await infinitely
        }
        copy.clear(); // clear the copy to help gc
    }

    private static class TaskControlImpl<T> implements TaskControl<T>, Callable<T> {

        private boolean finished = false;

        private final Object lockCanceled = new Object();

        private boolean canceled = false;

        private T result;

        private Exception exception;

        private final Lock lockResult = new ReentrantLock();

        private final Condition conditionResult = lockResult.newCondition();

        private final Callable<T> task;

        public TaskControlImpl(Callable<T> task) {
            this.task = task;
        }

        @Override
        public T get() throws InterruptedException {
            try {
                lockResult.lock();
                if (!finished) {
                    conditionResult.await();
                }
            }
            finally {
                lockResult.unlock();
            }
            return result;
        }

        public void set(T result) {
            try {
                lockResult.lock();
                this.result = result;
                markFinished();
            }
            finally {
                lockResult.unlock();
            }
        }

        @Override
        public void cancel() {
            try {
                lockResult.lock();
                if (finished) {
                    throw new IllegalStateException("can not cancel finished task");
                }
            }
            finally {
                lockResult.unlock();
            }
            synchronized (lockCanceled) {
                canceled = true;
            }
        }

        @Override
        public boolean isCanceled() {
            synchronized (lockCanceled) {
                return canceled;
            }
        }

        @Override
        public Exception getException() throws InterruptedException {
            try {
                lockResult.lock();
                if (!finished) {
                    conditionResult.await();
                }
            }
            finally {
                lockResult.unlock();
            }
            return exception;
        }

        public void setException(Exception exception) {
            try {
                lockResult.lock();
                this.exception = exception;
                markFinished();
            }
            finally {
                lockResult.unlock();
            }
        }

        // Due to some historial implementation I have to use the Callable interface
        // otherwise I can simply use Runnable. Hence, this call has no use here
        @Override
        public T call() throws Exception {return null;}

        public void execute() {
            // this method is called from a worker thread
            try {
                // if task is canceled before starting of execution
                // then mark it as finished and return
                if (isCanceled()) {
                    markFinished();
                    return;
                }

                T result = task.call();
                // if task is canceled before completion of execution
                // then mark it as finished and don't set result
                if (isCanceled()) {
                    markFinished();
                    return;
                }
                set(result);
            }
            catch (Exception ex) {
                // if task is canceled before completion of execution
                // then mark it as finished and don't set exception
                if (isCanceled()) {
                    markFinished();
                    return;
                }
                setException(ex);
            }
        }

        private void markFinished() {
            try {
                lockResult.lock();
                finished = true;
                conditionResult.signalAll();
            }
            finally {
                lockResult.unlock();
            }
        }
    }
}
