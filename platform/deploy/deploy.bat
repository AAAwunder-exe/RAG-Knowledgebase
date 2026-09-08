@echo off
REM ==================== 部署脚本 (Windows) ====================
REM 企业级 AI 知识管理平台
REM 用法: deploy.bat [build|up|down|logs|restart|status|clean]

chcp 65001 >nul

setlocal enabledelayedexpansion

set "PROJECT_NAME=ai-platform"

if "%1"=="" goto :show_usage

if "%1"=="build" goto :build
if "%1"=="up" goto :up
if "%1"=="down" goto :down
if "%1"=="restart" goto :restart
if "%1"=="redeploy" goto :redeploy
if "%1"=="logs" goto :logs
if "%1"=="status" goto :status
if "%1"=="clean" goto :clean

goto :show_usage

:check_docker
docker info >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未运行，请先启动 Docker
    exit /b 1
)
exit /b 0

:check_compose
docker compose version >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker Compose 不可用
    exit /b 1
)
exit /b 0

:build
call :check_docker || goto :eof
call :check_compose || goto :eof
echo [ai-platform] 开始构建镜像...
docker compose build --no-cache
echo [ai-platform] 镜像构建完成
goto :eof

:up
call :check_docker || goto :eof
call :check_compose || goto :eof
echo [ai-platform] 启动所有服务...
docker compose up -d
echo [ai-platform] 服务启动完成
goto :status

:down
call :check_docker || goto :eof
call :check_compose || goto :eof
echo [ai-platform] 停止所有服务...
docker compose down
echo [ai-platform] 服务已停止
goto :eof

:restart
call :check_docker || goto :eof
call :check_compose || goto :eof
echo [ai-platform] 重启所有服务...
docker compose down
docker compose up -d
goto :status

:redeploy
call :check_docker || goto :eof
call :check_compose || goto :eof
echo [ai-platform] 开始重新部署...
docker compose down
call :build
goto :up

:logs
call :check_docker || goto :eof
call :check_compose || goto :eof
if "%2"=="" (
    docker compose logs -f --tail=100
) else (
    docker compose logs -f --tail=100 %2
)
goto :eof

:status
call :check_docker || goto :eof
call :check_compose || goto :eof
echo [ai-platform] 当前服务状态:
echo.
docker compose ps
goto :eof

:clean
call :check_docker || goto :eof
call :check_compose || goto :eof
echo.
echo 此操作将删除所有容器、镜像和数据卷，确定吗？
set /p "confirm=输入 Y 确认, 其他键取消: "
if /i "%confirm%"=="y" (
    echo [ai-platform] 清理中...
    docker compose down -v --rmi all
    echo [ai-platform] 清理完成
) else (
    echo 已取消
)
goto :eof

:show_usage
echo.
echo 企业级 AI 知识管理平台 - Docker 部署脚本
echo ============================================
echo.
echo 用法: deploy.bat [命令] [服务名]
echo.
echo 命令列表:
echo   build       构建镜像
echo   up          启动服务
echo   down        停止服务
echo   restart     重启服务
echo   redeploy    重新部署（构建+重启）
echo   logs        查看日志 [服务名]
echo   status      查看服务状态
echo   clean       清理所有容器和数据
echo.
echo 示例:
echo   deploy.bat build
echo   deploy.bat up
echo   deploy.bat logs app
echo.
goto :eof

:eof
endlocal
