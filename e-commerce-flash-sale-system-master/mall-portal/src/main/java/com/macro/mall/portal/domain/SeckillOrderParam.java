package com.macro.mall.portal.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 秒杀订单参数
 */
@Data
public class SeckillOrderParam {
    @ApiModelProperty("秒杀商品ID")
    private Long goodsId;

    @ApiModelProperty("收货地址ID")
    private Long memberReceiveAddressId;

    @ApiModelProperty("支付方式：1->支付宝；2->微信")
    private Integer payType;
}