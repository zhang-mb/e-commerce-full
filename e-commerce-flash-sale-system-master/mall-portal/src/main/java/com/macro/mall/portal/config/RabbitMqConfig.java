package com.macro.mall.portal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * 消息队列相关配置（简化版，不依赖 RabbitMQ）
 */
@Configuration
public class RabbitMqConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqConfig.class);

    /**
     * 简化配置：不创建 RabbitMQ 队列，避免启动时需要 RabbitMQ
     * 订单取消功能改为简化实现
     */
    public RabbitMqConfig() {
        LOGGER.info("RabbitMqConfig - 使用简化配置，不依赖 RabbitMQ");
    }
}
