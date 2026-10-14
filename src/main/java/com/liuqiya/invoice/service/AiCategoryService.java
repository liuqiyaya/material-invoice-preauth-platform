package com.liuqiya.invoice.service;

import com.liuqiya.invoice.pojo.dto.AiCategoryResult;

public interface AiCategoryService {
    /**
     * 判断单个商品的品类
     *
     * @param itemName 商品名称
     * @return 三态结果
     */
    AiCategoryResult judge(String itemName);
}
