package com.macro.mall.portal.repository;

import com.macro.mall.portal.domain.MemberReadHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * 会员商品浏览历史Repository（简化版，不依赖 MongoDB）
 */
public class MemberReadHistoryRepository {
    /**
     * 根据会员id获取浏览记录（简化实现，返回空列表）
     */
    public List<MemberReadHistory> findByMemberIdOrderByCreateTimeDesc(Long memberId) {
        return new ArrayList<>();
    }

    /**
     * 保存记录（简化实现）
     */
    public MemberReadHistory save(MemberReadHistory history) {
        return history;
    }

    /**
     * 删除记录（简化实现）
     */
    public void deleteById(String id) {
        // 简化实现，不做任何操作
    }

    /**
     * 批量删除（简化实现）
     */
    public void deleteAllById(List<String> ids) {
        // 简化实现，不做任何操作
    }

    /**
     * 根据会员ID删除记录（简化实现）
     */
    public void deleteAllByMemberId(Long memberId) {
        // 简化实现，不做任何操作
    }
}
