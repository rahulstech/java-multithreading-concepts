import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TaskQueue {

    final LinkedList<Callable<?>> queue = new LinkedList<>();

    final Lock lock = new ReentrantLock();

    final Condition condition = lock.newCondition();

    public Callable<?> pop() throws InterruptedException {
        try {
            lock.lock();
            if (queue.isEmpty()) {
                condition.await();
            }
            return popInternal();
        }
        finally {
            lock.unlock();
        }
    }

    public Callable<?> pop(long timeout_millis) throws InterruptedException {
        try {
            lock.lock();
            if (queue.isEmpty()) {
                if (!condition.await(timeout_millis, TimeUnit.MILLISECONDS)) {
                    return popInternal();
                }
            }
            return popInternal();
        }
        finally {
            lock.unlock();
        }
    }

    public void push(Callable<?> task) {
        try {
            lock.lock();
            queue.add(task);
            condition.signalAll();
        }
        finally {
            lock.unlock();
        }
    }

    private Callable<?> popInternal() {
        if (queue.isEmpty()) {
            return null;
        }
        return queue.removeFirst();
    }

    public List<Callable<?>> empty() {
        try {
            lock.lock();
            List<Callable<?>> snaps = new ArrayList<>(queue);
            queue.clear();
            return snaps;
        }
        finally {
            lock.unlock();
        }
    }
}
