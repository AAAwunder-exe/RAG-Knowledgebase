# ==================== 多阶段构建 | 微服务模块通用 Dockerfile ====================
# 通过 --build-arg MODULE 指定构建哪个服务：
#   auth-service（认证域，8081）/ kb-service（知识域，8082）/ gateway（网关，8080）
# 依赖关系：common 模块通过 reactor 构建自动带上（mvn -pl modules/<MODULE> -am）
#
# 构建示例：
#   docker build --build-arg MODULE=auth-service -t ai-auth-service .
#   docker build --build-arg MODULE=kb-service   -t ai-kb-service .
#   docker build --build-arg MODULE=gateway      -t ai-gateway .

ARG MODULE=gateway

# ==================== Stage 1: Maven 构建 ====================
FROM maven:3.9-eclipse-temurin-17 AS builder
ARG MODULE
WORKDIR /app

# 复制父 POM 与全部模块源码（仅构建目标模块，依赖自动解析）
COPY pom.xml .
COPY modules/ modules/

# 只构建目标模块（含依赖模块 common），跳过测试
RUN mvn -pl modules/${MODULE} -am clean package -DskipTests -B

# ==================== Stage 2: 运行时镜像 ====================
FROM eclipse-temurin:17-jre-alpine
ARG MODULE
ENV TZ=Asia/Shanghai
# fontconfig/ttf-dejavu 供验证码 AWT 绘制文字使用（auth-service 需要）
RUN apk add --no-cache tzdata curl fontconfig ttf-dejavu && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 上传目录（kb-service 落盘用，compose 挂 uploads 卷）
RUN mkdir -p /app/uploads

# 从构建阶段复制目标模块 JAR
COPY --from=builder /app/modules/${MODULE}/target/${MODULE}-*.jar app.jar

# 端口由各服务 application.yml 决定（8080/8081/8082），EXPOSE 仅为文档说明
EXPOSE 8080 8081 8082

# 默认使用生产环境配置
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar", \
    "--spring.profiles.active=prod"]
