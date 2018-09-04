import com.dragon.learn.reentrantlock.setnxandexpire.SetNxAndExpireLockUtil;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 * 用途描述  *
 *
 * @author 王金龙
 * @version $Id: DeadLock, v0.1
 * @company 杭州信牛网络科技有限公司
 * @date 2018年04月27日 下午5:26 王金龙 Exp $
 */
public class DeadLockRun {


    /**
     * 模拟100个客户端获取锁，但只有一个获取锁成功
     * @throws InterruptedException
     */
    @Test
    public void getLockNormalTest() throws InterruptedException {
        String lockKey = "getLockNormalTest:" + System.currentTimeMillis();
        System.out.println("模拟10个线程同时获取分布式锁");

        CountDownLatch countDownLatch = new CountDownLatch(1);

        CountDownLatch countDownLatch2 = new CountDownLatch(100);

        Thread[] threads = new Thread[100];
        for(int i =0;i<threads.length;i++){
            threads[i] = new Thread(() -> {
                try {
                    countDownLatch.await();
                    System.out.println(Thread.currentThread().getName()+"开始获取锁"+System.currentTimeMillis());
                    boolean result = SetNxAndExpireLockUtil.lock(lockKey, 30);
                    if(result){
                        System.out.println(Thread.currentThread().getName()+"获取分布式锁成功----------------------");
                        System.out.println(Thread.currentThread().getName()+"开始 执行业务操作");
                        TimeUnit.SECONDS.sleep(2);
                        System.out.println(Thread.currentThread().getName()+"完成 执行业务操作");

                        System.out.println(Thread.currentThread().getName()+"开始 释放锁");
                        SetNxAndExpireLockUtil.unLock(lockKey);
                        System.out.println(Thread.currentThread().getName()+"完成 释放锁--------------------------");

                    }else {
                        System.out.println(Thread.currentThread().getName()+"获取分布式锁失败");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    countDownLatch2.countDown();
                }
            });
            threads[i].setName("线程 "+i);
            threads[i].start();
        }

        countDownLatch.countDown();


        countDownLatch2.await();

    }


    /**
     * 解决多个客户端同时获得锁的问题。
     * 1、每个客户端设置的值为唯一。
     * 2、删除KEY用LUA脚本，保证是原子性的且是当前客户端所设置的值。
     *
     * if redis.call("get",KEYS[1]) == ARGV[1] then
     *     return redis.call("del",KEYS[1])
     * else
     *     return 0
     * end
     *
     *
     * 客户端A获取锁后线程挂起了。挂起时间（6s）大于锁的过期时间(3s)。
     * 锁过期后(5秒后)，客户端B获取锁。
     * 客户端A恢复以后，处理完相关事件。通过lua删除锁（失败，不是A所设置的值）
     * 客户端C获取锁。（等到B释放后获取锁）
     *
     */
    @Test
    public void solveMultiClientGetLock() throws InterruptedException {
        CountDownLatch countDownLatch2 = new CountDownLatch(3);
        String lockKey = "solveMultiClientGetLock:" + System.currentTimeMillis();


        Thread clinetA = new Thread(()->{
            try {
                String clinetAValue = UUID.randomUUID().toString();
                System.out.println("客户端A开始获取锁");

                boolean clientAResult = SetNxAndExpireLockUtil.lockAtomic(lockKey,clinetAValue, 3);

                if(clientAResult){
                    System.out.println(Thread.currentThread().getName()+" 获取锁成功*************************");
                    System.out.println(Thread.currentThread().getName()+" 执行业务操作 时间比较长,超过锁的有效期");
                    TimeUnit.SECONDS.sleep(6);
                    System.out.println(Thread.currentThread().getName()+" 开始释放锁");
                    long lockResult = SetNxAndExpireLockUtil.unLockWithLua(lockKey,clinetAValue);
                    System.out.println(lockResult==1L?Thread.currentThread().getName()+" 完成释放锁":Thread.currentThread().getName()+"无需释放锁！！！！！！！！！");
                }else {
                    System.out.println(Thread.currentThread().getName()+" 获取锁失败");
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                countDownLatch2.countDown();
            }


            Thread clinetC = new Thread(()->{
                try {

                    String clinetBValue = UUID.randomUUID().toString();
                    boolean clientAResult = SetNxAndExpireLockUtil.lockAtomic(lockKey,clinetBValue, 3);

                    if(clientAResult){
                        System.out.println(Thread.currentThread().getName()+" 获取锁成功*************************");
                        System.out.println(Thread.currentThread().getName()+" 执行业务操作");
                        TimeUnit.SECONDS.sleep(1);
                        System.out.println(Thread.currentThread().getName()+" 开始释放锁");
                        Long lockResult = SetNxAndExpireLockUtil.unLockWithLua(lockKey,clinetBValue);
                        System.out.println(lockResult==1L?Thread.currentThread().getName()+" 完成释放锁":Thread.currentThread().getName()+"无需释放锁！！！！！！！！！");
                    }else {
                        System.out.println(Thread.currentThread().getName()+" 获取锁失败");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    countDownLatch2.countDown();
                }


            },"客户端C");

            clinetC.start();

        },"客户端A");
        clinetA.start();

        TimeUnit.SECONDS.sleep(5);
        Thread clinetB = new Thread(()->{
            try {

                String clinetCValue = UUID.randomUUID().toString();

                boolean clientAResult = SetNxAndExpireLockUtil.lockAtomic(lockKey,clinetCValue, 3);
                if(clientAResult){
                    System.out.println(Thread.currentThread().getName()+" 获取锁成功*************************");
                    System.out.println(Thread.currentThread().getName()+" 执行业务操作");
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(Thread.currentThread().getName()+" 开始释放锁");
                    long lockResult =SetNxAndExpireLockUtil.unLockWithLua(lockKey,clinetCValue);
                    System.out.println(lockResult==1L?Thread.currentThread().getName()+" 完成释放锁":Thread.currentThread().getName()+"无需释放锁！！！！！！！！！");
                }else {
                    System.out.println(Thread.currentThread().getName()+" 获取锁失败");
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                countDownLatch2.countDown();
            }


        },"客户端B");
        clinetB.start();
        countDownLatch2.await();
    }

    /**
     * 通过原子性的SET命令解决死锁问题。SET resource_name my_random_value NX PX 30000
     */
    @Test
    public void solveDeadLockWithAtomicLock() throws InterruptedException {

        System.out.println("客户端A开始获取锁");
        String lockKey = "testDeadLock:" + System.currentTimeMillis();

        boolean clientAResult = SetNxAndExpireLockUtil.lockAtomic(lockKey, 30);

        if(clientAResult){
            System.out.println("客户端A 获取锁成功");
            System.out.println("客户端A 执行业务操作");
            TimeUnit.SECONDS.sleep(10);
            System.out.println("客户端A 开始释放锁");
            SetNxAndExpireLockUtil.unLock(lockKey);
            System.out.println("客户端A 完成释放锁");
        }else {
            System.out.println("客户端A 获取锁失败");
        }


        CountDownLatch countDownLatch = new CountDownLatch(1);

        CountDownLatch countDownLatch2 = new CountDownLatch(10);
        Thread[] threads = new Thread[10];
        for(int i =0;i<threads.length;i++){
            threads[i] = new Thread(() -> {
                try {
                    countDownLatch.await();
//                    System.out.println(Thread.currentThread().getName()+"开始获取锁"+System.currentTimeMillis());
                    boolean result = SetNxAndExpireLockUtil.lock(lockKey, 30);
                    if(result){
                        System.out.println(Thread.currentThread().getName()+"获取分布式锁成功----------------------");
                        System.out.println(Thread.currentThread().getName()+"开始 执行业务操作");
                        TimeUnit.SECONDS.sleep(2);
                        System.out.println(Thread.currentThread().getName()+"完成 执行业务操作");

                        System.out.println(Thread.currentThread().getName()+"开始 释放锁");
                        SetNxAndExpireLockUtil.unLock(lockKey);
                        System.out.println(Thread.currentThread().getName()+"完成 释放锁--------------------------");

                    }else {
                        System.out.println(Thread.currentThread().getName()+"获取分布式锁失败");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    countDownLatch2.countDown();
                }
            });
            threads[i].setName("线程 "+i);
            threads[i].start();
        }

        countDownLatch.countDown();

        // 等待线程结果
        countDownLatch2.await();

    }
    /**
     * 模拟客户端A在获取锁时设置过期时间失败了（setnx成功，expire失败），其它客户端都无法获取锁的死锁问题。
     */
    @Test
    public void testDeadLock() throws InterruptedException {

        System.out.println("客户端A开始获取锁，但设置过期时间失败");
        String lockKey = "testDeadLock:" + System.currentTimeMillis();

        boolean clientAResult = SetNxAndExpireLockUtil.lockWithExpireFail(lockKey, 30);

        if(clientAResult){
            System.out.println("客户端A 获取锁成功");
            System.out.println("客户端A 执行业务操作");
            TimeUnit.SECONDS.sleep(1);
            System.out.println("客户端A 开始释放锁");
            SetNxAndExpireLockUtil.unLock(lockKey);
            System.out.println("客户端A 完成释放锁");
        }else {
            System.out.println("客户端A 获取锁失败");
        }


        CountDownLatch countDownLatch = new CountDownLatch(1);

        CountDownLatch countDownLatch2 = new CountDownLatch(10);
        Thread[] threads = new Thread[10];
        for(int i =0;i<threads.length;i++){
            threads[i] = new Thread(() -> {
                try {
                    countDownLatch.await();
//                    System.out.println(Thread.currentThread().getName()+"开始获取锁"+System.currentTimeMillis());
                    boolean result = SetNxAndExpireLockUtil.lock(lockKey, 30);
                    if(result){
                        System.out.println(Thread.currentThread().getName()+"获取分布式锁成功----------------------");
                        System.out.println(Thread.currentThread().getName()+"开始 执行业务操作");
                        TimeUnit.SECONDS.sleep(2);
                        System.out.println(Thread.currentThread().getName()+"完成 执行业务操作");

                        System.out.println(Thread.currentThread().getName()+"开始 释放锁");
                        SetNxAndExpireLockUtil.unLock(lockKey);
                        System.out.println(Thread.currentThread().getName()+"完成 释放锁--------------------------");

                    }else {
                        System.out.println(Thread.currentThread().getName()+"获取分布式锁失败");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    countDownLatch2.countDown();
                }
            });
            threads[i].setName("线程 "+i);
            threads[i].start();
        }

        countDownLatch.countDown();

        // 等待线程结果
        countDownLatch2.await();

    }


    /**
     * 模拟多个客户端同时获取锁（客户端A删除了客户端B设置的锁）
     *
     * 客户端A获取锁后线程挂起了。挂起时间（6s）大于锁的过期时间(3s)。
     * 锁过期后(5秒后)，客户端B获取锁。
     * 客户端A恢复以后，处理完相关事件，向redis发起 del命令。锁被释放(B失去锁保护)
     * 客户端C获取锁。客户端BC同时获得锁。
     */
    @Test
    public void testMultiClientGetLock() throws InterruptedException {


        CountDownLatch countDownLatch2 = new CountDownLatch(3);
        String lockKey = "testMultiClientGetLock:" + System.currentTimeMillis();


        Thread clinetA = new Thread(()->{
            try {

                System.out.println("客户端A开始获取锁");

                boolean clientAResult = SetNxAndExpireLockUtil.lock(lockKey, 3);

                if(clientAResult){
                    System.out.println(Thread.currentThread().getName()+" 获取锁成功*************************");
                    System.out.println(Thread.currentThread().getName()+" 执行业务操作 时间比较长");
                    TimeUnit.SECONDS.sleep(6);
                    System.out.println(Thread.currentThread().getName()+" 开始释放锁");
                    SetNxAndExpireLockUtil.unLock(lockKey);
                    System.out.println(Thread.currentThread().getName()+" 完成释放锁");
                }else {
                    System.out.println(Thread.currentThread().getName()+" 获取锁失败");
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                countDownLatch2.countDown();
            }


            Thread clinetC = new Thread(()->{
                try {

                    boolean clientAResult = SetNxAndExpireLockUtil.lock(lockKey, 3);

                    if(clientAResult){
                        System.out.println(Thread.currentThread().getName()+" 获取锁成功*************************");
                        System.out.println(Thread.currentThread().getName()+" 执行业务操作");
                        TimeUnit.SECONDS.sleep(1);
                        System.out.println(Thread.currentThread().getName()+" 开始释放锁");
                        SetNxAndExpireLockUtil.unLock(lockKey);
                        System.out.println(Thread.currentThread().getName()+" 完成释放锁");
                    }else {
                        System.out.println(Thread.currentThread().getName()+" 获取锁失败");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    countDownLatch2.countDown();
                }


            },"客户端C");

            clinetC.start();

        },"客户端A");
        clinetA.start();

        TimeUnit.SECONDS.sleep(5);
        Thread clinetB = new Thread(()->{
            try {

                boolean clientAResult = SetNxAndExpireLockUtil.lock(lockKey, 3);

                if(clientAResult){
                    System.out.println(Thread.currentThread().getName()+" 获取锁成功*************************");
                    System.out.println(Thread.currentThread().getName()+" 执行业务操作");
                    TimeUnit.SECONDS.sleep(4);
                    System.out.println(Thread.currentThread().getName()+" 开始释放锁");
                    SetNxAndExpireLockUtil.unLock(lockKey);
                    System.out.println(Thread.currentThread().getName()+" 完成释放锁");
                }else {
                    System.out.println(Thread.currentThread().getName()+" 获取锁失败");
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                countDownLatch2.countDown();
            }


        },"客户端B");
        clinetB.start();
        countDownLatch2.await();



    }

}
