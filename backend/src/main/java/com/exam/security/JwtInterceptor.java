package com.exam.security;

import com.exam.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 路径前缀 -> 允许的角色列表(白名单之外的受保护接口,按此规则校验角色)
     *
     * <p>规则:
     * <ul>
     *   <li>/admin/**  -> admin</li>
     *   <li>/user/**   -> student</li>
     *   <li>/portal/** -> student</li>
     *   <li>其它受保护接口 -> 不限(任何已登录用户)</li>
     * </ul>
     * </p>
     *
     * <p>WebMvcConfig 的 excludePathPatterns 已经放行白名单,这里的 role 检查只对受保护路径生效。</p>
     */
    private static final List<Map.Entry<String, String>> PATH_ROLE_RULES = java.util.Arrays.asList(
            new java.util.AbstractMap.SimpleEntry<>("/admin/",  "admin"),
            new java.util.AbstractMap.SimpleEntry<>("/user/",   "student"),
            new java.util.AbstractMap.SimpleEntry<>("/portal/", "student")
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 也支持 query string 中的 token(给 <img src> 这类不能带 header 的场景用)
        // 用法:/xxx?token=<jwt>;后端会在校验后从 url 中清除,不污染后端日志
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }

        // 检查路径是否在白名单(可选 token 透传)
        // 注意:这里要剥离 servlet context-path(/api),只保留 servlet 内部路径
        String requestUri = stripContextPath(request.getRequestURI(), request.getContextPath());
        boolean isPublic = isPublicPath(requestUri);

        if (token == null || token.isEmpty()) {
            // 没 token: 白名单路径放行(继续执行,userId 不会被设置);
            // 其它路径直接 401
            if (isPublic) {
                return true;
            }
            writeError(response, 401, "未登录，请先登录");
            return false;
        }

        if (!jwtUtil.isTokenValid(token)) {
            // token 无效: 白名单路径放行(当作未登录),其它路径 401
            if (isPublic) {
                return true;
            }
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        }

        Claims claims = jwtUtil.parseTokenIfValid(token);
        Long userId = Long.valueOf(claims.get("userId").toString());
        Object usernameObj = claims.get("username");
        String username = usernameObj != null ? usernameObj.toString() : "";
        String role = claims.get("role") != null ? claims.get("role").toString() : "student";
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role);

        // 白名单路径: 即便有 token 也只用于"获取当前用户上下文",不强制要求角色
        if (isPublic) {
            return true;
        }

        // 受保护路径: 角色越权检查(默认放行未配置前缀的路径)
        String path = requestUri;
        for (Map.Entry<String, String> rule : PATH_ROLE_RULES) {
            if (path.startsWith(rule.getKey())) {
                if (!rule.getValue().equals(role)) {
                    log.warn("角色越权: userId={}, username={}, role={}, path={}, required={}",
                            userId, username, role, path, rule.getValue());
                    writeError(response, 403, "无权访问此接口");
                    return false;
                }
                break;
            }
        }
        return true;
    }

    /**
     * 去掉 servlet context-path 前缀(例如 /api),返回 servlet 内部路径
     */
    private String stripContextPath(String uri, String contextPath) {
        if (contextPath == null || contextPath.isEmpty() || "/".equals(contextPath)) {
            return uri;
        }
        if (uri.startsWith(contextPath)) {
            String stripped = uri.substring(contextPath.length());
            return stripped.isEmpty() ? "/" : stripped;
        }
        return uri;
    }

    /**
     * 判断当前请求路径是否在白名单(白名单路径可访问,带 token 时也会解析上下文)
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/")
                || path.startsWith("/public/")
                || path.equals("/user/course/public/list")
                || path.equals("/user/exam/public/list")
                || path.startsWith("/user/live/public/")
                || path.startsWith("/portal/certificate/")
                || path.startsWith("/uploads/")
                || path.startsWith("/static/")
                || path.startsWith("/ws/")
                || path.equals("/error");
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        PrintWriter writer = response.getWriter();
        // 401 -> unauthorized(后端 code 401), 403 -> forbidden(后端 code 403)
        Result<?> result = (code == 401)
                ? Result.unauthorized(message)
                : Result.forbidden(message);
        writer.write(objectMapper.writeValueAsString(result));
        writer.flush();
    }
}
