package com.macro.mall.portal.service;

import com.macro.mall.model.SmsCoupon;
import com.macro.mall.model.SmsCouponHistory;
import com.macro.mall.portal.domain.CartPromotionItem;
import com.macro.mall.portal.domain.SmsCouponHistoryDetail;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户优惠券管理Service

 */
public interface UmsMemberCouponService {
    /**
     * 会员添加优惠�?     */
    @Transactional
    void add(Long couponId);

    /**
     * 获取优惠券历史列�?     */
    List<SmsCouponHistory> listHistory(Integer useStatus);

    /**
     * 根据购物车信息获取可用优惠券
     */
    List<SmsCouponHistoryDetail> listCart(List<CartPromotionItem> cartItemList, Integer type);

    /**
     * 获取当前商品相关优惠�?     */
    List<SmsCoupon> listByProduct(Long productId);

    /**
     * 获取用户优惠券列�?     */
    List<SmsCoupon> list(Integer useStatus);
}
