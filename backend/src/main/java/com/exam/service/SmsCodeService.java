package com.exam.service;

/**
 * 短信验证码服务(内存实现,仅供开发和单实例部署)
 *
 * <p>生产环境应替换为阿里云/腾讯云 SMS SDK 实现,通过配置开关切换。</p>
 *
 * <p>规则:
 * <ul>
 *   <li>每个手机号在 type 维度下保留最新一条验证码</li>
 *   <li>验证码 5 分钟过期</li>
 *   <li>60 秒内不允许重发</li>
 * </ul>
 */
public interface SmsCodeService {
    /**
     * 生成并保存验证码,同时返回明文(给前端/日志用)
     * @param phone 手机号
     * @param type 业务类型:register / reset-password / login
     * @return 验证码明文
     * @throws IllegalStateException 60 秒内重复发送
     */
    String generate(String phone, String type);

    /**
     * 校验验证码是否正确(校验通过后立即失效)
     * @param phone 手机号
     * @param type 业务类型
     * @param code 用户输入的验证码
     * @return true 通过;false 不通过
     */
    boolean verify(String phone, String type, String code);
}
