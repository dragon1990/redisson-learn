package com.dragon.learn.reentrantlock;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

public class ReentrantLockDemo {


    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setDatabase(0);
        RedissonClient redissonClient = Redisson.create(config);

        RLock lock = redissonClient.getLock("anyLock");
        if (!lock.tryLock()) {
            return;
        }
        try {
            // 业务处理
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }
}
