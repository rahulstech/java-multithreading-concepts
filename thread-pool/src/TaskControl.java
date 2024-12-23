public interface TaskControl<T> {

    /**
     * waits for the task to completes indefinitely or
     * returns immediately if task has finished already
     * @return the result
     */
    T get();

    /**
     * Returns any exception throws during task execution. It may be {@literal  null}.
     * Just like {@link #get()} method it waits indefinitely till the task is finished.
     * If task is already finished then it returns immediately
     *
     * @return the exception or null
     */
    Exception getException();


    /**
     * Cancels the running task. After cancellation get will always return null
     */
    void cancel();

    /**
     * Check weather the task is canceled calling {@link #cancel()}
     *
     * @return {@code true} task canceled previously, {@code false} otherwise
     */
    boolean isCanceled();
}
