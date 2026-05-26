电商秒杀系统
项目介绍
基于 SpringBoot+Redis+RabbitMQ 实现的前后端分离高并发秒杀系统，支撑大促期间 20000+ QPS 的流量，解决了库存超卖、流量击穿、重复下单、接口超时等核心问题。前端采用 Vue3 开发，完成商品展示、秒杀抢购、个人订单等业务页面。
技术栈
前端
Vue3、Vite、Axios、Vue Router
后端
SpringBoot 2.7.15、MyBatis-Plus、Redis、RabbitMQ、Redisson
中间件
Nginx、MySQL8.0
工具
Guava、Lua 脚本
核心功能
1. 商品缓存预热：项目启动时将热点商品同步到 Redis，降低 DB 查询压力
2. 多级限流策略：Nginx 层限流 + Guava 接口层限流，防止流量击穿数据库
3. 库存原子扣减：Redis+Lua 脚本实现库存预扣，超卖率降为 0
4. 异步订单生成：RabbitMQ 异步处理订单，接口响应时间从 300ms 降至 50ms 内
5. 分布式锁防重：Redisson 实现分布式锁，防止 MQ 消息重复消费，避免重复下单
6. 用户权限管理：SpringSecurity+JWT 完成登录认证、接口权限管控
7. 商品检索：Elasticsearch 实现商品关键词搜索
技术亮点
1. 采用 Redis+Lua 脚本保证库存扣减原子性，彻底解决高并发下的超卖问题
2. 多级限流 + 异步解耦架构，接口 QPS 从 500 提升至 20000+
3. 定时任务 + 消息重试机制，保证 Redis 缓存与 MySQL 数据库库存最终一致性
4. 前端接口防抖 + 后端多层拦截，杜绝无效请求挤占服务资源
5. 使用 Redisson 分布式锁，解决集群环境下订单重复提交问题
问题与优化
1. 初始版本直接查询 MySQL，热点接口响应 300ms+ → 优化为 Redis 缓存后降至 50ms 以内
2. 秒杀场景出现库存超卖 → 引入 Redis+Lua 脚本原子扣减库存解决
3. 同步生成订单导致接口超时、吞吐量低下 → 改为 RabbitMQ 异步生成订单
4. 瞬时大量请求击穿数据库 → 前端限流 + Nginx 限流 + 接口本地限流三层防护
5.MQ 重复消费造成重复订单 → Redisson 分布式锁实现幂等处理
本地部署运行
后端
1. 导入后端项目至 IDEA，配置 MySQL、Redis、RabbitMQ 连接信息
2. 执行 SQL 脚本初始化数据表
3. 运行项目主类，服务端口 8080
前端
plaintext
cd flash-sale-frontend
npm install
npm run dev
访问地址：http://localhost:5173
