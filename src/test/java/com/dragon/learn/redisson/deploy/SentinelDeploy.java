package com.dragon.learn.redisson.deploy;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class SentinelDeploy {

    public static void main(String[] args) {

        Config config = new Config();
        //指定使用哨兵部署方式
        config.useSentinelServers()
                //设置sentinel.conf配置里的sentinel别名
                //比如sentinel.conf里配置为sentinel monitor my-sentinel-name 127.0.0.1 6379 2,那么这里就配置my-sentinel-name
                .setMasterName("my-sentinel-name")
                //这里设置sentinel节点的服务IP和端口，sentinel是采用Paxos拜占庭协议，一般sentinel至少3个节点
                //记住这里不是配置redis节点的服务端口和IP，sentinel会自己把请求转发给后面monitor的redis节点
                .addSentinelAddress("redis://127.0.0.1:26379")
                .addSentinelAddress("redis://127.0.0.1:26389")
                .addSentinelAddress("redis://127.0.0.1:26399");


        //创建客户端(发现创建RedissonClient非常耗时，基本在2秒-4秒左右)
        RedissonClient redisson = Redisson.create(config);

        //最后关闭RedissonClient
        redisson.shutdown();

    }
}
