package com.enterprise.ai.captcha.service.impl;

import com.enterprise.ai.captcha.service.CaptchaService;
import com.enterprise.ai.captcha.vo.CaptchaVO;
import com.enterprise.ai.common.result.BusinessException;
import com.enterprise.ai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现
 * 基于 java.awt 手写绘制，不引入第三方依赖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate redisTemplate;

    /** Redis 中验证码的 key 前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    /** 验证码有效期（秒） */
    private static final long CAPTCHA_TTL_SECONDS = 300L;

    /** 验证码字符集（剔除 0/O/1/I/l 等易混淆字符） */
    private static final char[] CHARS = {
        '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    };
    private static final int CODE_LENGTH = 4;

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    @Override
    public CaptchaVO generate() {
        String code = randomCode();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        String base64 = render(code);
        // 存储验证码（区分大小写存入，校验时忽略大小写）
        redisTemplate.opsForValue().set(
            CAPTCHA_KEY_PREFIX + uuid, code, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("生成验证码 uuid={}, code={}", uuid, code);
        return new CaptchaVO(uuid, base64);
    }

    @Override
    public void validate(String uuid, String code) {
        if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }

        String key = CAPTCHA_KEY_PREFIX + uuid;
        String stored = redisTemplate.opsForValue().get(key);
        // 无论成败都删除，保证单次使用
        redisTemplate.delete(key);

        if (stored == null) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
        }
        if (!stored.equalsIgnoreCase(code.trim())) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }
    }

    private String randomCode() {
        Random random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    /**
     * 将验证码绘制为 PNG 图片并返回裸 base64
     */
    private String render(String code) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            // 抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 背景
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            Random random = ThreadLocalRandom.current();

            // 干扰线
            for (int i = 0; i < 4; i++) {
                g.setColor(randomColor(120, 200));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            // 噪点
            for (int i = 0; i < 40; i++) {
                g.setColor(randomColor(120, 220));
                g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
            }

            // 绘制字符（逐个随机旋转）
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 26);
            g.setFont(font);
            int startX = 8;
            int baseY = 28;
            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                g.setColor(randomColor(30, 120));
                // 随机旋转 -25° ~ 25°
                double angle = (random.nextDouble() - 0.5) * 0.9;
                AffineTransform old = g.getTransform();
                g.rotate(angle, startX + i * 28 + 7, baseY);
                g.drawString(String.valueOf(c), startX + i * 28, baseY);
                g.setTransform(old);
            }

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.error("验证码图片生成失败", e);
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }
    }

    private Color randomColor(int min, int max) {
        Random random = ThreadLocalRandom.current();
        return new Color(min + random.nextInt(max - min), min + random.nextInt(max - min), min + random.nextInt(max - min));
    }
}
