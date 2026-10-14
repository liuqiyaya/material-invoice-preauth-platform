package com.liuqiya.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuqiya.invoice.pojo.entity.InvoiceItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoiceItemMapper extends BaseMapper<InvoiceItem> {
}
