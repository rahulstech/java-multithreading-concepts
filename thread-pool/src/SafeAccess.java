import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SafeAccess<T> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private T data;

    public SafeAccess() {this(null);}

    public SafeAccess(T initialValue) {
        this.data = initialValue;
    }

    public T get() {
        try {
            lock.readLock().lock();
            return data;
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public void set(T data) {
        try {
            lock.writeLock().lock();
            this.data = data;
        }
        finally {
            lock.writeLock().unlock();
        }
    }
}
