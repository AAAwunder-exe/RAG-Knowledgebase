package com.enterprise.ai.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.ai.ai.client.RagIndexClient;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import com.enterprise.ai.document.dto.DocumentUploadDTO;
import com.enterprise.ai.document.dto.DocumentUpdateDTO;
import com.enterprise.ai.document.entity.Document;
import com.enterprise.ai.document.mapper.DocumentMapper;
import com.enterprise.ai.document.service.DocumentService;
import com.enterprise.ai.document.vo.DocumentVO;
import com.enterprise.ai.knowledge.entity.KnowledgeBase;
import com.enterprise.ai.knowledge.mapper.KnowledgeBaseMapper;
import com.enterprise.ai.security.context.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final SecurityContextUtils securityContextUtils;
    private final RagIndexClient ragIndexClient;

    @Value("${document.upload.path:./uploads}")
    private String uploadPath;

    @Value("${document.upload.allowed-extensions:pdf,doc,docx,md,markdown,txt}")
    private String[] allowedExtensions;

    @Value("${document.upload.max-size-mb:50}")
    private long maxSizeMb;

    private static final int MAX_CONTENT_LENGTH = 10000;

    @Override
    @Transactional
    public DocumentVO uploadDocument(DocumentUploadDTO dto, MultipartFile file) {
        // 验证知识库是否存在
        KnowledgeBase kb = knowledgeBaseMapper.selectById(dto.getKnowledgeId());
        if (kb == null) {
            throw new BusinessException(ResultCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        // 验证文件类型
        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);
        if (!allowedExtensionSet().contains(extension.toLowerCase())) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED, "不支持的文件类型，仅支持: " + String.join(", ", allowedExtensions));
        }

        // 验证文件大小
        if (file.getSize() > maxSizeMb * 1024 * 1024) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED, "文件大小超过限制，最大 " + maxSizeMb + "MB");
        }

        // 读取文件字节（用于搜索）——必须在 transferTo 之前，否则临时文件被移走后拿不到字节
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            throw new BusinessException(ResultCode.DOCUMENT_UPLOAD_FAILED);
        }

        // 保存文件
        String fileName = UUID.randomUUID().toString() + "." + extension;
        String relativePath = "/" + kb.getId() + "/" + fileName;
        Path fullPath = Paths.get(uploadPath, String.valueOf(kb.getId()));

        try {
            Files.createDirectories(fullPath);
            file.transferTo(fullPath.resolve(fileName));
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(ResultCode.DOCUMENT_UPLOAD_FAILED);
        }

        // 仅文本类文件提取搜索内容（显式 UTF-8 解码，避免平台默认编码乱码）；
        // pdf/doc/docx 为二进制格式，跳过本地内容提取，避免存入乱码
        String content = "";
        if (isTextFile(extension)) {
            content = new String(bytes, StandardCharsets.UTF_8);
            // 限制内容长度
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH);
            }
        }

        // 创建文档记录
        Document doc = new Document();
        doc.setKnowledgeId(dto.getKnowledgeId());
        doc.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : originalName);
        doc.setFilePath(relativePath);
        doc.setOriginalName(originalName);
        doc.setType(extension.toLowerCase());
        doc.setSize(file.getSize());
        doc.setCreatorId(securityContextUtils.getCurrentUserId());
        doc.setCreatorName(securityContextUtils.getCurrentUsername());
        doc.setStatus(1);
        doc.setTags(dto.getTags());
        doc.setSummary(dto.getSummary());
        doc.setContent(content);

        documentMapper.insert(doc);

        // ===== 同步到 RAG 索引 =====
        // Python 侧解析全文（PDF/docx 等本地无法提取）→ 切分 → 向量化 → 入库；
        // 同时返回提取的全文，回填 content 供全文预览与搜索
        try {
            RagIndexClient.AddResult ragResult = ragIndexClient.addDocument(
                    bytes, originalName, dto.getKnowledgeId(), doc.getId(), doc.getTitle());
            if (StringUtils.hasText(ragResult.getText())) {
                doc.setContent(ragResult.getText());
                documentMapper.updateById(doc);
            }
            if (ragResult.getChunkCount() == null || ragResult.getChunkCount() == 0) {
                log.warn("文档未索引进 RAG（可能无法解析内容）: {}", originalName);
            }
        } catch (Exception e) {
            log.error("文档索引同步失败，回滚上传: {}", originalName, e);
            // 清理已保存的物理文件，避免残留
            try {
                Files.deleteIfExists(fullPath.resolve(fileName));
            } catch (IOException ignore) {
                log.warn("清理上传文件失败: {}", fullPath.resolve(fileName), ignore);
            }
            throw new BusinessException(ResultCode.DOCUMENT_UPLOAD_FAILED,
                    "文档索引同步失败，请确认 RAG 服务正常后重试");
        }

        // 更新知识库文档数量
        kb.setDocumentCount((kb.getDocumentCount() == null ? 0 : kb.getDocumentCount()) + 1);
        knowledgeBaseMapper.updateById(kb);

        log.info("文档上传成功: {} -> {}", originalName, doc.getId());

        return convertToVO(doc);
    }

    @Override
    public Page<DocumentVO> pageDocumentsByKnowledgeId(Long knowledgeId, Integer current, Integer size, String keyword) {
        Page<Document> page = new Page<>(current, size);
        Page<Document> result = documentMapper.selectPage(page,
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeId, knowledgeId)
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(Document::getTitle, keyword)
                                .or()
                                .like(Document::getOriginalName, keyword))
                        .orderByDesc(Document::getCreateTime));
        return convertToVOPage(result);
    }

    @Override
    public DocumentVO getDocumentById(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        return convertToVO(doc);
    }

    @Override
    public String getDocumentContent(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        return doc.getContent();
    }

    @Override
    @Transactional
    public DocumentVO updateDocument(Long id, DocumentUpdateDTO dto) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        if (StringUtils.hasText(dto.getTitle())) {
            doc.setTitle(dto.getTitle());
        }
        if (dto.getTags() != null) {
            doc.setTags(dto.getTags());
        }
        if (dto.getSummary() != null) {
            doc.setSummary(dto.getSummary());
        }
        if (dto.getStatus() != null) {
            doc.setStatus(dto.getStatus());
        }

        documentMapper.updateById(doc);
        return convertToVO(documentMapper.selectById(id));
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        // 删除文件
        Path filePath = Paths.get(uploadPath, doc.getFilePath().substring(1));
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("物理文件删除失败: {}", filePath, e);
        }

        documentMapper.deleteById(id);

        // 同步删除 RAG 索引中的片段（失败不影响数据库删除，记录告警便于排查）
        try {
            ragIndexClient.deleteDocument(id);
        } catch (Exception e) {
            log.warn("RAG 索引删除失败，docId={}，索引可能残留", id, e);
        }

        // 更新知识库文档数量
        KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKnowledgeId());
        if (kb != null && kb.getDocumentCount() != null) {
            kb.setDocumentCount(Math.max(0, kb.getDocumentCount() - 1));
            knowledgeBaseMapper.updateById(kb);
        }

        log.info("文档删除成功: {}", doc.getOriginalName());
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 根据配置构建允许的扩展名集合（去空白、转小写）
     */
    private Set<String> allowedExtensionSet() {
        return Arrays.stream(allowedExtensions)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }

    /**
     * 是否为纯文本类型（可直接按 UTF-8 解码提取内容）
     */
    private boolean isTextFile(String ext) {
        return "md".equalsIgnoreCase(ext) || "markdown".equalsIgnoreCase(ext) || "txt".equalsIgnoreCase(ext);
    }

    private DocumentVO convertToVO(Document doc) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(doc, vo);
        return vo;
    }

    private Page<DocumentVO> convertToVOPage(Page<Document> page) {
        Page<DocumentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
            .map(this::convertToVO)
            .toList());
        return voPage;
    }
}
