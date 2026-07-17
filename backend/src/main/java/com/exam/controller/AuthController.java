package com.exam.controller;

import com.exam.common.BusinessException;
import com.exam.common.Result;
import com.exam.dto.ChangePasswordDTO;
import com.exam.dto.ChooseSubjectDTO;
import com.exam.dto.LoginDTO;
import com.exam.dto.RegisterDTO;
import com.exam.dto.ResetPasswordDTO;
import com.exam.dto.SendCodeDTO;
import com.exam.security.JwtUtil;
import com.exam.service.AuthService;
import com.exam.service.CaptchaService;
import com.exam.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO dto) {
        // 校验图形验证码
        captchaService.assertValid(dto.getCaptchaKey(), dto.getCaptchaCode());
        authService.register(dto);
        return Result.success();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto,
                                     HttpServletRequest request) {
        // 绝对规则: 改密必须带有效 token, 不带一律拒绝
        // 改密入口限定为:
        //   1) 学员"我的"页: POST /auth/change-password (已登录 + 原密码)
        //   2) 管理员后台:   PUT /admin/student/{id} (管理员 token)
        // POST /auth/reset-password 接口已彻底关闭:
        //   1) 前端登录页"忘记密码"入口已关闭
        //   2) 后端这里再防御一次: 任何不带 token 的请求一律拒绝
        //   3) 防止被外部利用: 即便知道接口地址 + 手机号, 无 token 也无法改密
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("未登录,不允许修改密码");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            throw new BusinessException("登录已过期,请重新登录");
        }
        // 只允许学员角色: 学员"忘记密码"流程已被前端关闭,
        // 这里再防御: 即便已登录, 也必须走 /auth/change-password(校验原密码)
        String role = jwtUtil.getRole(token);
        if (!"student".equals(role)) {
            throw new BusinessException("该接口已废弃,管理员改密请用后台");
        }
        // 学员"忘记密码"流程已废弃: 即便带 token, 也不再走未校验原密码的改密
        // 学员改密必须用 POST /auth/change-password (需原密码)
        throw new BusinessException("该接口已废弃,请通过\"我的\"页修改密码");
    }

    /**
     * 登录态修改密码(学员端"我的"页入口)
     * <p>必须带 token, 通过 token 解析 studentId, 需要校验原密码</p>
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto,
                                       HttpServletRequest request) {
        // 必须已登录
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("请先登录");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            throw new BusinessException("登录已过期,请重新登录");
        }
        // 只允许学员角色(管理员改密走后台 /admin/student/{id})
        String role = jwtUtil.getRole(token);
        if (!"student".equals(role)) {
            throw new BusinessException("该接口仅供学员使用,管理员请在后台改密");
        }
        Long studentId = jwtUtil.getUserId(token);
        authService.changePassword(studentId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }

    /**
     * 发送短信验证码(已废弃,改用图形验证码 GET /public/captcha/generate)
     * <p>保留此接口避免老客户端报错,但不再发送短信,统一返回提示</p>
     */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody @Valid SendCodeDTO dto) {
        // 新流程: 已改为图形验证码, 此接口不再实际发送短信
        return Result.success();
    }

    @PostMapping("/choose-subject")
    public Result<Void> chooseSubject(HttpServletRequest request, @RequestBody @Valid ChooseSubjectDTO dto) {
        Long studentId = (Long) request.getAttribute("userId");
        authService.chooseSubject(studentId, dto);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<LoginVO> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return Result.success(authService.getCurrentUserInfo(userId, role));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
