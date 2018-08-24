package com.dragon.learn.redisson.deploy;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class ClusterDeploy {

    public static void main(String[] args) {

        Config config = new Config();
        //指定使用集群部署方式
        config.useClusterServers()
                // 集群状态扫描间隔时间，单位是毫秒
                .setScanInterval(2000)
                //cluster方式至少6个节点(3主3从，3主做sharding，3从用来保证主宕机后可以高可用)
                .addNodeAddress("redis://127.0.0.1:6379" )
                .addNodeAddress("redis://127.0.0.1:6380")
                .addNodeAddress("redis://127.0.0.1:6381")
                .addNodeAddress("redis://127.0.0.1:6382")
                .addNodeAddress("redis://127.0.0.1:6383")
                .addNodeAddress("redis://127.0.0.1:6384");

        //创建客户端(发现创建RedissonClient非常耗时，基本在2秒-4秒左右)
        RedissonClient redisson = Redisson.create(config);

        //最后关闭RedissonClient
        redisson.shutdown();
    }
}
