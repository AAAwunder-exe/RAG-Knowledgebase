#!/bin/bash
# ==================== 部署脚本 ====================
# 企业级 AI 知识管理平台
# 用法: ./deploy.sh [build|up|down|logs|restart|status|clean]

set -e

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 项目名称
PROJECT_NAME="ai-platform"

# 打印带颜色的消息
print_message() {
    echo -e "${GREEN}[${PROJECT_NAME}]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[警告]${NC} $1"
}

print_error() {
    echo -e "${RED}[错误]${NC} $1"
}

# 检查 Docker 是否运行
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker 未运行，请先启动 Docker"
        exit 1
    fi
}

# 检查 Docker Compose 是否可用
check_docker_compose() {
    if ! docker compose version > /dev/null 2>&1; then
        print_error "Docker Compose 不可用，请安装 Docker Compose V2"
        exit 1
    fi
}

# 构建镜像
build() {
    print_message "开始构建镜像..."
    docker compose build --no-cache
    print_message "镜像构建完成"
}

# 启动服务
up() {
    print_message "启动所有服务..."
    docker compose up -d
    print_message "服务启动完成"
    show_status
}

# 停止服务
down() {
    print_message "停止所有服务..."
    docker compose down
    print_message "服务已停止"
}

# 重启服务
restart() {
    print_message "重启所有服务..."
    docker compose down
    docker compose up -d
    show_status
}

# 查看日志
logs() {
    local service=$1
    if [ -n "$service" ]; then
        docker compose logs -f --tail=100 "$service"
    else
        docker compose logs -f --tail=100
    fi
}

# 查看状态
show_status() {
    print_message "当前服务状态:"
    echo ""
    docker compose ps
}

# 清理所有容器和镜像
clean() {
    print_warning "此操作将删除所有容器、镜像和数据卷，确定吗？(y/N)"
    read -r confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        print_message "清理中..."
        docker compose down -v --rmi all
        print_message "清理完成"
    else
        print_message "已取消"
    fi
}

# 重新部署（构建+重启）
redeploy() {
    print_message "开始重新部署..."
    docker compose down
    build
    up
    print_message "重新部署完成"
}

# 主函数
main() {
    local command=$1
    local service=$2

    check_docker
    check_docker_compose

    case "$command" in
        build)
            build
            ;;
        up)
            up
            ;;
        down)
            down
            ;;
        restart)
            restart
            ;;
        redeploy)
            redeploy
            ;;
        logs)
            logs "$service"
            ;;
        status)
            show_status
            ;;
        clean)
            clean
            ;;
        *)
            echo "用法: $0 [命令] [服务名]"
            echo ""
            echo "命令列表:"
            echo "  build       构建镜像"
            echo "  up          启动服务"
            echo "  down        停止服务"
            echo "  restart     重启服务"
            echo "  redeploy    重新部署（构建+重启）"
            echo "  logs        查看日志 [服务名]"
            echo "  status      查看服务状态"
            echo "  clean       清理所有容器和数据"
            echo ""
            echo "示例:"
            echo "  $0 build"
            echo "  $0 up"
            echo "  $0 logs app"
            ;;
    esac
}

main "$@"
