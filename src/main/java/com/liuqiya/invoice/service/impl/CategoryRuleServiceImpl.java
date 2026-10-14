package com.liuqiya.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liuqiya.invoice.enumeration.CategoryLevel;
import com.liuqiya.invoice.exception.BizException;
import com.liuqiya.invoice.mapper.CategoryRuleMapper;
import com.liuqiya.invoice.mapper.OperationLogMapper;
import com.liuqiya.invoice.pojo.dto.CategoryRuleDTO;
import com.liuqiya.invoice.pojo.dto.CategoryRuleQueryDTO;
import com.liuqiya.invoice.pojo.entity.CategoryRule;
import com.liuqiya.invoice.pojo.entity.OperationLog;
import com.liuqiya.invoice.pojo.vo.PageVO;
import com.liuqiya.invoice.service.CategoryRuleService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class CategoryRuleServiceImpl implements CategoryRuleService {
    @Autowired
    private CategoryRuleMapper categoryRuleMapper;
    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public PageVO<CategoryRule> getCategoryRuleList(CategoryRuleQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        if (pageSize > 200) {pageSize = 200;}

        LambdaQueryWrapper<CategoryRule> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getCategoryName())) {
            wrapper.like(CategoryRule::getCategoryName, query.getCategoryName());
        }
        if (query.getLevel() != null) {
            wrapper.eq(CategoryRule::getLevel, CategoryLevel.of(query.getLevel()));
        }
        if (query.getEnabled() != null) {
            wrapper.eq(CategoryRule::getEnabled, query.getEnabled());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(CategoryRule::getKeywords, query.getKeyword());
        }
        wrapper.orderByAsc(CategoryRule::getLevel).orderByAsc(CategoryRule::getId);

        Page<CategoryRule> page = categoryRuleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageVO.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public CategoryRule getCategoryRuleDetail(Long id) {
        if (id == null) { throw new BizException(400, "id 不能为空");}
        CategoryRule rule = categoryRuleMapper.selectById(id);
        if (rule == null) { throw new BizException(404, "规则不存在：" + id);}
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCategoryRule(CategoryRuleDTO dto, Long operatorId) {
        // 校验
        validateDTO(dto);

        // 重复校验：同名同结论
        Long count = categoryRuleMapper.selectCount(
                new LambdaQueryWrapper<CategoryRule>()
                        .eq(CategoryRule::getCategoryName, dto.getCategoryName())
                        .eq(CategoryRule::getLevel, CategoryLevel.of(dto.getLevel()))
        );
        if (count != null && count > 0) {
            throw new BizException(400, "该品类名 + 结论已存在");
        }

        CategoryRule rule = new CategoryRule();
        rule.setCategoryName(dto.getCategoryName());
        rule.setKeywords(dto.getKeywords());
        rule.setLevel(dto.getLevel());
        rule.setRemark(dto.getRemark());
        rule.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());

        categoryRuleMapper.insert(rule);

        writeLog(operatorId, "CATEGORY_RULE_ADD", null,
                "新增规则：id=" + rule.getId() + ", name=" + dto.getCategoryName());
        log.info("【品类规则】新增 id={}, name={}", rule.getId(), dto.getCategoryName());
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategoryRule(CategoryRuleDTO dto, Long operatorId) {
        if (dto.getId() == null) { throw new BizException(400, "id 不能为空");}
        validateDTO(dto);

        CategoryRule rule = categoryRuleMapper.selectById(dto.getId());
        if (rule == null) { throw new BizException(404, "规则不存在：" + dto.getId());}

        String before = snapshot(rule);

        rule.setCategoryName(dto.getCategoryName());
        rule.setKeywords(dto.getKeywords());
        rule.setLevel(dto.getLevel());
        rule.setRemark(dto.getRemark());
        if (dto.getEnabled() != null) {
            rule.setEnabled(dto.getEnabled());
        }
        categoryRuleMapper.updateById(rule);

        writeLog(operatorId, "CATEGORY_RULE_UPDATE", before, snapshot(rule));
        log.info("【品类规则】修改 id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategoryRule(Long id, Long operatorId) {
        if (id == null) { throw new BizException(400, "id 不能为空");}
        CategoryRule rule = categoryRuleMapper.selectById(id);
        if (rule == null) { throw new BizException(404, "规则不存在：" + id);}

        String before = snapshot(rule);
        categoryRuleMapper.deleteById(id);

        writeLog(operatorId, "CATEGORY_RULE_DELETE", before, null);
        log.info("【品类规则】删除 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long id, Integer enabled, Long operatorId) {
        if (id == null) { throw new BizException(400, "id 不能为空");}
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new BizException(400, "enabled 只能是 0 或 1");
        }
        CategoryRule rule = categoryRuleMapper.selectById(id);
        if (rule == null) { throw new BizException(404, "规则不存在：" + id);}

        String before = snapshot(rule);
        rule.setEnabled(enabled);
        categoryRuleMapper.updateById(rule);

        writeLog(operatorId, "CATEGORY_RULE_TOGGLE",
                before, "enabled=" + enabled);
        log.info("【品类规则】切换状态 id={}, enabled={}", id, enabled);
    }


    private void validateDTO(CategoryRuleDTO dto) {
        if (!StringUtils.hasText(dto.getCategoryName())) {
            throw new BizException(400, "品类名不能为空");
        }
        if (dto.getLevel() == null) {
            throw new BizException(400, "level 不能为空");
        }
        try {
            CategoryLevel.of(dto.getLevel());
        } catch (Exception e) {
            throw new BizException(400, "无效的 level：" + dto.getLevel());
        }
    }

    private String snapshot(CategoryRule r) {
        return "name=" + r.getCategoryName()
                + ", keywords=" + r.getKeywords()
                + ", level=" + (r.getLevel() == null ? null : r.getLevel())
                + ", enabled=" + r.getEnabled()
                + ", remark=" + r.getRemark();
    }

    private void writeLog(Long operatorId, String action, String before, String after) {
        try {
            OperationLog log = new OperationLog();
            log.setOperatorId(operatorId);
            log.setAction(action);
            log.setBeforeValue(before);
            log.setAfterValue(after);
            operationLogMapper.insert(log);
        } catch (Exception e) {
            log.error("写日志失败：action={}", action, e);
        }
    }
}
