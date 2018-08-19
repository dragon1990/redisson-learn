package com.dragon.learn.redisson;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class ReplicationDeploy {
    public static void main(String[] args) {
        Config config = new Config();

        config.useReplicatedServers()
                // 主节点变化扫描间隔时间
                .setScanInterval(2000)
                //设置云服务商的redis服务IP和端口，目前支持亚马逊云的AWS ElastiCache和微软云的Azure Redis 缓存
                .addNodeAddress("redis://123.57.221.104.1:6379")
                .addNodeAddress("redis://123.57.221.105:6380")
                .addNodeAddress("redis://123.57.221.106:6382");

        //创建客户端(发现创建RedissonClient非常耗时，基本在2秒-4秒左右)
        RedissonClient redisson = Redisson.create(config);

        //最后关闭RedissonClient
        redisson.shutdown();
    }
}
