package com.macro.mall.portal.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.github.pagehelper.PageHelper;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.common.service.RedisService;
import com.macro.mall.mapper.*;
import com.macro.mall.model.*;
import com.macro.mall.portal.component.CancelOrderSender;
import com.macro.mall.portal.dao.PortalOrderDao;
import com.macro.mall.portal.dao.PortalOrderItemDao;
import com.macro.mall.portal.dao.SmsCouponHistoryDao;
import com.macro.mall.portal.domain.*;
import com.macro.mall.portal.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 前台订单管理Service

 */
@Slf4j
@Service
public class OmsPortalOrderServiceImpl implements OmsPortalOrderService {
    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private OmsCartItemService cartItemService;
    @Autowired
    private UmsMemberReceiveAddressService memberReceiveAddressService;
    @Autowired
    private UmsMemberCouponService memberCouponService;
    @Autowired
    private UmsIntegrationConsumeSettingMapper integrationConsumeSettingMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;
    @Autowired
    private SmsCouponHistoryDao couponHistoryDao;
    @Autowired
    private OmsOrderMapper orderMapper;
    @Autowired
    private PortalOrderItemDao orderItemDao;
    @Autowired
    private SmsCouponHistoryMapper couponHistoryMapper;
    @Autowired
    private RedisService redisService;
    @Value("${redis.key.orderId}")
    private String REDIS_KEY_ORDER_ID;
    @Value("${redis.database}")
    private String REDIS_DATABASE;
    @Autowired
    private PortalOrderDao portalOrderDao;
    @Autowired
    private OmsOrderSettingMapper orderSettingMapper;
    @Autowired
    private OmsOrderItemMapper orderItemMapper;
    @Autowired
    private CancelOrderSender cancelOrderSender;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public ConfirmOrderResult generateConfirmOrder(List<Long> cartIds) {
        ConfirmOrderResult result = new ConfirmOrderResult();
        //获取购物车信�?        UmsMember currentMember = memberService.getCurrentMember();
        if(currentMember == null) return result;
        List<CartPromotionItem> cartPromotionItemList = cartItemService.listPromotion(currentMember.getId(),cartIds);
        result.setCartPromotionItemList(cartPromotionItemList);
        //获取用户收货地址列表
        List<UmsMemberReceiveAddress> memberReceiveAddressList = memberReceiveAddressService.list();
        result.setMemberReceiveAddressList(memberReceiveAddressList);
        //获取用户可用优惠券列�?        List<SmsCouponHistoryDetail> couponHistoryDetailList = memberCouponService.listCart(cartPromotionItemList, 1);
        result.setCouponHistoryDetailList(couponHistoryDetailList);
        //获取用户积分
        result.setMemberIntegration(currentMember.getIntegration());
        //获取积分使用规则
        UmsIntegrationConsumeSetting integrationConsumeSetting = integrationConsumeSettingMapper.selectByPrimaryKey(1L);
        result.setIntegrationConsumeSetting(integrationConsumeSetting);
        //计算总金额、活动优惠、应付金�?        ConfirmOrderResult.CalcAmount calcAmount = calcCartAmount(cartPromotionItemList);
        result.setCalcAmount(calcAmount);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> generateOrder(OrderParam orderParam) {
        List<OmsOrderItem> orderItemList = new ArrayList<>();
        //校验收货地址
        if(orderParam.getMemberReceiveAddressId()==null){
            Asserts.fail("请选择收货地址�?);
        }
        //获取购物车及优惠信息
        UmsMember currentMember = memberService.getCurrentMember();
        if(currentMember == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("message", "用户未登�?);
            return result;
        }
        List<CartPromotionItem> cartPromotionItemList = cartItemService.listPromotion(currentMember.getId(), orderParam.getCartIds());
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            //生成下单商品信息
            OmsOrderItem orderItem = new OmsOrderItem();
            orderItem.setProductId(cartPromotionItem.getProductId());
            orderItem.setProductName(cartPromotionItem.getProductName());
            orderItem.setProductPic(cartPromotionItem.getProductPic());
            orderItem.setProductAttr(cartPromotionItem.getProductAttr());
            orderItem.setProductBrand(cartPromotionItem.getProductBrand());
            orderItem.setProductSn(cartPromotionItem.getProductSn());
            orderItem.setProductPrice(cartPromotionItem.getPrice());
            orderItem.setProductQuantity(cartPromotionItem.getQuantity());
            orderItem.setProductSkuId(cartPromotionItem.getProductSkuId());
            orderItem.setProductSkuCode(cartPromotionItem.getProductSkuCode());
            orderItem.setProductCategoryId(cartPromotionItem.getProductCategoryId());
            orderItem.setPromotionAmount(cartPromotionItem.getReduceAmount());
            orderItem.setPromotionName(cartPromotionItem.getPromotionMessage());
            orderItem.setGiftIntegration(cartPromotionItem.getIntegration());
            orderItem.setGiftGrowth(cartPromotionItem.getGrowth());
            orderItemList.add(orderItem);
        }
        //判断购物车中商品是否都有库存
        if (!hasStock(cartPromotionItemList)) {
            Asserts.fail("库存不足，无法下�?);
        }
        //判断使用使用了优惠券
        if (orderParam.getCouponId() == null) {
            //不用优惠�?            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setCouponAmount(new BigDecimal(0));
            }
        } else {
            //使用优惠�?            SmsCouponHistoryDetail couponHistoryDetail = getUseCoupon(cartPromotionItemList, orderParam.getCouponId());
            if (couponHistoryDetail == null) {
                Asserts.fail("该优惠券不可�?);
            }
            //对下单商品的优惠券进行处�?            handleCouponAmount(orderItemList, couponHistoryDetail);
        }
        //判断是否使用积分
        if (orderParam.getUseIntegration() == null||orderParam.getUseIntegration().equals(0)) {
            //不使用积�?            for (OmsOrderItem orderItem : orderItemList) {
                orderItem.setIntegrationAmount(new BigDecimal(0));
            }
        } else {
            //使用积分
            BigDecimal totalAmount = calcTotalAmount(orderItemList);
            BigDecimal integrationAmount = getUseIntegrationAmount(orderParam.getUseIntegration(), totalAmount, currentMember, orderParam.getCouponId() != null);
            if (integrationAmount.compareTo(new BigDecimal(0)) == 0) {
                Asserts.fail("积分不可�?);
            } else {
                //可用情况下分摊到可用商品�?                for (OmsOrderItem orderItem : orderItemList) {
                    BigDecimal perAmount = orderItem.getProductPrice()
                            .multiply(new BigDecimal(orderItem.getProductQuantity()))
                            .divide(totalAmount, 3, RoundingMode.HALF_EVEN)
                            .multiply(integrationAmount);
                    orderItem.setIntegrationAmount(perAmount);
                }
            }
        }
        //计算order_item的实付金�?        handleRealAmount(orderItemList);
        //进行库存锁定
        lockStock(cartPromotionItemList);
        //根据商品合计、运费、活动优惠、优惠券、积分计算应付金�?        OmsOrder order = new OmsOrder();
        order.setDiscountAmount(new BigDecimal(0));
        order.setTotalAmount(calcTotalAmount(orderItemList));
        order.setFreightAmount(new BigDecimal(0));
        order.setPromotionAmount(calcPromotionAmount(orderItemList));
        order.setPromotionInfo(getOrderPromotionInfo(orderItemList));
        if (orderParam.getCouponId() == null) {
            order.setCouponAmount(new BigDecimal(0));
        } else {
            order.setCouponId(orderParam.getCouponId());
            order.setCouponAmount(calcCouponAmount(orderItemList));
        }
        if (orderParam.getUseIntegration() == null) {
            order.setUseIntegration(0);
            order.setIntegration(0);
            order.setIntegrationAmount(new BigDecimal(0));
        } else {
            order.setUseIntegration(orderParam.getUseIntegration());
            order.setIntegration(calcGifIntegration(orderItemList));
            order.setIntegrationAmount(calcIntegrationAmount(orderItemList));
        }
        order.setPayAmount(calcPayAmount(order));
        //转化为订单信息并插入数据�?        order.setMemberId(currentMember.getId());
        order.setCreateTime(new Date());
        order.setMemberUsername(currentMember.getUsername());
        //支付方式�?->未支付；1->支付宝；2->微信
        order.setPayType(orderParam.getPayType());
        //订单来源�?->PC订单�?->app订单
        order.setSourceType(1);
        //订单状态：0->待付款；1->待发货；2->已发货；3->已完成；4->已关闭；5->无效订单
        order.setStatus(0);
        //订单类型�?->正常订单�?->秒杀订单
        order.setOrderType(0);
        //收货人信息：姓名、电话、邮编、地址
        UmsMemberReceiveAddress address = memberReceiveAddressService.getItem(orderParam.getMemberReceiveAddressId());
        order.setReceiverName(address.getName());
        order.setReceiverPhone(address.getPhoneNumber());
        order.setReceiverPostCode(address.getPostCode());
        order.setReceiverProvince(address.getProvince());
        order.setReceiverCity(address.getCity());
        order.setReceiverRegion(address.getRegion());
        order.setReceiverDetailAddress(address.getDetailAddress());
        //0->未确认；1->已确�?        order.setConfirmStatus(0);
        order.setDeleteStatus(0);
        //计算赠送积�?        order.setIntegration(calcGifIntegration(orderItemList));
        //计算赠送成长�?        order.setGrowth(calcGiftGrowth(orderItemList));
        //生成订单�?        order.setOrderSn(generateOrderSn(order));
        //设置自动收货天数
        List<OmsOrderSetting> orderSettings = orderSettingMapper.selectByExample(new OmsOrderSettingExample());
        if(CollUtil.isNotEmpty(orderSettings)){
            order.setAutoConfirmDay(orderSettings.get(0).getConfirmOvertime());
        }
        // TODO: 2018/9/3 bill_*,delivery_*
        //插入order表和order_item�?        orderMapper.insert(order);
        for (OmsOrderItem orderItem : orderItemList) {
            orderItem.setOrderId(order.getId());
            orderItem.setOrderSn(order.getOrderSn());
        }
        orderItemDao.insertList(orderItemList);
        //如使用优惠券更新优惠券使用状�?        if (orderParam.getCouponId() != null) {
            updateCouponStatus(orderParam.getCouponId(), currentMember.getId(), 1);
        }
        //如使用积分需要扣除积�?        if (orderParam.getUseIntegration() != null) {
            order.setUseIntegration(orderParam.getUseIntegration());
            if(currentMember.getIntegration()==null){
                currentMember.setIntegration(0);
            }
            memberService.updateIntegration(currentMember.getId(), currentMember.getIntegration() - orderParam.getUseIntegration());
        }
        //删除购物车中的下单商�?        deleteCartItemList(cartPromotionItemList, currentMember);
        //发送延迟消息取消订�?        sendDelayMessageCancelOrder(order.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("orderItemList", orderItemList);
        return result;
    }

    // 支付相关Redis Key前缀
    private static final String PAY_LOCK_KEY_PREFIX = "order:pay:lock:";
    private static final String PAY_SUCCESS_KEY_PREFIX = "order:pay:success:";
    private static final long PAY_LOCK_EXPIRE_TIME = 30000; // 支付锁超时时�?0�?
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Integer paySuccess(Long orderId, Integer payType) {
        String lockKey = PAY_LOCK_KEY_PREFIX + orderId;
        String successKey = PAY_SUCCESS_KEY_PREFIX + orderId;
        
        // 1. 检查是否已支付成功（幂等性检查）
        if (Boolean.TRUE.equals(redisTemplate.hasKey(successKey))) {
            log.warn("订单已支付成功，拒绝重复支付，orderId={}", orderId);
            Asserts.fail("订单已支付成功！");
        }
        
        // 2. 获取分布式锁，防止并发支�?        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", PAY_LOCK_EXPIRE_TIME, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!Boolean.TRUE.equals(lockAcquired)) {
            log.warn("订单正在支付中，请稍后重试，orderId={}", orderId);
            Asserts.fail("订单正在支付中，请稍后重试！");
        }
        
        try {
            // 3. 再次检查是否已支付（双重检查）
            if (Boolean.TRUE.equals(redisTemplate.hasKey(successKey))) {
                log.warn("订单已支付成功（二次检查），orderId={}", orderId);
                Asserts.fail("订单已支付成功！");
            }
            
            // 4. 查询订单，确认订单状�?            OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
            if (order == null) {
                log.error("订单不存在，orderId={}", orderId);
                Asserts.fail("订单不存在！");
            }
            
            if (order.getDeleteStatus() != 0) {
                log.error("订单已删除，orderId={}", orderId);
                Asserts.fail("订单已删除！");
            }
            
            if (order.getStatus() != 0) {
                log.warn("订单状态不是待支付，无法支付，orderId={}, status={}", orderId, order.getStatus());
                Asserts.fail("订单状态不是待支付�?);
            }
            
            // 5. 修改订单支付状态（使用乐观锁）
            OmsOrder updateOrder = new OmsOrder();
            updateOrder.setId(orderId);
            updateOrder.setStatus(1);
            updateOrder.setPaymentTime(new Date());
            updateOrder.setPayType(payType);
            
            OmsOrderExample orderExample = new OmsOrderExample();
            orderExample.createCriteria()
                    .andIdEqualTo(orderId)
                    .andDeleteStatusEqualTo(0)
                    .andStatusEqualTo(0);
            
            int updateCount = orderMapper.updateByExampleSelective(updateOrder, orderExample);
            if (updateCount == 0) {
                log.error("订单状态更新失败，可能已被其他线程处理，orderId={}", orderId);
                Asserts.fail("订单支付失败，请重试�?);
            }
            
            // 6. 标记支付成功（幂等性标记）
            redisTemplate.opsForValue().set(successKey, "1", 24, java.util.concurrent.TimeUnit.HOURS);
            
            // 7. 扣减库存
            int totalCount = reduceStockAfterPay(orderId, order.getOrderType());
            
            log.info("订单支付成功，orderId={}, payType={}", orderId, payType);
            return totalCount;
            
        } finally {
            // 释放�?            redisTemplate.delete(lockKey);
        }
    }
    
    /**
     * 支付成功后扣减库�?     */
    private int reduceStockAfterPay(Long orderId, Integer orderType) {
        OmsOrderDetail orderDetail = portalOrderDao.getDetail(orderId);
        int totalCount = 0;
        
        for (OmsOrderItem orderItem : orderDetail.getOrderItemList()) {
            if (orderType != null && orderType == 1) {
                // 秒杀订单：扣减Redis库存
                String stockKey = "seckill:stock:" + orderItem.getProductId();
                Long remainingStock = redisTemplate.opsForValue().decrement(stockKey);
                if (remainingStock == null || remainingStock < 0) {
                    // 恢复库存
                    redisTemplate.opsForValue().increment(stockKey);
                    log.error("秒杀商品库存不足，orderId={}, productId={}", orderId, orderItem.getProductId());
                    Asserts.fail("库存不足，无法扣减！");
                }
                log.info("秒杀订单扣减Redis库存成功，orderId={}, productId={}, remainingStock={}", 
                        orderId, orderItem.getProductId(), remainingStock);
                totalCount++;
            } else {
                // 普通订单：扣减数据库库�?                int count = portalOrderDao.reduceSkuStock(orderItem.getProductSkuId(), orderItem.getProductQuantity());
                if (count == 0) {
                    log.error("普通订单库存扣减失败，orderId={}, skuId={}", orderId, orderItem.getProductSkuId());
                    Asserts.fail("库存不足，无法扣减！");
                }
                totalCount += count;
            }
        }
        return totalCount;
    }

    @Override
    public Integer cancelTimeOutOrder() {
        Integer count=0;
        OmsOrderSetting orderSetting = orderSettingMapper.selectByPrimaryKey(1L);
        //查询超时、未支付的订单及订单详情
        List<OmsOrderDetail> timeOutOrders = portalOrderDao.getTimeOutOrders(orderSetting.getNormalOrderOvertime());
        if (CollectionUtils.isEmpty(timeOutOrders)) {
            return count;
        }
        //修改订单状态为交易取消
        List<Long> ids = new ArrayList<>();
        for (OmsOrderDetail timeOutOrder : timeOutOrders) {
            ids.add(timeOutOrder.getId());
        }
        portalOrderDao.updateOrderStatus(ids, 4);
        for (OmsOrderDetail timeOutOrder : timeOutOrders) {
            //解除订单商品库存锁定
            portalOrderDao.releaseSkuStockLock(timeOutOrder.getOrderItemList());
            //修改优惠券使用状�?            updateCouponStatus(timeOutOrder.getCouponId(), timeOutOrder.getMemberId(), 0);
            //返还使用积分
            if (timeOutOrder.getUseIntegration() != null) {
                UmsMember member = memberService.getById(timeOutOrder.getMemberId());
                memberService.updateIntegration(timeOutOrder.getMemberId(), member.getIntegration() + timeOutOrder.getUseIntegration());
            }
        }
        return timeOutOrders.size();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelOrder(Long orderId) {
        // 查询订单
        OmsOrder cancelOrder = orderMapper.selectByPrimaryKey(orderId);
        if (cancelOrder == null) {
            log.warn("取消订单失败：订单不存在，orderId={}", orderId);
            return;
        }
        
        // 状态校验：只能取消未付款状态的订单（status=0�?        if (cancelOrder.getStatus() != 0) {
            log.warn("取消订单失败：订单状态不允许取消，orderId={}, status={}", orderId, cancelOrder.getStatus());
            Asserts.fail("订单状态不允许取消�?);
        }
        
        // 修改订单状态为取消
        cancelOrder.setStatus(4);
        orderMapper.updateByPrimaryKeySelective(cancelOrder);
        
        try {
            OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
            orderItemExample.createCriteria().andOrderIdEqualTo(orderId);
            List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(orderItemExample);
            
            // 判断是否为秒杀订单（orderType=1�?            boolean isSeckillOrder = cancelOrder.getOrderType() != null && cancelOrder.getOrderType() == 1;
            
            // 解除订单商品库存锁定
            if (!CollectionUtils.isEmpty(orderItemList)) {
                for (OmsOrderItem orderItem : orderItemList) {
                    if (isSeckillOrder) {
                        // 秒杀订单：释放Redis库存
                        String stockKey = "seckill:stock:" + orderItem.getProductId();
                        redisTemplate.opsForValue().increment(stockKey, orderItem.getProductQuantity());
                        log.info("秒杀订单取消，已恢复Redis库存，productId={}, quantity={}", orderItem.getProductId(), orderItem.getProductQuantity());
                        
                        // 移除已购买标�?                        String orderKey = "seckill:order:" + orderItem.getProductId() + ":" + cancelOrder.getMemberId();
                        redisTemplate.delete(orderKey);
                    } else {
                        // 普通订单：释放数据库库�?                        if (orderItem.getProductSkuId() == null) {
                            log.error("库存释放失败：productSkuId 为空，orderItemId={}, orderId={}", orderItem.getId(), orderId);
                            throw new RuntimeException("库存释放失败：商品SKU ID为空�?);
                        }
                        int count = portalOrderDao.releaseStockBySkuId(orderItem.getProductSkuId(), orderItem.getProductQuantity());
                        if(count == 0) {
                            log.error("库存释放失败：库存不足或锁定库存不足，productSkuId={}, quantity={}, orderId={}", orderItem.getProductSkuId(), orderItem.getProductQuantity(), orderId);
                            throw new RuntimeException("库存释放失败：锁定库存不足！");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("取消订单失败，库存操作异常，将回滚订单状态，orderId={}, error={}", orderId, e.getMessage());
            // 库存操作失败，回滚订单状态为待付�?            cancelOrder.setStatus(0);
            orderMapper.updateByPrimaryKeySelective(cancelOrder);
            throw e;
        }
        
        // 修改优惠券使用状�?        updateCouponStatus(cancelOrder.getCouponId(), cancelOrder.getMemberId(), 0);
        // 返还使用积分
        if (cancelOrder.getUseIntegration() != null) {
            UmsMember member = memberService.getById(cancelOrder.getMemberId());
            memberService.updateIntegration(cancelOrder.getMemberId(), member.getIntegration() + cancelOrder.getUseIntegration());
        }
    }

    @Override
    public void sendDelayMessageCancelOrder(Long orderId) {
        //获取订单超时时间
        OmsOrderSetting orderSetting = orderSettingMapper.selectByPrimaryKey(1L);
        long delayTimes = orderSetting.getNormalOrderOvertime() * 60 * 1000;
        //发送延迟消�?        cancelOrderSender.sendMessage(orderId, delayTimes);
    }

    @Override
    public void confirmReceiveOrder(Long orderId) {
        UmsMember member = memberService.getCurrentMember();
        if(member == null) return;
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if(!member.getId().equals(order.getMemberId())){
            Asserts.fail("不能确认他人订单�?);
        }
        if(order.getStatus()!=2){
            Asserts.fail("该订单还未发货！");
        }
        order.setStatus(3);
        order.setConfirmStatus(1);
        order.setReceiveTime(new Date());
        orderMapper.updateByPrimaryKey(order);
    }

    @Override
    public CommonPage<OmsOrderDetail> list(Integer status, Integer pageNum, Integer pageSize) {
        // 添加参数空值检�?        if(pageNum == null) pageNum = 1;
        if(pageSize == null) pageSize = 10;
        if(status != null && status == -1){
            status = null;
        }
        
        UmsMember member = memberService.getCurrentMember();
        
        // 检查用户是否有�?        if(member == null || member.getId() == null || member.getId() == 0) {
            CommonPage<OmsOrderDetail> emptyPage = new CommonPage<>();
            emptyPage.setList(new ArrayList<>());
            emptyPage.setTotal(0L);
            emptyPage.setPageNum(pageNum);
            emptyPage.setPageSize(pageSize);
            emptyPage.setTotalPage(0);
            return emptyPage;
        }
        
        try {
            PageHelper.startPage(pageNum, pageSize);
            OmsOrderExample orderExample = new OmsOrderExample();
            OmsOrderExample.Criteria criteria = orderExample.createCriteria();
            criteria.andDeleteStatusEqualTo(0)
                    .andMemberIdEqualTo(member.getId());
            if(status != null){
                criteria.andStatusEqualTo(status);
            }
            orderExample.setOrderByClause("create_time desc");
            List<OmsOrder> orderList = orderMapper.selectByExample(orderExample);
            
            // 检查查询结�?            if (orderList == null) {
                orderList = new ArrayList<>();
            }
            
            CommonPage<OmsOrder> orderPage = CommonPage.restPage(orderList);
            
            //设置分页信息
            CommonPage<OmsOrderDetail> resultPage = new CommonPage<>();
            resultPage.setPageNum(orderPage.getPageNum());
            resultPage.setPageSize(orderPage.getPageSize());
            resultPage.setTotal(orderPage.getTotal());
            resultPage.setTotalPage(orderPage.getTotalPage());
            
            if(CollUtil.isEmpty(orderList)){
                resultPage.setList(new ArrayList<>());
                return resultPage;
            }
            
            //设置数据信息
            List<Long> orderIds = orderList.stream()
                    .filter(order -> order.getId() != null)
                    .map(OmsOrder::getId)
                    .collect(Collectors.toList());
            
            List<OmsOrderItem> orderItemList = new ArrayList<>();
            if(!CollUtil.isEmpty(orderIds)){
                OmsOrderItemExample orderItemExample = new OmsOrderItemExample();
                orderItemExample.createCriteria().andOrderIdIn(orderIds);
                orderItemList = orderItemMapper.selectByExample(orderItemExample);
                if(orderItemList == null){
                    orderItemList = new ArrayList<>();
                }
            }
            
            List<OmsOrderDetail> orderDetailList = new ArrayList<>();
            for (OmsOrder omsOrder : orderList) {
                if(omsOrder == null) continue;
                
                OmsOrderDetail orderDetail = new OmsOrderDetail();
                BeanUtil.copyProperties(omsOrder, orderDetail);
                
                Long orderId = omsOrder.getId();
                List<OmsOrderItem> relatedItemList = orderItemList.stream()
                        .filter(item -> item != null && item.getOrderId() != null && item.getOrderId().equals(orderId))
                        .collect(Collectors.toList());
                orderDetail.setOrderItemList(relatedItemList);
                orderDetailList.add(orderDetail);
            }
            resultPage.setList(orderDetailList);
            return resultPage;
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            CommonPage<OmsOrderDetail> emptyPage = new CommonPage<>();
            emptyPage.setList(new ArrayList<>());
            emptyPage.setTotal(0L);
            emptyPage.setPageNum(pageNum);
            emptyPage.setPageSize(pageSize);
            emptyPage.setTotalPage(0);
            return emptyPage;
        }
    }

    @Override
    public OmsOrderDetail detail(Long orderId) {
        OmsOrder omsOrder = orderMapper.selectByPrimaryKey(orderId);
        OmsOrderItemExample example = new OmsOrderItemExample();
        example.createCriteria().andOrderIdEqualTo(orderId);
        List<OmsOrderItem> orderItemList = orderItemMapper.selectByExample(example);
        OmsOrderDetail orderDetail = new OmsOrderDetail();
        BeanUtil.copyProperties(omsOrder,orderDetail);
        orderDetail.setOrderItemList(orderItemList);
        return orderDetail;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteOrder(Long orderId) {
        UmsMember member = memberService.getCurrentMember();
        if(member == null) return;
        OmsOrder order = orderMapper.selectByPrimaryKey(orderId);
        if(!member.getId().equals(order.getMemberId())){
            Asserts.fail("不能删除他人订单�?);
        }
        // 添加 deleteStatus 判断
        if(order.getDeleteStatus() == 1){
            Asserts.fail("订单已删除！");
        }
        if(order.getStatus()==3||order.getStatus()==4){
            order.setDeleteStatus(1);
            orderMapper.updateByPrimaryKey(order);
        }else{
            Asserts.fail("只能删除已完成或已关闭的订单�?);
        }
    }

    @Override
    public void paySuccessByOrderSn(String orderSn, Integer payType) {
        OmsOrderExample example =  new OmsOrderExample();
        example.createCriteria()
                .andOrderSnEqualTo(orderSn)
                .andStatusEqualTo(0)
                .andDeleteStatusEqualTo(0);
        List<OmsOrder> orderList = orderMapper.selectByExample(example);
        // 添加异常提示
        if(CollUtil.isEmpty(orderList)){
            Asserts.fail("订单不存在或订单状态不是未支付�?);
        }
        OmsOrder order = orderList.get(0);
        paySuccess(order.getId(),payType);
    }

    /**
     * 生成18位订单编�?8位日�?2位平台号�?2位支付方�?6位以上自增id
     */
    private String generateOrderSn(OmsOrder order) {
        StringBuilder sb = new StringBuilder();
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = REDIS_DATABASE+":"+ REDIS_KEY_ORDER_ID + date;
        Long increment = redisService.incr(key, 1);
        sb.append(date);
        sb.append(String.format("%02d", order.getSourceType()));
        sb.append(String.format("%02d", order.getPayType()));
        String incrementStr = increment.toString();
        if (incrementStr.length() <= 6) {
            sb.append(String.format("%06d", increment));
        } else {
            sb.append(incrementStr);
        }
        return sb.toString();
    }

    /**
     * 从购物车中删除已下单的商品信�?     */
    private void deleteCartItemList(List<CartPromotionItem> cartPromotionItemList, UmsMember currentMember) {
        List<Long> ids = new ArrayList<>();
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            ids.add(cartPromotionItem.getId());
        }
        cartItemService.delete(currentMember.getId(), ids);
    }

    /**
     * 计算该订单赠送的成长�?     */
    private Integer calcGiftGrowth(List<OmsOrderItem> orderItemList) {
        Integer sum = 0;
        for (OmsOrderItem orderItem : orderItemList) {
            sum = sum + orderItem.getGiftGrowth() * orderItem.getProductQuantity();
        }
        return sum;
    }

    /**
     * 计算该订单赠送的积分
     */
    private Integer calcGifIntegration(List<OmsOrderItem> orderItemList) {
        int sum = 0;
        for (OmsOrderItem orderItem : orderItemList) {
            sum += orderItem.getGiftIntegration() * orderItem.getProductQuantity();
        }
        return sum;
    }

    /**
     * 将优惠券信息更改为指定状�?     *
     * @param couponId  优惠券id
     * @param memberId  会员id
     * @param useStatus 0->未使用；1->已使�?     */
    private void updateCouponStatus(Long couponId, Long memberId, Integer useStatus) {
        if (couponId == null) return;
        //查询第一张优惠券
        SmsCouponHistoryExample example = new SmsCouponHistoryExample();
        example.createCriteria().andMemberIdEqualTo(memberId)
                .andCouponIdEqualTo(couponId).andUseStatusEqualTo(useStatus == 0 ? 1 : 0);
        List<SmsCouponHistory> couponHistoryList = couponHistoryMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(couponHistoryList)) {
            SmsCouponHistory couponHistory = couponHistoryList.get(0);
            couponHistory.setUseTime(new Date());
            couponHistory.setUseStatus(useStatus);
            couponHistoryMapper.updateByPrimaryKeySelective(couponHistory);
        }
    }

    private void handleRealAmount(List<OmsOrderItem> orderItemList) {
        for (OmsOrderItem orderItem : orderItemList) {
            //原价-促销优惠-优惠券抵�?积分抵扣
            BigDecimal realAmount = orderItem.getProductPrice()
                    .subtract(orderItem.getPromotionAmount())
                    .subtract(orderItem.getCouponAmount())
                    .subtract(orderItem.getIntegrationAmount());
            orderItem.setRealAmount(realAmount);
        }
    }

    /**
     * 获取订单促销信息
     */
    private String getOrderPromotionInfo(List<OmsOrderItem> orderItemList) {
        StringBuilder sb = new StringBuilder();
        for (OmsOrderItem orderItem : orderItemList) {
            sb.append(orderItem.getPromotionName());
            sb.append(";");
        }
        String result = sb.toString();
        if (result.endsWith(";")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 计算订单应付金额
     */
    private BigDecimal calcPayAmount(OmsOrder order) {
        //总金�?运费-促销优惠-优惠券优�?积分抵扣
        BigDecimal payAmount = order.getTotalAmount()
                .add(order.getFreightAmount())
                .subtract(order.getPromotionAmount())
                .subtract(order.getCouponAmount())
                .subtract(order.getIntegrationAmount());
        return payAmount;
    }

    /**
     * 计算订单优惠券金�?     */
    private BigDecimal calcIntegrationAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal integrationAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getIntegrationAmount() != null) {
                integrationAmount = integrationAmount.add(orderItem.getIntegrationAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return integrationAmount;
    }

    /**
     * 计算订单优惠券金�?     */
    private BigDecimal calcCouponAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal couponAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getCouponAmount() != null) {
                couponAmount = couponAmount.add(orderItem.getCouponAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return couponAmount;
    }

    /**
     * 计算订单活动优惠
     */
    private BigDecimal calcPromotionAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal promotionAmount = new BigDecimal(0);
        for (OmsOrderItem orderItem : orderItemList) {
            if (orderItem.getPromotionAmount() != null) {
                promotionAmount = promotionAmount.add(orderItem.getPromotionAmount().multiply(new BigDecimal(orderItem.getProductQuantity())));
            }
        }
        return promotionAmount;
    }

    /**
     * 获取可用积分抵扣金额
     *
     * @param useIntegration 使用的积分数�?     * @param totalAmount    订单总金�?     * @param currentMember  使用的用�?     * @param hasCoupon      是否已经使用优惠�?     */
    private BigDecimal getUseIntegrationAmount(Integer useIntegration, BigDecimal totalAmount, UmsMember currentMember, boolean hasCoupon) {
        BigDecimal zeroAmount = new BigDecimal(0);
        //判断用户是否有这么多积分
        if (useIntegration.compareTo(currentMember.getIntegration()) > 0) {
            return zeroAmount;
        }
        //根据积分使用规则判断是否可用
        //是否可与优惠券共�?        UmsIntegrationConsumeSetting integrationConsumeSetting = integrationConsumeSettingMapper.selectByPrimaryKey(1L);
        if (hasCoupon && integrationConsumeSetting.getCouponStatus().equals(0)) {
            //不可与优惠券共用
            return zeroAmount;
        }
        //是否达到最低使用积分门�?        if (useIntegration.compareTo(integrationConsumeSetting.getUseUnit()) < 0) {
            return zeroAmount;
        }
        //是否超过订单抵用最高百分比
        BigDecimal integrationAmount = new BigDecimal(useIntegration).divide(new BigDecimal(integrationConsumeSetting.getUseUnit()), 2, RoundingMode.HALF_EVEN);
        BigDecimal maxPercent = new BigDecimal(integrationConsumeSetting.getMaxPercentPerOrder()).divide(new BigDecimal(100), 2, RoundingMode.HALF_EVEN);
        if (integrationAmount.compareTo(totalAmount.multiply(maxPercent)) > 0) {
            return zeroAmount;
        }
        return integrationAmount;
    }

    /**
     * 对优惠券优惠进行处理
     *
     * @param orderItemList       order_item列表
     * @param couponHistoryDetail 可用优惠券详�?     */
    private void handleCouponAmount(List<OmsOrderItem> orderItemList, SmsCouponHistoryDetail couponHistoryDetail) {
        SmsCoupon coupon = couponHistoryDetail.getCoupon();
        if (coupon.getUseType().equals(0)) {
            //全场通用
            calcPerCouponAmount(orderItemList, coupon);
        } else if (coupon.getUseType().equals(1)) {
            //指定分类
            List<OmsOrderItem> couponOrderItemList = getCouponOrderItemByRelation(couponHistoryDetail, orderItemList, 0);
            calcPerCouponAmount(couponOrderItemList, coupon);
        } else if (coupon.getUseType().equals(2)) {
            //指定商品
            List<OmsOrderItem> couponOrderItemList = getCouponOrderItemByRelation(couponHistoryDetail, orderItemList, 1);
            calcPerCouponAmount(couponOrderItemList, coupon);
        }
    }

    /**
     * 对每个下单商品进行优惠券金额分摊的计�?     *
     * @param orderItemList 可用优惠券的下单商品商品
     */
    private void calcPerCouponAmount(List<OmsOrderItem> orderItemList, SmsCoupon coupon) {
        BigDecimal totalAmount = calcTotalAmount(orderItemList);
        for (OmsOrderItem orderItem : orderItemList) {
            //(商品价格/可用商品总价)*优惠券面�?            BigDecimal couponAmount = orderItem.getProductPrice().divide(totalAmount, 3, RoundingMode.HALF_EVEN).multiply(coupon.getAmount());
            orderItem.setCouponAmount(couponAmount);
        }
    }

    /**
     * 获取与优惠券有关系的下单商品
     *
     * @param couponHistoryDetail 优惠券详�?     * @param orderItemList       下单商品
     * @param type                使用关系类型�?->相关分类�?->指定商品
     */
    private List<OmsOrderItem> getCouponOrderItemByRelation(SmsCouponHistoryDetail couponHistoryDetail, List<OmsOrderItem> orderItemList, int type) {
        List<OmsOrderItem> result = new ArrayList<>();
        if (type == 0) {
            List<Long> categoryIdList = new ArrayList<>();
            for (SmsCouponProductCategoryRelation productCategoryRelation : couponHistoryDetail.getCategoryRelationList()) {
                categoryIdList.add(productCategoryRelation.getProductCategoryId());
            }
            for (OmsOrderItem orderItem : orderItemList) {
                if (categoryIdList.contains(orderItem.getProductCategoryId())) {
                    result.add(orderItem);
                } else {
                    orderItem.setCouponAmount(new BigDecimal(0));
                }
            }
        } else if (type == 1) {
            List<Long> productIdList = new ArrayList<>();
            for (SmsCouponProductRelation productRelation : couponHistoryDetail.getProductRelationList()) {
                productIdList.add(productRelation.getProductId());
            }
            for (OmsOrderItem orderItem : orderItemList) {
                if (productIdList.contains(orderItem.getProductId())) {
                    result.add(orderItem);
                } else {
                    orderItem.setCouponAmount(new BigDecimal(0));
                }
            }
        }
        return result;
    }

    /**
     * 获取该用户可以使用的优惠�?     *
     * @param cartPromotionItemList 购物车优惠列�?     * @param couponId              使用优惠券id
     */
    private SmsCouponHistoryDetail getUseCoupon(List<CartPromotionItem> cartPromotionItemList, Long couponId) {
        List<SmsCouponHistoryDetail> couponHistoryDetailList = memberCouponService.listCart(cartPromotionItemList, 1);
        for (SmsCouponHistoryDetail couponHistoryDetail : couponHistoryDetailList) {
            if (couponHistoryDetail.getCoupon().getId().equals(couponId)) {
                return couponHistoryDetail;
            }
        }
        return null;
    }

    /**
     * 计算总金�?     */
    private BigDecimal calcTotalAmount(List<OmsOrderItem> orderItemList) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsOrderItem item : orderItemList) {
            totalAmount = totalAmount.add(item.getProductPrice().multiply(new BigDecimal(item.getProductQuantity())));
        }
        return totalAmount;
    }

    /**
     * 锁定下单商品的所有库�?     */
    private void lockStock(List<CartPromotionItem> cartPromotionItemList) {
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            PmsSkuStock skuStock = skuStockMapper.selectByPrimaryKey(cartPromotionItem.getProductSkuId());
            skuStock.setLockStock(skuStock.getLockStock() + cartPromotionItem.getQuantity());
            int count = portalOrderDao.lockStockBySkuId(cartPromotionItem.getProductSkuId(),cartPromotionItem.getQuantity());
            if(count==0){
                Asserts.fail("库存不足，无法下�?);
            }
        }
    }

    /**
     * 判断下单商品是否都有库存
     */
    private boolean hasStock(List<CartPromotionItem> cartPromotionItemList) {
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            if (cartPromotionItem.getRealStock()==null //判断真实库存是否为空
                    ||cartPromotionItem.getRealStock() <= 0 //判断真实库存是否小于0
                    || cartPromotionItem.getRealStock() < cartPromotionItem.getQuantity()) //判断真实库存是否小于下单的数�?            {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算购物车中商品的价�?     */
    private ConfirmOrderResult.CalcAmount calcCartAmount(List<CartPromotionItem> cartPromotionItemList) {
        ConfirmOrderResult.CalcAmount calcAmount = new ConfirmOrderResult.CalcAmount();
        calcAmount.setFreightAmount(new BigDecimal(0));
        BigDecimal totalAmount = new BigDecimal("0");
        BigDecimal promotionAmount = new BigDecimal("0");
        for (CartPromotionItem cartPromotionItem : cartPromotionItemList) {
            totalAmount = totalAmount.add(cartPromotionItem.getPrice().multiply(new BigDecimal(cartPromotionItem.getQuantity())));
            promotionAmount = promotionAmount.add(cartPromotionItem.getReduceAmount().multiply(new BigDecimal(cartPromotionItem.getQuantity())));
        }
        calcAmount.setTotalAmount(totalAmount);
        calcAmount.setPromotionAmount(promotionAmount);
        calcAmount.setPayAmount(totalAmount.subtract(promotionAmount));
        return calcAmount;
    }

}