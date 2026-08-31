package com.enterprise.ai.document.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.document.dto.DocumentUploadDTO;
import com.enterprise.ai.document.dto.DocumentUpdateDTO;
import com.enterprise.ai.document.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 上传文档
     */
    DocumentVO uploadDocument(DocumentUploadDTO dto, MultipartFile file);

    /**
     * 分页查询知识库下的文档
     */
    Page<DocumentVO> pageDocumentsByKnowledgeId(Long knowledgeId, Integer current, Integer size, String keyword);

    /**
     * 根据 ID 获取文档
     */
    DocumentVO getDocumentById(Long id);

    /**
     * 获取文档内容
     */
    String getDocumentContent(Long id);

    /**
     * 更新文档信息
     */
    DocumentVO updateDocument(Long id, DocumentUpdateDTO dto);

    /**
     * 删除文档
     */
    void deleteDocument(Long id);
}
