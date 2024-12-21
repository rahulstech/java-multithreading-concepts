## Java Concurrency Client-Server Simulation
This app simulates chat among different user. Users connects to server randomly. Client sends message to other client. The message is first stored into a server database. The server checks if the receiver client is connected or not. If not connected then the messages are queued otherwise sent. Once successfully sent message are removed from the sever. 

### Learnings from this project

- **Protecting resources from concurrent access:** initially `synchronized{}` `instance.wait()` and `instance.notify()` is used to achieve concurrent access protection. To implement it a `final` Object is used as monitor. Before accessing protected resource need to wrap it inside a `synchronized(lockObject) { ... }`.
synchronization ensure the `lockObject` can be accessed simultaneously by multiple thread. Therefore, anything inside the synchronized block can not be accessed by the multiple threads concurrently. Next is wait and notify. **Note** both wait and notify should be called inside a synchronized block on which these methods
are called and wait need to be wrapped with `InterruptedException`. wait will put the caller thread in waiting state until notify is called and will release the locking immediately. Let's say we want the resource to be processed when the certain condition is met otherwise wait till the condition is met. 
- **Using Lock and Condition:** The same thing can be implemented using `Lock` and `Condition` available in java concurrent package. Lock has methods `lock()` and `unlock()`. This does the same thing that synchronized block does. Need to be cautious when using lock. Therefore, safe way to use lock like this
```
Lock lock = ... ;
try {
    lock.lock();
}
finally {
    lock.unlock()
}
```
This ensures that the lock is always unlocked after every use. Otherwise, a deadlock situation may arise. Lock has another method `newCondition()` which returns a `Condition` instance. Condition has methods like `await()` and `signal()` which are similar to `wait` and `notifY()` respectively. As we need to call wait and notify
inside a synchronized block, we need to lock and then unlock the Lock which created the Condition before and after using await and single methods. await throws `InterruptedException` too so need to catch this. Java provides different types of Lock like `ReentrantLock`, `ReadWriteLock`, `StampedLock`. `ReentrantLock` is used for
simple cases. `ReadWriteLock` has `readLock()` and `writeLock()`. It is used when only one thread is allowed to modify the protected resource at time; but multiple threads can read the resource concurrently. `ReentrantReadWriteLock` is an implementation of ReadWriteLock.
- **Use of Semaphore:** `Semaphore` allows only `n` numbers of threads to access the protected resource concurrently. When all the `n` threads are consuming the resource a new request is either canceled or made waiting till some release the resource. The number of permits, i.e. `n`, must be set during creation of Semaphore.
Main difference between Semaphore and Lock is that Semaphore allows permitted number of threads to access the resource simultaneously; but lock allows only one thread to access the resource at a time, the other thread which calls lock method on the same lock instance waits util someone unlocks it. Therefore, avoid semaphore when read as well as
write is involved. 