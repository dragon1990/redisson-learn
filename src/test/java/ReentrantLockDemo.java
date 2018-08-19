import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ReentrantLockDemo {

    RedissonClient redisson;

    @Before
    public void init() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        redisson = Redisson.create(config);

    }

    @Test
    public void lockManual() {

        RLock lock = redisson.getLock("anyLock");
        // 最常见的使用方法
        lock.lock();

        lock.unlock();
    }

    @Test
    public void lockWithTime() throws InterruptedException {

        RLock lock = redisson.getLock("anyLock");
        // 加锁以后10秒钟自动解锁
        // 无需调用unlock方法手动解锁
        lock.lock(10, TimeUnit.SECONDS);

        // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
        boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
    }

    @Test
    public void lockAsync() {
        RLock lock = redisson.getLock("anyLock");
        lock.lockAsync();
        lock.lockAsync(10, TimeUnit.SECONDS);
        Future<Boolean> res = lock.tryLockAsync(100, 10, TimeUnit.SECONDS);
    }

}
