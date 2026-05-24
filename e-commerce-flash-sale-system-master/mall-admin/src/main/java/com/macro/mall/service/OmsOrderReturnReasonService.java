package com.macro.mall.service;

import com.macro.mall.model.OmsOrderReturnReason;

import java.util.List;

/**
 * 退货原因管理Service

 */
public interface OmsOrderReturnReasonService {
    /**
     * 添加退货原�?     */
    int create(OmsOrderReturnReason returnReason);

    /**
     * 修改退货原�?     */
    int update(Long id, OmsOrderReturnReason returnReason);

    /**
     * 批量删除退货原�?     */
    int delete(List<Long> ids);

    /**
     * 分页获取退货原�?     */
    List<OmsOrderReturnReason> list(Integer pageSize, Integer pageNum);

    /**
     * 批量修改退货原因状�?     */
    int updateStatus(List<Long> ids, Integer status);

    /**
     * 获取单个退货原因详情信�?     */
    OmsOrderReturnReason getItem(Long id);
}
