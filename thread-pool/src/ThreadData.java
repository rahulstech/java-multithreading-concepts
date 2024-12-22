import java.util.HashMap;

public class ThreadData<T> {

    final HashMap<Thread, T> store = new HashMap<>();

    final T initialData;

    public ThreadData(T initialData) {
        this.initialData = initialData;
    }

    public T get() {
        return store.getOrDefault(Thread.currentThread(),initialData);
    }

    public void set(T data) {
        store.put(Thread.currentThread(), data);
    }

    public void set(Thread thread, T data) {
        synchronized (store) {
            store.put(thread, data);
        }
    }

    public void remove(Thread thread) {
        synchronized (store) {
            store.remove(thread);
        }
    }
}
