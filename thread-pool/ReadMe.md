## Custom ThreadPool implementation

This project demonstrates how to implement a full-featured thread pool. This thread pool maintains a fixed number of max active threads. Threads are created as per needed. Also, if a thread in the pool remains idle for the predefined milliseconds then the thread is automatically killed.
This thread pool returns and instance of TaskControl on each submission of a new task. This TaskControl instance can be used to get the result or the exception from the concerned task or cancel the task. Thus, TaskControl proves full grained control over the task. The stopAll feature
helps the thread pool user to bar the pool from accepting new task but wait till the current tasks in queue to complete. The stopAllNow feature bar the pool from accepting new task as well as stop executing any task in the queue. Note that the stopAllNow can not stop the currently running task.

### Learning outcomes

- **Blocking Queue:** implemented a blocking queue. a blocking queue blocks popping when it is empty. in this project blocking queue is implemented using linked list and java concurrent lock and condition. linked list is used because it is faster than array for a queue purpose. for queue, we just
add and remove from tail and head respectively. for array list or array the remove at head triggers shrinking the array which increases the time and memory usage. but for linked list it simply deletes the head node only. besides queue does not sequentially access the elements, for which array or
array list would be best but not linked list, therefore linked list is preferred.
- **Managing threads in pool:** the project implements thread pool that allows at most a certain number of active threads. the max allowed active threads is set during pool creation. but the pool does not create all the threads at the beginning. it creates new thread only when number of active threads
is less than the max allowed active threads and there pending task in queue. each thread loop infinitely and pops task one by one executes it. if there is no more task in the queue then the thread waits a certain millis before it terminates. thus pool ensures that no unnecessary thread is kept alive.
- **Control over running task:** on submitting a task to pool, it returns an instance of TaskControl. one can use it to get the task result or the exception throws during execution, cancel a running task and check if a task is canceled or not. **Note** that cancel does not guarantee the cancellation of
the actual execution; but it any subsequent calls to get or getException method will return null and isCancelled will return true. a task can be canceled before or during the execution of the task. cancelling a finished task will throw IllegalStateException.
the get and getException blocks the caller thread till the task finishes or is canceled.