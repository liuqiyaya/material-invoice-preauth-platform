package com.liuqiya.invoice.controller;

import com.liuqiya.invoice.pojo.dto.CategoryRuleDTO;
import com.liuqiya.invoice.pojo.dto.CategoryRuleQueryDTO;
import com.liuqiya.invoice.pojo.entity.CategoryRule;
import com.liuqiya.invoice.pojo.vo.PageVO;
import com.liuqiya.invoice.result.Result;
import com.liuqiya.invoice.service.CategoryRuleService;
import com.liuqiya.invoice.utils.ThreadLocalUtil;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 维护分类规则
 */
@Slf4j
@RestController
@RequestMapping("/category-rule")
public class CategoryRuleController {
    @Autowired
    private CategoryRuleService categoryRuleService;

    /**
     * 查询分类规则列表(可筛选)
     */
    @GetMapping("/list")
    public Result<PageVO<CategoryRule>> categoryRuleList(CategoryRuleQueryDTO query) {
        return Result.success(categoryRuleService.getCategoryRuleList(query));
    }

    /**
     * 查询分类规则详情
     */
    @GetMapping("/detail")
    public Result<CategoryRule> categoryRuleDetail(@RequestParam("id") Long id) {
        return Result.success(categoryRuleService.getCategoryRuleDetail(id));
    }

    /**
     * 新增分类规则
     */
    @PostMapping("/add")
    public Result<Long> addCategoryRule(@Valid @RequestBody CategoryRuleDTO dto) {
        Long adminId = currentAdminId();
        return Result.success("新增成功", categoryRuleService.addCategoryRule(dto, adminId));
    }

    /**
     * 修改分类规则
     */
    @PutMapping("/update")
    public Result<Void> updateCategoryRule(@Valid @RequestBody CategoryRuleDTO dto) {
        Long adminId = currentAdminId();
        categoryRuleService.updateCategoryRule(dto, adminId);
        return Result.success();
    }

    /**
     * 删除分类规则
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteCategoryRule(@RequestParam("id") Long id) {
        Long adminId = currentAdminId();
        categoryRuleService.deleteCategoryRule(id, adminId);
        return Result.success();
    }

    /**
     * 启用/禁用分类规则
     */
    @PostMapping("/toggle-enabled")
    public Result<Void> toggleEnabled(@RequestParam("id") Long id,
                                      @RequestParam("enabled") Integer enabled) {
        Long adminId = currentAdminId();
        categoryRuleService.toggleEnabled(id, enabled, adminId);
        return Result.success();
    }

    private Long currentAdminId() {
        Claims claims = ThreadLocalUtil.get();
        return Long.valueOf(claims.get("id").toString());
    }
}
