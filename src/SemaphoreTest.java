import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SemaphoreTest {

    public static void main(String[] args) {
        testConcurrentReadWriteWithSemaphore();
    }

    static void testConcurrentReadWriteWithSemaphore() {
        Resource res = new Resource();

//        final Semaphore sem = new Semaphore(2);
//        new Thread(concurrentResourceAccessWithSemaphore(res,sem)).start();
//        new Thread(concurrentResourceAccessWithSemaphore(res,sem)).start();

        final Lock lock = new ReentrantLock();
        new Thread(concurrentResourceAccessWithLock(res,lock)).start();
        new Thread(concurrentResourceAccessWithLock(res,lock)).start();
    }

    static Runnable concurrentResourceAccessWithSemaphore(Resource res, Semaphore sem) {
        return () -> {
            try {
                sem.acquire();
                int x = res.counter;
                System.out.println(Thread.currentThread().getName()+": before res="+x);
                x = x+1;
                res.counter = x;
                System.out.println(Thread.currentThread().getName()+": after res="+res.counter);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } finally {
                sem.release();
            }
        };
    }

    static Runnable concurrentResourceAccessWithLock(Resource res, Lock lock) {
        return () -> {
            try {
                lock.lock();
                int x = res.counter;
                System.out.println(Thread.currentThread().getName()+": before res="+x);
                x = x+1;
                res.counter = x;
                System.out.println(Thread.currentThread().getName()+": after res="+res.counter);
            } finally {
                lock.unlock();
            }
        };
    }

    static void testSemaphoreVsLock() {
//        Semaphore sem = new Semaphore(2);
//        Thread t1 = new Thread(()->consumeResource(sem,2000),"FirstThread");
//        Thread t2 = new Thread(()->consumeResource(sem,1000),"Second Thread");
//        Thread t3 = new Thread(()->consumeResource(sem,500),"Third Thread");

        Lock lock = new ReentrantLock();
        Thread t1 = new Thread(()->consumeResource(lock,5000),"FirstThread");
        Thread t2 = new Thread(()->consumeResource(lock,2500),"Second Thread");
        Thread t3 = new Thread(()->consumeResource(lock,500),"Third Thread");

        t1.start();
        t2.start();
        t3.start();
    }

    static void consumeResource(Lock lock, long millis) {
        try {
            System.out.println(Thread.currentThread().getName()+": Acquiring lock");
            lock.lock();
            System.out.println(Thread.currentThread().getName()+": start working");
            Thread.sleep(millis);
            System.out.println(Thread.currentThread().getName()+": work complete");
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        finally {
            lock.unlock();
            System.out.println(Thread.currentThread().getName()+": unlock");
        }
    }

    static void consumeResource(Semaphore sem, long millis) {
        try {
            System.out.println(Thread.currentThread().getName()+": Acquiring semaphore");
            sem.acquire();
            System.out.println(Thread.currentThread().getName()+": start working");
            Thread.sleep(millis);
            System.out.println(Thread.currentThread().getName()+": work complete");
        }
        catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        finally {
            sem.release();
            System.out.println(Thread.currentThread().getName()+": semaphore released");
        }
    }

    static class Resource {
        int counter = 0;
    }
}
