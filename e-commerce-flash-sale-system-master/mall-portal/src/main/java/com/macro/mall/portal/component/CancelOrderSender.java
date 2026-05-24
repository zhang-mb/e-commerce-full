package com.macro.mall.portal.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 取消订单消息的发送者（简化版，不依赖 RabbitMQ）
 */
@Component
public class CancelOrderSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CancelOrderSender.class);

    /**
     * 发送取消订单消息
     * 简化版：不依赖 RabbitMQ，直接记录日志
     */
    public void sendMessage(Long orderId, final long delayTimes) {
        // 简化实现：不发送消息到队列，只记录日志
        LOGGER.info("CancelOrderSender - 订单取消消息已记录（简化版，不依赖RabbitMQ）, orderId:{}, delayTimes:{}", orderId, delayTimes);
    }
}
