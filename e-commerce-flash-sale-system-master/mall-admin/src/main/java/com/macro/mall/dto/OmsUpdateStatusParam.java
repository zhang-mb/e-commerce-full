package com.macro.mall.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 确认收货请求参数

 */
@Getter
@Setter
public class OmsUpdateStatusParam {
    @ApiModelProperty("服务单号")
    private Long id;
    @ApiModelProperty("收货地址关联id")
    private Long companyAddressId;
    @ApiModelProperty("确认退款金�?)
    private BigDecimal returnAmount;
    @ApiModelProperty("处理备注")
    private String handleNote;
    @ApiModelProperty("处理�?)
    private String handleMan;
    @ApiModelProperty("收货备注")
    private String receiveNote;
    @ApiModelProperty("收货�?)
    private String receiveMan;
    @ApiModelProperty("申请状态：1->退货中�?->已完成；3->已拒�?)
    private Integer status;
}
