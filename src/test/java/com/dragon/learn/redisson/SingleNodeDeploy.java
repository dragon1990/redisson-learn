package com.dragon.learn.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class SingleNodeDeploy {

    public static void main(String[] args) {
        //创建配置
        Config config = new Config();

        //指定使用单节点部署方式
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");

        //创建客户端(发现创建RedissonClient非常耗时，基本在2秒-4秒左右)
        RedissonClient redisson = Redisson.create(config);

        //最后关闭RedissonClient
        redisson.shutdown();

    }
}
