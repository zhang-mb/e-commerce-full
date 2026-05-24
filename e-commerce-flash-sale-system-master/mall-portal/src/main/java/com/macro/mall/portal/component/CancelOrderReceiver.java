package com.macro.mall.portal.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 取消订单消息的接收者（简化版，不依赖 RabbitMQ）
 */
@Component
public class CancelOrderReceiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderReceiver.class);

    /**
     * 简化实现：不监听 RabbitMQ 队列
     * 订单取消功能通过其他方式实现
     */
    public CancelOrderReceiver() {
        LOGGER.info("CancelOrderReceiver - 使用简化配置，不依赖 RabbitMQ");
    }
}
