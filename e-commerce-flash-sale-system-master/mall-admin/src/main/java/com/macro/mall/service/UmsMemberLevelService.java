package com.macro.mall.service;

import com.macro.mall.model.UmsMemberLevel;

import java.util.List;

/**
 * 会员等级管理Service

 */
public interface UmsMemberLevelService {
    /**
     * 获取所有会员等�?     * @param defaultStatus 是否为默认会�?     */
    List<UmsMemberLevel> list(Integer defaultStatus);
}
