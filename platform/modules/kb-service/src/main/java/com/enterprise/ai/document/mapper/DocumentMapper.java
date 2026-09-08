package com.enterprise.ai.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.ai.document.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper 接口
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
