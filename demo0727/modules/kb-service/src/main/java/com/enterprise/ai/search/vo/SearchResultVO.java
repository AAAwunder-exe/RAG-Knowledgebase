package com.enterprise.ai.search.vo;

import com.enterprise.ai.document.vo.DocumentVO;
import lombok.Data;

/**
 * 搜索结果 VO
 */
@Data
public class SearchResultVO {

    /** 文档信息 */
    private DocumentVO document;

    /** 匹配的高亮文本 */
    private String highlight;

    /** 相关度分数 */
    private Double score;
}
