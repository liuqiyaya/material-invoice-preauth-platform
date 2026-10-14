package com.liuqiya.invoice.service;

import com.liuqiya.invoice.pojo.dto.CategoryRuleDTO;
import com.liuqiya.invoice.pojo.dto.CategoryRuleQueryDTO;
import com.liuqiya.invoice.pojo.entity.CategoryRule;
import com.liuqiya.invoice.pojo.vo.PageVO;

/**
 * 品类规则服务接口
 */
public interface CategoryRuleService {

    //查询品类规则
    PageVO<CategoryRule> getCategoryRuleList(CategoryRuleQueryDTO query);

    CategoryRule getCategoryRuleDetail(Long id);

    Long addCategoryRule(CategoryRuleDTO dto, Long operatorId);

    void updateCategoryRule(CategoryRuleDTO dto, Long operatorId);

    void deleteCategoryRule(Long id, Long operatorId);

    void toggleEnabled(Long id, Integer enabled, Long operatorId);
}
