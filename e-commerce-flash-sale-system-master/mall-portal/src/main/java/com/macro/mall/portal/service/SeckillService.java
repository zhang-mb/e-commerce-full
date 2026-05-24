package com.macro.mall.portal.service;

import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.SmsFlashPromotion;
import com.macro.mall.model.SmsFlashPromotionSession;
import com.macro.mall.portal.domain.FlashPromotionProduct;
import com.macro.mall.portal.domain.SeckillOrderParam;
import java.util.List;

/**
 * 秒杀服务接口
 */
public interface SeckillService {

    /**
     * 获取当前秒杀活动
     */
    SmsFlashPromotion getCurrentFlashPromotion();

    /**
     * 获取秒杀场次列表
     */
    List<SmsFlashPromotionSession> getFlashPromotionSessionList();

    /**
     * 获取秒杀商品列表
     */
    List<FlashPromotionProduct> getSeckillProductList();

    /**
     * 获取秒杀商品详情
     */
    FlashPromotionProduct getSeckillProduct(Long id);

    /**
     * 获取指定场次的秒杀商品
     */
    List<FlashPromotionProduct> getFlashPromotionProducts(Long sessionId);

    /**
     * 执行秒杀（简化版）
     */
    boolean doSeckill(Long goodsId);

    /**
     * 执行秒杀并生成订单（新增方法）
     */
    OmsOrder seckill(SeckillOrderParam param);
}