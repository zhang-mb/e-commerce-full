package com.macro.mall.portal.service.impl;

import com.macro.mall.mapper.*;
import com.macro.mall.model.*;
import com.macro.mall.portal.domain.FlashPromotionProduct;
import com.macro.mall.portal.domain.SeckillOrderParam;  // 新增导入
import com.macro.mall.portal.service.SeckillService;
import com.macro.mall.portal.service.UmsMemberService;  // 新增导入
import com.macro.mall.portal.util.DateUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;  // 新增导入
import java.text.SimpleDateFormat;  // 新增导入
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 秒杀服务实现类
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillServiceImpl.class);
    private static final String SECKILL_STOCK_KEY = "seckill:stock:";
    private static final String SECKILL_ORDER_KEY = "seckill:order:";
    private static final String SECKILL_PRODUCT_LIST_KEY = "seckill:product:list";

    @Autowired
    private SmsFlashPromotionMapper flashPromotionMapper;
    @Autowired
    private SmsFlashPromotionSessionMapper promotionSessionMapper;
    @Autowired
    private SmsFlashPromotionProductRelationMapper flashPromotionProductRelationMapper;
    @Autowired
    private PmsProductMapper productMapper;
    @Autowired
    private PmsSkuStockMapper skuStockMapper;  // 新增注入
    @Autowired
    private OmsOrderMapper orderMapper;  // 新增注入
    @Autowired
    private OmsOrderItemMapper orderItemMapper;  // 新增注入
    @Autowired
    private UmsMemberReceiveAddressMapper addressMapper;  // 新增注入
    @Autowired
    private UmsMemberService memberService;  // 新增注入
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SmsFlashPromotionLogMapper flashPromotionLogMapper;  // 新增注入

    @Override
    public SmsFlashPromotion getCurrentFlashPromotion() {
        return getFlashPromotion(new Date());
    }

    @Override
    public List<SmsFlashPromotionSession> getFlashPromotionSessionList() {
        SmsFlashPromotion promotion = getFlashPromotion(new Date());
        if (promotion == null) {
            return getMockSessions();
        }
        // 数据库中有活动，查询所有启用的场次
        SmsFlashPromotionSessionExample example = new SmsFlashPromotionSessionExample();
        example.createCriteria().andStatusEqualTo(1);
        example.setOrderByClause("start_time asc");
        return promotionSessionMapper.selectByExample(example);
    }

    @Override
    public List<FlashPromotionProduct> getFlashPromotionProducts(Long sessionId) {
        return getFlashPromotionProductsInternal(sessionId);
    }

    @Override
    public List<FlashPromotionProduct> getSeckillProductList() {
        // 先从Redis缓存获取
        String cacheKey = SECKILL_PRODUCT_LIST_KEY;
        List<FlashPromotionProduct> cachedList = (List<FlashPromotionProduct>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedList != null && !cachedList.isEmpty()) {
            LOGGER.debug("从Redis缓存获取秒杀商品列表，数量: {}", cachedList.size());
            return cachedList;
        }

        Date now = new Date();
        SmsFlashPromotion flashPromotion = getFlashPromotion(now);
        if (flashPromotion == null) {
            // 无有效秒杀活动，返回模拟数据
            LOGGER.debug("当前无有效秒杀活动，返回模拟数据");
            List<FlashPromotionProduct> mockList = getMockSeckillProducts();
            redisTemplate.opsForValue().set(cacheKey, mockList, 10, TimeUnit.MINUTES);
            return mockList;
        }
        SmsFlashPromotionSession session = getFlashPromotionSession(now);
        if (session == null) {
            // 无当前场次，返回模拟数据
            LOGGER.debug("当前无秒杀场次，返回模拟数据");
            List<FlashPromotionProduct> mockList = getMockSeckillProducts();
            redisTemplate.opsForValue().set(cacheKey, mockList, 10, TimeUnit.MINUTES);
            return mockList;
        }

        List<FlashPromotionProduct> productList = getFlashPromotionProducts(session.getId());
        
        // 将结果存入Redis缓存，有效期10分钟
        if (!productList.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, productList, 10, TimeUnit.MINUTES);
            LOGGER.info("秒杀商品列表已缓存到Redis，数量: {}", productList.size());
        }
        
        return productList;
    }

    @Override
    public FlashPromotionProduct getSeckillProduct(Long id) {
        Date now = new Date();
        SmsFlashPromotion flashPromotion = getFlashPromotion(now);
        if (flashPromotion == null) {
            return getMockSeckillProducts().stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
        SmsFlashPromotionSession session = getFlashPromotionSession(now);
        if (session == null) {
            return getMockSeckillProducts().stream()
                    .filter(p -> p.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
        return getFlashPromotionProducts(session.getId()).stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取模拟秒杀商品数据
     */
    private List<FlashPromotionProduct> getMockSeckillProducts() {
        List<FlashPromotionProduct> list = new ArrayList<>();
        
        FlashPromotionProduct p1 = new FlashPromotionProduct();
        p1.setId(1L);
        p1.setName("Apple iPhone 15 Pro");
        p1.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/redmi_k50_01.jpg");
        p1.setPrice(new BigDecimal(7999));
        p1.setFlashPromotionPrice(new BigDecimal(5999));
        p1.setFlashPromotionCount(100);
        p1.setFlashPromotionLimit(1);
        p1.setProductSn("IP15PRO");
        p1.setProductCategoryId(1L);
        p1.setFlashPromotionId(1L);
        p1.setFlashPromotionSessionId(1L);
        list.add(p1);
        
        FlashPromotionProduct p2 = new FlashPromotionProduct();
        p2.setId(2L);
        p2.setName("Sony WH-1000XM5 耳机");
        p2.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/huawei_mate50_01.jpg");
        p2.setPrice(new BigDecimal(2999));
        p2.setFlashPromotionPrice(new BigDecimal(1999));
        p2.setFlashPromotionCount(50);
        p2.setFlashPromotionLimit(1);
        p2.setProductSn("SONYWH5");
        p2.setProductCategoryId(2L);
        p2.setFlashPromotionId(1L);
        p2.setFlashPromotionSessionId(1L);
        list.add(p2);
        
        FlashPromotionProduct p3 = new FlashPromotionProduct();
        p3.setId(3L);
        p3.setName("MacBook Air M3");
        p3.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221108/oppo_r8_01.jpg");
        p3.setPrice(new BigDecimal(11999));
        p3.setFlashPromotionPrice(new BigDecimal(8999));
        p3.setFlashPromotionCount(30);
        p3.setFlashPromotionLimit(1);
        p3.setProductSn("MBAIRM3");
        p3.setProductCategoryId(1L);
        p3.setFlashPromotionId(1L);
        p3.setFlashPromotionSessionId(1L);
        list.add(p3);
        
        FlashPromotionProduct p4 = new FlashPromotionProduct();
        p4.setId(4L);
        p4.setName("Nike Air Max 运动鞋");
        p4.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5b19403eN9f0b3cb8.jpg");
        p4.setPrice(new BigDecimal(1299));
        p4.setFlashPromotionPrice(new BigDecimal(699));
        p4.setFlashPromotionCount(200);
        p4.setFlashPromotionLimit(1);
        p4.setProductSn("NIKEAM");
        p4.setProductCategoryId(3L);
        p4.setFlashPromotionId(1L);
        p4.setFlashPromotionSessionId(1L);
        list.add(p4);
        
        FlashPromotionProduct p5 = new FlashPromotionProduct();
        p5.setId(5L);
        p5.setName("iPad Pro 12.9英寸");
        p5.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/redmi_k50_01.jpg");
        p5.setPrice(new BigDecimal(9999));
        p5.setFlashPromotionPrice(new BigDecimal(7999));
        p5.setFlashPromotionCount(80);
        p5.setFlashPromotionLimit(1);
        p5.setProductSn("IPADPRO");
        p5.setProductCategoryId(1L);
        p5.setFlashPromotionId(1L);
        p5.setFlashPromotionSessionId(1L);
        list.add(p5);
        
        FlashPromotionProduct p6 = new FlashPromotionProduct();
        p6.setId(6L);
        p6.setName("Samsung Galaxy S24 Ultra");
        p6.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/huawei_mate50_01.jpg");
        p6.setPrice(new BigDecimal(9699));
        p6.setFlashPromotionPrice(new BigDecimal(7699));
        p6.setFlashPromotionCount(60);
        p6.setFlashPromotionLimit(1);
        p6.setProductSn("S24ULTRA");
        p6.setProductCategoryId(1L);
        p6.setFlashPromotionId(1L);
        p6.setFlashPromotionSessionId(1L);
        list.add(p6);
        
        FlashPromotionProduct p7 = new FlashPromotionProduct();
        p7.setId(7L);
        p7.setName("华为Mate 60 Pro");
        p7.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/huawei_mate50_01.jpg");
        p7.setPrice(new BigDecimal(6999));
        p7.setFlashPromotionPrice(new BigDecimal(5499));
        p7.setFlashPromotionCount(120);
        p7.setFlashPromotionLimit(1);
        p7.setProductSn("MATE60P");
        p7.setProductCategoryId(1L);
        p7.setFlashPromotionId(1L);
        p7.setFlashPromotionSessionId(1L);
        list.add(p7);
        
        FlashPromotionProduct p8 = new FlashPromotionProduct();
        p8.setId(8L);
        p8.setName("小米14 Ultra");
        p8.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221104/redmi_k50_01.jpg");
        p8.setPrice(new BigDecimal(6499));
        p8.setFlashPromotionPrice(new BigDecimal(4999));
        p8.setFlashPromotionCount(150);
        p8.setFlashPromotionLimit(1);
        p8.setProductSn("MI14U");
        p8.setProductCategoryId(1L);
        p8.setFlashPromotionId(1L);
        p8.setFlashPromotionSessionId(1L);
        list.add(p8);
        
        FlashPromotionProduct p9 = new FlashPromotionProduct();
        p9.setId(9L);
        p9.setName("Sony PlayStation 5");
        p9.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20221108/oppo_r8_01.jpg");
        p9.setPrice(new BigDecimal(4299));
        p9.setFlashPromotionPrice(new BigDecimal(3499));
        p9.setFlashPromotionCount(40);
        p9.setFlashPromotionLimit(1);
        p9.setProductSn("PS5");
        p9.setProductCategoryId(4L);
        p9.setFlashPromotionId(1L);
        p9.setFlashPromotionSessionId(1L);
        list.add(p9);
        
        FlashPromotionProduct p10 = new FlashPromotionProduct();
        p10.setId(10L);
        p10.setName("Apple Watch Ultra 2");
        p10.setPic("http://macro-oss.oss-cn-shenzhen.aliyuncs.com/mall/images/20180615/5b19403eN9f0b3cb8.jpg");
        p10.setPrice(new BigDecimal(6499));
        p10.setFlashPromotionPrice(new BigDecimal(5299));
        p10.setFlashPromotionCount(50);
        p10.setFlashPromotionLimit(1);
        p10.setProductSn("WATCHU2");
        p10.setProductCategoryId(5L);
        p10.setFlashPromotionId(1L);
        p10.setFlashPromotionSessionId(1L);
        list.add(p10);
        
        // 初始化Redis库存
        list.forEach(p -> {
            String stockKey = SECKILL_STOCK_KEY + p.getId();
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
                redisTemplate.opsForValue().set(stockKey, p.getFlashPromotionCount());
            }
        });
        
        return list;
    }

    /**
     * 获取模拟场次数据
     */
    private List<SmsFlashPromotionSession> getMockSessions() {
        List<SmsFlashPromotionSession> sessions = new ArrayList<>();
        Date now = new Date();
        
        // 正在进行的场次
        SmsFlashPromotionSession session1 = new SmsFlashPromotionSession();
        session1.setId(1L);
        session1.setName("10:00场");
        session1.setStartTime(new java.sql.Time(now.getTime() - 2 * 60 * 60 * 1000));
        session1.setEndTime(new java.sql.Time(now.getTime() + 2 * 60 * 60 * 1000));
        session1.setStatus(1);
        sessions.add(session1);
        
        // 即将开始的场次
        SmsFlashPromotionSession session2 = new SmsFlashPromotionSession();
        session2.setId(2L);
        session2.setName("14:00场");
        session2.setStartTime(new java.sql.Time(now.getTime() + 3 * 60 * 60 * 1000));
        session2.setEndTime(new java.sql.Time(now.getTime() + 7 * 60 * 60 * 1000));
        session2.setStatus(1);
        sessions.add(session2);
        
        // 已结束的场次
        SmsFlashPromotionSession session3 = new SmsFlashPromotionSession();
        session3.setId(3L);
        session3.setName("08:00场");
        session3.setStartTime(new java.sql.Time(now.getTime() - 5 * 60 * 60 * 1000));
        session3.setEndTime(new java.sql.Time(now.getTime() - 1 * 60 * 60 * 1000));
        session3.setStatus(1);
        sessions.add(session3);
        
        return sessions;
    }

    /**
     * 获取当前时间的秒杀活动
     */
    private SmsFlashPromotion getFlashPromotion(Date date) {
        SmsFlashPromotionExample example = new SmsFlashPromotionExample();
        example.createCriteria()
                .andStatusEqualTo(1)
                .andStartDateLessThanOrEqualTo(date)
                .andEndDateGreaterThanOrEqualTo(date);
        List<SmsFlashPromotion> flashPromotions = flashPromotionMapper.selectByExample(example);
        return CollectionUtils.isEmpty(flashPromotions) ? null : flashPromotions.get(0);
    }

    /**
     * 获取当前时间的秒杀场次
     */
    private SmsFlashPromotionSession getFlashPromotionSession(Date date) {
        // 获取当前时间的时分秒部分
        java.sql.Time currentTime = new java.sql.Time(date.getTime());
        
        SmsFlashPromotionSessionExample example = new SmsFlashPromotionSessionExample();
        example.createCriteria()
                .andStatusEqualTo(1)
                .andStartTimeLessThanOrEqualTo(currentTime)
                .andEndTimeGreaterThanOrEqualTo(currentTime);
        List<SmsFlashPromotionSession> sessions = promotionSessionMapper.selectByExample(example);
        return CollectionUtils.isEmpty(sessions) ? null : sessions.get(0);
    }

    /**
     * 获取秒杀商品列表（优化N+1查询）
     */
    private List<FlashPromotionProduct> getFlashPromotionProductsInternal(Long sessionId) {
        List<FlashPromotionProduct> list = new ArrayList<>();
        
        // 1. 查询该场次的所有商品关联关系
        SmsFlashPromotionProductRelationExample example = new SmsFlashPromotionProductRelationExample();
        example.createCriteria().andFlashPromotionSessionIdEqualTo(sessionId);
        List<SmsFlashPromotionProductRelation> relations = flashPromotionProductRelationMapper.selectByExample(example);
        
        if (CollectionUtils.isEmpty(relations)) {
            return list;
        }
        
        // 2. 收集所有商品ID，批量查询（解决N+1问题）
        List<Long> productIds = relations.stream()
                .map(SmsFlashPromotionProductRelation::getProductId)
                .collect(java.util.stream.Collectors.toList());
        
        PmsProductExample productExample = new PmsProductExample();
        productExample.createCriteria().andIdIn(productIds);
        List<PmsProduct> products = productMapper.selectByExample(productExample);
        
        // 3. 将商品转换为Map，便于快速查找
        java.util.Map<Long, PmsProduct> productMap = products.stream()
                .collect(java.util.stream.Collectors.toMap(PmsProduct::getId, p -> p));
        
        // 4. 遍历关系列表，组装结果
        for (SmsFlashPromotionProductRelation relation : relations) {
            PmsProduct product = productMap.get(relation.getProductId());
            if (product != null) {
                FlashPromotionProduct flashProduct = new FlashPromotionProduct();
                BeanUtils.copyProperties(product, flashProduct);
                flashProduct.setFlashPromotionCount(relation.getFlashPromotionCount());
                flashProduct.setFlashPromotionLimit(relation.getFlashPromotionLimit());
                flashProduct.setFlashPromotionPrice(relation.getFlashPromotionPrice());
                flashProduct.setFlashPromotionId(relation.getFlashPromotionId());
                flashProduct.setFlashPromotionSessionId(relation.getFlashPromotionSessionId());
                list.add(flashProduct);
                
                // 初始化Redis库存
                String stockKey = SECKILL_STOCK_KEY + product.getId();
                if (!Boolean.TRUE.equals(redisTemplate.hasKey(stockKey))) {
                    redisTemplate.opsForValue().set(stockKey, relation.getFlashPromotionCount());
                }
            }
        }
        
        return list;
    }

    @Override
    @Transactional
    public boolean doSeckill(Long goodsId) {
        String stockKey = SECKILL_STOCK_KEY + goodsId;
        String orderKey = SECKILL_ORDER_KEY + goodsId + ":" + getCurrentUserId();

        try {
            // 1. 检查是否已购买
            if (Boolean.TRUE.equals(redisTemplate.hasKey(orderKey))) {
                LOGGER.warn("用户已购买过该秒杀商品，goodsId: {}", goodsId);
                return false; // 重复购买
            }

            // 2. 扣减库存（原子操作）
            Long stock = redisTemplate.opsForValue().decrement(stockKey);
            if (stock < 0) {
                redisTemplate.opsForValue().increment(stockKey); // 恢复库存
                LOGGER.warn("秒杀商品库存不足，goodsId: {}", goodsId);
                return false; // 库存不足
            }

            // 3. 标记已购买
            redisTemplate.opsForValue().set(orderKey, "1", 24, TimeUnit.HOURS);
            LOGGER.info("秒杀成功，goodsId: {}, 剩余库存: {}", goodsId, stock);

            return true;
        } catch (Exception e) {
            LOGGER.error("秒杀操作异常，goodsId: {}", goodsId, e);
            // 异常时恢复库存
            redisTemplate.opsForValue().increment(stockKey);
            return false;
        }
    }

    // ============ 新增方法：秒杀并生成订单 ============
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OmsOrder seckill(SeckillOrderParam param) {
        Long goodsId = param.getGoodsId();
        Long userId = getCurrentUserId();
        String stockKey = SECKILL_STOCK_KEY + goodsId;
        String orderKey = SECKILL_ORDER_KEY + goodsId + ":" + userId;

        try {
            LOGGER.info("用户开始秒杀，userId: {}, goodsId: {}", userId, goodsId);

            // 1. 检查是否已购买
            if (Boolean.TRUE.equals(redisTemplate.hasKey(orderKey))) {
                LOGGER.warn("用户已购买过该秒杀商品，userId: {}, goodsId: {}", userId, goodsId);
                throw new RuntimeException("您已经购买过该秒杀商品");
            }

            // 2. 扣减库存（原子操作）
            Long stock = redisTemplate.opsForValue().decrement(stockKey);
            if (stock < 0) {
                redisTemplate.opsForValue().increment(stockKey); // 恢复库存
                LOGGER.warn("秒杀商品库存不足，userId: {}, goodsId: {}", userId, goodsId);
                throw new RuntimeException("秒杀失败，库存不足");
            }

            // 3. 标记已购买
            redisTemplate.opsForValue().set(orderKey, "1", 24, TimeUnit.HOURS);

            // 4. 获取商品信息
            FlashPromotionProduct flashProduct = getSeckillProduct(goodsId);
            if (flashProduct == null) {
                LOGGER.error("秒杀商品不存在，goodsId: {}", goodsId);
                throw new RuntimeException("秒杀商品不存在");
            }

            // 5. 获取用户收货地址
            UmsMemberReceiveAddress address = addressMapper.selectByPrimaryKey(param.getMemberReceiveAddressId());
            if (address == null) {
                LOGGER.error("收货地址不存在，addressId: {}", param.getMemberReceiveAddressId());
                throw new RuntimeException("收货地址不存在");
            }

            // 6. 获取秒杀活动信息
            Date now = new Date();
            SmsFlashPromotion flashPromotion = getFlashPromotion(now);
            SmsFlashPromotionSession session = getFlashPromotionSession(now);

            // 7. 创建订单
            OmsOrder order = new OmsOrder();
            order.setMemberId(userId);
            order.setMemberUsername(getCurrentUsername());
            order.setCreateTime(new Date());
            order.setPayType(param.getPayType() != null ? param.getPayType() : 1);
            order.setSourceType(1); // APP订单
            order.setStatus(0); // 待付款
            order.setOrderType(1); // 秒杀订单

            // 设置收货人信息
            order.setReceiverName(address.getName());
            order.setReceiverPhone(address.getPhoneNumber());
            order.setReceiverPostCode(address.getPostCode());
            order.setReceiverProvince(address.getProvince());
            order.setReceiverCity(address.getCity());
            order.setReceiverRegion(address.getRegion());
            order.setReceiverDetailAddress(address.getDetailAddress());

            order.setConfirmStatus(0);
            order.setDeleteStatus(0);
            order.setAutoConfirmDay(7);

            // 设置秒杀活动信息
            if (flashPromotion != null) {
                order.setPromotionInfo("秒杀活动: " + flashPromotion.getTitle());
            }

            // 生成订单号
            order.setOrderSn(generateOrderSn(order));

            // 计算金额
            order.setTotalAmount(flashProduct.getFlashPromotionPrice());
            order.setFreightAmount(new BigDecimal(0));
            order.setPromotionAmount(flashProduct.getPrice().subtract(flashProduct.getFlashPromotionPrice()));
            order.setCouponAmount(new BigDecimal(0));
            order.setIntegration(0);
            order.setIntegrationAmount(new BigDecimal(0));
            order.setPayAmount(flashProduct.getFlashPromotionPrice());

            // 8. 插入订单
            orderMapper.insert(order);
            LOGGER.info("订单创建成功，orderId: {}, orderSn: {}", order.getId(), order.getOrderSn());

            // 9. 创建订单项
            OmsOrderItem orderItem = new OmsOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderSn(order.getOrderSn());
            orderItem.setProductId(flashProduct.getId());
            orderItem.setProductName(flashProduct.getName());
            orderItem.setProductPic(flashProduct.getPic());
            orderItem.setProductSn(flashProduct.getProductSn());
            orderItem.setProductPrice(flashProduct.getPrice());
            orderItem.setProductQuantity(1);
            orderItem.setPromotionAmount(order.getPromotionAmount());
            orderItem.setPromotionName("秒杀活动");
            orderItem.setCouponAmount(new BigDecimal(0));
            orderItem.setIntegrationAmount(new BigDecimal(0));
            orderItem.setRealAmount(flashProduct.getFlashPromotionPrice());

            // 获取SKU信息
            PmsSkuStockExample skuExample = new PmsSkuStockExample();
            skuExample.createCriteria().andProductIdEqualTo(flashProduct.getId());
            List<PmsSkuStock> skuStocks = skuStockMapper.selectByExample(skuExample);
            if (!CollectionUtils.isEmpty(skuStocks)) {
                PmsSkuStock skuStock = skuStocks.get(0);
                orderItem.setProductSkuId(skuStock.getId());
                orderItem.setProductSkuCode(skuStock.getSkuCode());
            }

            orderItem.setProductCategoryId(flashProduct.getProductCategoryId());
            orderItemMapper.insert(orderItem);

            // 10. 记录秒杀日志
            SmsFlashPromotionLog log = new SmsFlashPromotionLog();
            log.setMemberId(userId.intValue());  // 转换为 Integer
            log.setProductId(goodsId);
            log.setProductName(flashProduct.getName());
            log.setSubscribeTime(new Date());
            flashPromotionLogMapper.insert(log);

            LOGGER.info("秒杀订单创建完成，orderId: {}, goodsId: {}, userId: {}", order.getId(), goodsId, userId);
            return order;

        } catch (RuntimeException e) {
            // 业务异常，记录日志后重新抛出
            LOGGER.error("秒杀业务异常，userId: {}, goodsId: {}, error: {}", userId, goodsId, e.getMessage());
            throw e;
        } catch (Exception e) {
            // 系统异常，记录完整堆栈
            LOGGER.error("秒杀系统异常，userId: {}, goodsId: {}", userId, goodsId, e);
            // 恢复库存
            redisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("秒杀失败，请稍后重试");
        }
    }

    /**
     * 生成18位订单编号
     */
    private String generateOrderSn(OmsOrder order) {
        StringBuilder sb = new StringBuilder();
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = "mall:oms:orderId" + date;
        Long increment = redisTemplate.opsForValue().increment(key, 1);
        if (increment == null) {
            increment = 1L;
            redisTemplate.opsForValue().set(key, increment, 24, TimeUnit.HOURS);
        }
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

    private String getCurrentUsername() {
        UmsMember member = memberService.getCurrentMember();
        return member != null ? member.getUsername() : "anonymous";
    }

    private Long getCurrentUserId() {
        UmsMember member = memberService.getCurrentMember();
        return member != null ? member.getId() : 1L;
    }
}