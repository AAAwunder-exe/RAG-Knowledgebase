package com.enterprise.ai.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Python RAG 引擎索引管理客户端
 * 负责把 Java 侧上传/删除的文档同步到 RAG 索引（对应 main.py 的 /index/add、/index/delete）
 */
@Slf4j
@Service
public class RagIndexClient {

    @Value("${ai.rag.base-url:http://localhost:8001}")
    private String ragBaseUrl;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        // PDF 解析 + 向量化较慢，索引超时放宽到 5 分钟
        factory.setReadTimeout(300000);
        this.restTemplate = new RestTemplate(factory);
        log.info("RAG 索引客户端初始化完成: base-url={}", ragBaseUrl);
    }

    /**
     * 增量加入一个文档到 RAG 索引
     *
     * @param data             文件字节（需在 file.transferTo 之前读取）
     * @param originalFilename 原始文件名
     * @return 入库的片段数和提取出的全文文本（用于回填全文预览）
     */
    public AddResult addDocument(byte[] data, String originalFilename, Long kbId, Long docId, String title) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        });
        body.add("kb_id", String.valueOf(kbId));
        body.add("doc_id", String.valueOf(docId));
        body.add("title", title == null ? "" : title);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        AddResult result = restTemplate.postForObject(ragBaseUrl + "/index/add", entity, AddResult.class);
        if (result == null) {
            throw new IllegalStateException("RAG /index/add 返回空响应");
        }
        log.info("文档已同步到 RAG 索引: docId={}, chunkCount={}", docId, result.getChunkCount());
        return result;
    }

    /**
     * 从 RAG 索引中删除一个文档的全部片段
     */
    public DeleteResult deleteDocument(Long docId) {
        Map<String, Object> body = new HashMap<>();
        body.put("doc_id", docId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(ragBaseUrl + "/index/delete", entity, DeleteResult.class);
    }


    /** 对应 main.py /index/add 的响应 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddResult {
        @JsonProperty("chunk_count")
        private Integer chunkCount;
        private String text;
        private String file;
    }

    /** 对应 main.py /index/delete 的响应 */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeleteResult {
        @JsonProperty("doc_id")
        private Long docId;
        @JsonProperty("removed_chunks")
        private Integer removedChunks;
    }
}
