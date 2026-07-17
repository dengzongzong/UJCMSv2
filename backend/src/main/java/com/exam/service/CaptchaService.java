package com.exam.service;

/**
 * 图形验证码 Service
 * <p>前端流程:</p>
 * <ol>
 *   <li>GET /public/captcha/generate 拿到 { captchaKey, imageBase64, expireMillis }</li>
 *   <li>注册/重置密码时, body 里带 { captchaKey, captchaCode }</li>
 *   <li>后端一次性消费 captchaKey, 校验通过放行, 不通过抛 "验证码错误或已过期"</li>
 * </ol>
 */
public interface CaptchaService {

    /**
     * 生成图形验证码,返回 captchaKey + base64 图 + 过期时间(毫秒)
     */
    CaptchaResult generate();

    /**
     * 验证图形验证码(一次性消费)
     * @param captchaKey 生成时返回的 key
     * @param code       用户输入的字符
     * @return 验证通过 true, 不通过 false
     */
    boolean verify(String captchaKey, String code);

    /**
     * 验证图形验证码,失败抛 BusinessException
     */
    void assertValid(String captchaKey, String code);
}
