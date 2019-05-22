package me.w1992wished.zk.lock.server;

/**
 * @author Administrator
 */
public class ZkTest {
    public static void main(String[] args) {
        Runnable task1 = () -> {
            DistributedLock lock = null;
            try {
                lock = new DistributedLock(args[0],"test1");
                //lock = new DistributedLock("127.0.0.1:2182","test2");
                lock.lock();
                Thread.sleep(3000);
                System.out.println("===Thread " + Thread.currentThread().getId() + " running");
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if(lock != null) {
                    lock.unlock();
                }
            }

        };
        new Thread(task1).start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        ConcurrentTest.ConcurrentTask[] tasks = new ConcurrentTest.ConcurrentTask[60];
        for(int i=0;i<tasks.length;i++){
            ConcurrentTest.ConcurrentTask task3 = () -> {
                DistributedLock lock = null;
                try {
                    lock = new DistributedLock(args[0],"test2");
                    lock.lock();
                    Thread.sleep(3000);
                    System.out.println("Thread " + Thread.currentThread().getId() + " running");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    lock.unlock();
                }

            };
            tasks[i] = task3;
        }
        new ConcurrentTest(tasks);
    }
}