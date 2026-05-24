package com.macro.mall.portal.controller;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.SmsFlashPromotion;
import com.macro.mall.model.SmsFlashPromotionSession;
import com.macro.mall.portal.domain.FlashPromotionProduct;
import com.macro.mall.portal.domain.SeckillOrderParam;
import com.macro.mall.portal.service.SeckillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 秒杀Controller
 */
@Controller
@Api(tags = "SeckillController")
@Tag(name = "SeckillController", description = "秒杀管理")
@RequestMapping("/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @ApiOperation("获取当前秒杀活动")
    @RequestMapping(value = "/promotion", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<SmsFlashPromotion> getCurrentFlashPromotion() {
        SmsFlashPromotion promotion = seckillService.getCurrentFlashPromotion();
        return CommonResult.success(promotion);
    }

    @ApiOperation("获取秒杀场次列表")
    @RequestMapping(value = "/session/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<SmsFlashPromotionSession>> getFlashPromotionSessionList() {
        List<SmsFlashPromotionSession> list = seckillService.getFlashPromotionSessionList();
        return CommonResult.success(list);
    }

    @ApiOperation("获取秒杀商品列表")
    @RequestMapping(value = "/goods/list", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<List<FlashPromotionProduct>> getSeckillProductList(
            @RequestParam(required = false) Long sessionId) {
        List<FlashPromotionProduct> list;
        if (sessionId != null) {
            // 根据场次ID获取商品（秒杀专区用）
            list = seckillService.getFlashPromotionProducts(sessionId);
        } else {
            // 获取当前进行中场次的商品（首页用）
            list = seckillService.getSeckillProductList();
        }
        return CommonResult.success(list);
    }

    @ApiOperation("获取秒杀商品详情")
    @RequestMapping(value = "/goods/{id}", method = RequestMethod.GET)
    @ResponseBody
    public CommonResult<FlashPromotionProduct> getSeckillProduct(@PathVariable Long id) {
        FlashPromotionProduct product = seckillService.getSeckillProduct(id);
        if (product == null) {
            return CommonResult.failed("商品不存在");
        }
        return CommonResult.success(product);
    }

    @ApiOperation("执行秒杀（简化版，仅扣库存）")
    @RequestMapping(value = "/do", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult doSeckill(@RequestParam Long goodsId) {
        boolean success = seckillService.doSeckill(goodsId);
        if (success) {
            return CommonResult.success(null, "秒杀成功");
        }
        return CommonResult.failed("秒杀失败，库存不足或已购买");
    }

    // ============ 新增接口：秒杀下单 ============
    @ApiOperation("执行秒杀并生成订单")
    @RequestMapping(value = "/order", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult<OmsOrder> seckill(@RequestBody SeckillOrderParam param) {
        try {
            OmsOrder order = seckillService.seckill(param);
            return CommonResult.success(order, "秒杀成功");
        } catch (RuntimeException e) {
            return CommonResult.failed(e.getMessage());
        }
    }
}