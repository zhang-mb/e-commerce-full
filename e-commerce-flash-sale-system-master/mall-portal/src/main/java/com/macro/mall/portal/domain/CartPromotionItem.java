package com.macro.mall.portal.domain;

import com.macro.mall.model.OmsCartItem;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 带促销信息的购物车商品封装

 */
@Getter
@Setter
public class CartPromotionItem extends OmsCartItem{
    @ApiModelProperty("促销活动信息")
    private String promotionMessage;
    @ApiModelProperty("促销活动减去的金额，针对每个商品")
    private BigDecimal reduceAmount;
    @ApiModelProperty("剩余库存-锁定库存")
    private Integer realStock;
    @ApiModelProperty("购买商品赠送积�?)
    private Integer integration;
    @ApiModelProperty("购买商品赠送成长�?)
    private Integer growth;
}
