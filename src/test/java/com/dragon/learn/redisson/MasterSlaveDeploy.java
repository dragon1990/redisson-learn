package com.dragon.learn.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class MasterSlaveDeploy {

    public static void main(String[] args) {

        Config config = new Config();
        //指定使用主从部署方式
        config.useMasterSlaveServers()
                //设置redis主节点
                .setMasterAddress("redis://127.0.0.1:6379")
                //设置redis从节点
                .addSlaveAddress("redis://127.0.0.1:6380", "redis://127.0.0.1:6381");

        //创建客户端(发现创建RedissonClient非常耗时，基本在2秒-4秒左右)
        RedissonClient redisson = Redisson.create(config);

        //最后关闭RedissonClient
        redisson.shutdown();

    }
}
