package com.dragon.learn.reentrantlock.setnxandexpire;

import redis.clients.jedis.Jedis;

import java.util.Collections;

/**
 * 用途描述  *
 *
 * @author 王金龙
 * @version $Id: SetNXAndExpireLock, v0.1
 * @company 杭州信牛网络科技有限公司
 * @date 2018年04月27日 下午5:30 王金龙 Exp $
 */
public class SetNxAndExpireLockUtil {

    public static boolean wrongGetLock2(String lockKey, int expireTime) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            long expires = System.currentTimeMillis() + expireTime;
            String expiresStr = String.valueOf(expires);

            // 如果当前锁不存在，返回加锁成功
            if (jedis.setnx(lockKey, expiresStr) == 1) {
                return true;
            }

            // 如果锁存在，获取锁的过期时间
            String currentValueStr = jedis.get(lockKey);
            if (currentValueStr != null &&
                    Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
                // 锁已过期，获取上一个锁的过期时间，并设置现在锁的过期时间
                String oldValueStr = jedis.getSet(lockKey, expiresStr);
                if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                    // 考虑多线程并发的情况，只有一个线程的设置值和当前值相同，它才有权利加锁
                    return true;
                }
            }
        }
        // 其他情况，一律返回加锁失败
        return false;

    }

    /**
     * 通过setnx与expire实现获取锁
     *
     * @param key    获取锁的键
     * @param expire 过期时间，秒
     * @return true 获取锁成功，获取锁失败
     */
    public static boolean lockAtomic(String key, long expire) {
        String code;
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            code = jedis.set(key, "1", "NX", "EX", expire);
        }
        return "OK".equalsIgnoreCase(code);
    }

    /**
     * 通过set实现获取锁
     *
     * @param key    获取锁的键
     * @param value  获取锁的值
     * @param expire 过期时间，秒
     * @return true 获取锁成功，获取锁失败
     */
    public static boolean lockAtomic(String key, String value, long expire) {
        String code;
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            code = jedis.set(key, value, "NX", "EX", expire);
        }
        System.out.println(code);
        return "OK".equalsIgnoreCase(code);
    }


    /**
     * 通过setnx与expire实现获取锁
     *
     * @param key    获取锁的键
     * @param expire 过期时间，秒
     * @return true 获取锁成功，获取锁失败
     */
    public static boolean wrongGetLock1(String key, int expire) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            long status = jedis.setnx(key, "1");
            if (status == 1) {
                jedis.expire(key, expire);
                return true;
            }
        }
        return false;
    }

    /**
     * 删除指定键的缓存
     *
     * @param key 键
     */
    public static void unLock(String key) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.del(key);
        }
    }

    /**
     * 通过LUA原子性删除指定键和值的对象
     *
     * @param key   键
     * @param value 值
     */
    public static long unLockWithLua(String key, String value) {
        Jedis jedis = new Jedis("localhost", 6379);
        long result = (long) jedis.eval("if redis.call(\"get\",KEYS[1]) == ARGV[1] then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end", Collections.singletonList(key), Collections.singletonList(value));
        if (result == 0) {
            System.out.println("删除键为" + key + " 值为" + value + "缓存失败");
        } else {
            System.out.println("删除键为" + key + " 值为" + value + "缓存成功");
        }
        return result;
    }


    /**
     * 通过setnx与expire实现获取锁，但在设置过期时间时失败了。
     *
     * @param key    获取锁的键
     * @param expire 过期时间，秒
     * @return true 获取锁成功，获取锁失败
     */
    public static boolean lockWithExpireFail(String key, int expire) {
        long status;
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            status = jedis.setnx(key, "1");
        }
        if (status == 1) {
            System.out.println("在设置过期时间时因为某种原因失败了 无法执行 jedis.expire(key, expire)");
//            throw new RuntimeException("在设置过期时间时因为某种原因失败了");
//            jedis.expire(key, expire);
            return false;
        }

        return false;
    }

}
