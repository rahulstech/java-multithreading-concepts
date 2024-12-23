import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class ThreadPoolTest {

    public static void main(String[] args) throws Exception {
//        testGetTaskResult();

//        testGetTaskResultWhenTaskAlreadyFinished();

        testCancelTask();
    }
    
    static void testRunMultipleTasksInLimitedThreads() throws Exception {
        FixedThreadPool pool = new FixedThreadPool(10);
        Random random = new Random();
        for (int i = 0; i < 2000; i++) {
            pool.execute(new Task("task-"+i, random.nextLong(100, 1000)));
        }
    }

    static void testStopAll() throws Exception {
        FixedThreadPool pool = new FixedThreadPool(10);
        Random random = new Random();
        for (int i = 0; i < 2000; i++) {
            pool.execute(new Task("task-"+i, random.nextLong(100, 1000)));
        }
        Thread.sleep(2000);

        pool.stopAll();
    }

    static void testStopAllNow() throws Exception {
        FixedThreadPool pool = new FixedThreadPool(10);
        Random random = new Random();
        for (int i = 0; i < 2000; i++) {
            pool.execute(new Task("task-"+i, random.nextLong(100, 1000)));
        }
        Thread.sleep(2000);
        
        List<Callable<?>> tasks = pool.stopAllNow();
        for (Callable<?> task : tasks) {
            System.out.println("not completed: "+((Task) task).taskName);
        }
    }
    
    static void testGetTaskResult() throws Exception {
        FixedThreadPool pool = new FixedThreadPool(1);
        
        TaskControl<Integer> control = (TaskControl<Integer>) pool.execute(() -> {
            System.out.println("starting task that returns result");
            Thread.sleep(2000);
            System.out.println("task that returns result complete");
            return 7;
        });

        System.out.println("waiting for result...");
        int result = control.get();
        System.out.println("task result returned: "+result);

    }

    static void testGetTaskResultWhenTaskAlreadyFinished() throws Exception {
        FixedThreadPool pool = new FixedThreadPool(1);

        TaskControl<Integer> control = (TaskControl<Integer>) pool.execute(() -> {
            System.out.println("starting task ");
            Thread.sleep(1000);
            System.out.println("task complete");
            return 7;
        });

        System.out.println("delaying to let the task finish before");
        Thread.sleep(5000);

        System.out.println("waiting for result...");
        int result = control.get();
        System.out.println("task result returned: "+result);
    }

    static void testCancelTask() throws Exception {
        FixedThreadPool pool = new FixedThreadPool(2);

        final TaskControl<Integer> control = (TaskControl<Integer>) pool.execute(() -> {
            System.out.println("starting task1");
            Thread.sleep(5000);
            System.out.println("task1 complete");
            return 7;
        });

        pool.execute(() -> {
            System.out.println("i am going to cancel the task1");
            Thread.sleep(2500);
            control.cancel();
            System.out.println("task1 canceled");
            return null;
        });

        System.out.println("waiting for result...");
        Integer result = control.get();
        System.out.println("task result returned: " + result +" expected 7");
    }

    static class Task implements Callable<Void> {

        final String taskName;

        final long taskTime;

        public Task(String name, long time) {
            this.taskName = name;
            this.taskTime = time;
        }

        @Override
        public Void call() throws Exception {
            System.out.println(getThreadName()+": starting task "+taskName);
            Thread.sleep(taskTime);
            System.out.println(getThreadName()+": finish task: "+taskName+" took "+taskTime+"ms");
            return null;
        }

        String getThreadName() {
            return Thread.currentThread().getName();
        }
    }
}
