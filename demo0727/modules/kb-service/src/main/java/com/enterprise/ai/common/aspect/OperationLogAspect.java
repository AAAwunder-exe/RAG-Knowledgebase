package com.enterprise.ai.common.aspect;

import com.enterprise.ai.common.annotation.OperationLog;
import com.enterprise.ai.security.context.SecurityContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final SecurityContextUtils securityContextUtils;

    public OperationLogAspect(SecurityContextUtils securityContextUtils) {
        this.securityContextUtils = securityContextUtils;
    }

    @Around("@annotation(com.enterprise.ai.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog operationLog = method.getAnnotation(OperationLog.class);

        String className = point.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        String username = securityContextUtils.getCurrentUsername();
        String ip = getClientIP();
        LocalDateTime startTime = LocalDateTime.now();

        log.info("【操作日志】模块: {} | 操作: {} | 用户: {} | IP: {} | 类: {}.{}",
                operationLog.module(),
                operationLog.value(),
                username,
                ip,
                className,
                methodName);

        if (operationLog.recordParams()) {
            Object[] args = point.getArgs();
            log.debug("【请求参数】{}", args);
        }

        try {
            Object result = point.proceed();

            if (operationLog.recordResult()) {
                log.debug("【返回结果】{}", result);
            }

            return result;
        } catch (Throwable e) {
            log.error("【操作异常】模块: {} | 操作: {} | 异常: {}",
                    operationLog.module(),
                    operationLog.value(),
                    e.getMessage(), e);
            throw e;
        }
    }

    private String getClientIP() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.warn("获取客户端 IP 失败", e);
        }
        return "unknown";
    }
}
