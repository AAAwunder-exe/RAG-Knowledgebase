import request from './request'
import type { CaptchaVO } from '@/types'

/** 获取图形验证码 */
export function getCaptcha() {
  return request.get<CaptchaVO, CaptchaVO>('/auth/captcha')
}
