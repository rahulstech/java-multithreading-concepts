import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class ThreadPoolTest {

    public static void main(String[] args) throws Exception {

        FixedThreadPool pool = new FixedThreadPool(10);
        Random random = new Random();

        for (int i = 0; i < 2000; i++) {
            pool.execute(new Task("task-"+i, random.nextLong(100, 1000)));
        }

        Thread.sleep(2000);

//        pool.stopAll();
//
//        try {
//            pool.execute(new Task("new-task",500));
//        }
//        catch (IllegalStateException ex) {
//            ex.printStackTrace();
//        }

        List<Callable<?>> tasks = pool.stopAllNow();
        for (Callable<?> task : tasks) {
            System.out.println("not completed: "+((Task) task).taskName);
        }
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
