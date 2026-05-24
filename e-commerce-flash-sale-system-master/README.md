# 电商秒杀系统
## 项目介绍
基于SpringBoot+Redis+RabbitMQ实现的高并发秒杀系统，支撑大促期间2000+ QPS的流量，解决了库存超卖、流量击穿、重复下单等核心问题。

## 技术栈
- 后端：SpringBoot、MyBatis-Plus、Redis、RabbitMQ、Redisson
- 中间件：Nginx、MySQL
- 工具：Guava、Lua脚本

## 核心功能
1.  商品缓存预热：启动时将热点商品同步到Redis，降低DB查询压力
2.  多级限流策略：Nginx层限流+Guava接口层限流，防止流量击穿
3.  库存原子扣减：Redis+Lua脚本实现库存预扣，超卖率降为0
4.  异步订单生成：RabbitMQ异步处理订单，接口响应时间从300ms→50ms
5.  分布式锁防重：Redisson实现分布式锁，防止MQ消息重复消费

## 技术亮点
1.  用Redis+Lua保证库存扣减原子性，解决高并发下的超卖问题
2.  多级限流+异步解耦，使接口QPS从500提升至2000+
3.  定时任务+消息重试，保证Redis与DB库存最终一致

## 问题与优化
- 初始版本直接查MySQL，热点接口响应300ms+ → 优化为Redis缓存后降至50ms以内
- 秒杀时出现库存超卖 → 引入Redis+Lua脚本解决
- 同步生成订单导致接口超时 → 改为RabbitMQ异步生成订单
