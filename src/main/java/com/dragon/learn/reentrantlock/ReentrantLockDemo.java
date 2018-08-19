package com.dragon.learn.reentrantlock;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class ReentrantLockDemo {




    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redisson = Redisson.create(config);

        RLock lock = redisson.getLock("anyLock");
        // 最常见的使用方法
        lock.lock();

        lock.unlock();
    }
}
