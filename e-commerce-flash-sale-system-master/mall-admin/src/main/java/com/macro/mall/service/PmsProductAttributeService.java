package com.macro.mall.service;

import com.macro.mall.dto.PmsProductAttributeParam;
import com.macro.mall.dto.ProductAttrInfo;
import com.macro.mall.model.PmsProductAttribute;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品属性管理Service

 */
public interface PmsProductAttributeService {
    /**
     * 根据分类ID和类型分页获取商品属�?     * @param cid 分类id
     * @param type 0->规格�?->参数
     */
    List<PmsProductAttribute> getList(Long cid, Integer type, Integer pageSize, Integer pageNum);

    /**
     * 添加商品属�?     */
    @Transactional
    int create(PmsProductAttributeParam pmsProductAttributeParam);

    /**
     * 修改商品属�?     */
    int update(Long id, PmsProductAttributeParam productAttributeParam);

    /**
     * 获取单个商品属性信�?     */
    PmsProductAttribute getItem(Long id);

    /**
     * 批量删除商品属�?     */
    @Transactional
    int delete(List<Long> ids);

    /**
     * 获取商品分类对应属性列�?     */
    List<ProductAttrInfo> getProductAttrInfo(Long productCategoryId);
}
