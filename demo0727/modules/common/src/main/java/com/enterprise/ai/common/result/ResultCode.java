package com.enterprise.ai.common.result;

import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILURE(500, "操作失败"),
    
    // 参数错误 (400-499)
    PARAM_INVALID(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    // 业务错误 (1000-1999)
    USERNAME_EXISTS(1001, "用户名已存在"),
    USERNAME_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ACCOUNT_LOCKED(1004, "账户已锁定"),
    ACCOUNT_DISABLED(1005, "账户已禁用"),
    TOKEN_EXPIRED(1006, "Token 已过期"),
    TOKEN_INVALID(1007, "Token 无效"),
    CAPTCHA_EXPIRED(1008, "验证码已过期，请刷新后重试"),
    CAPTCHA_ERROR(1009, "验证码错误"),
    
    // 权限错误 (2000-2999)
    PERMISSION_DENIED(2001, "权限不足"),
    ROLE_NOT_FOUND(2002, "角色不存在"),
    
    // 知识库错误 (3000-3999)
    KNOWLEDGE_BASE_NOT_FOUND(3001, "知识库不存在"),
    KNOWLEDGE_BASE_NAME_EXISTS(3002, "知识库名称已存在"),
    
    // 文档错误 (4000-4999)
    DOCUMENT_NOT_FOUND(4001, "文档不存在"),
    DOCUMENT_UPLOAD_FAILED(4002, "文档上传失败"),
    DOCUMENT_DELETE_FAILED(4003, "文档删除失败"),
    FILE_TYPE_NOT_SUPPORTED(4004, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(4005, "文件大小超出限制");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
