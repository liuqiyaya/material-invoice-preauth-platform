package com.liuqiya.invoice.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 分类规则
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("category_rule")
public class CategoryRule {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 关键词
     */
    @TableField("keywords")
    private String keywords;

    /**
     * 分类结论:1-可以,2-存疑,3-不可以
     */
    @TableField("level")
    private Integer level;

    /**
     * 分类说明
     */
    @TableField("remark")
    private String remark;

    /**
     * 是否启用: 0-禁用, 1-启用
     */
    @TableField("enabled")
    private Integer enabled;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(value="create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value="update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
