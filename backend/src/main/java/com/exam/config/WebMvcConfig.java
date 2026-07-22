package com.exam.config;

import com.exam.security.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Value("${upload.path}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注意:这里要带上 context-path 前缀(/api/...),因为 Spring 的 Interceptor
        // 在 addPathPatterns 匹配时,URI 还没被 servlet 剥离 context-path。
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 公开:登录/注册/重置密码/发送验证码
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/reset-password",
                        "/api/auth/send-code",
                        // 公开:专业/公告/新闻/轮播图/关于
                        "/api/public/**",
                        // 公开:课程/考试公开列表
                        "/api/user/course/public/list",
                        "/api/user/exam/public/list",
                        // 公开:学员端证书查询/下载(凭身份证+姓名双因子,无需登录)
                        "/api/portal/certificate/**",
                        // 静态资源
                        "/api/uploads/**",
                        "/api/static/**",
                        "/api/ws/**",
                        "/api/error"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 兼容两种前缀:
        //   1) /uploads/**  (servlet 部署在 / 但通过 nginx 转发 /uploads)
        //   2) /api/uploads/** (servlet context-path = /api)
        // 前端目前走的是 /api/uploads/xxx(apiUrl 强制补了 /api),
        // 这里同时注册两条规则,部署到任意场景都能命中
        registry.addResourceHandler("/uploads/**", "/api/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
        // 静态资源: /static/** 和 /api/static/** (兼容数据库中 /static/xxx 的旧路径)
        // 同时映射到 uploads/ 和 uploads/static/ 两个目录,因为文件实际在 uploads/static/upload/ 下
        registry.addResourceHandler("/static/**", "/api/static/**")
                .addResourceLocations("classpath:/static/", "file:" + uploadPath + "/", "file:" + uploadPath + "/static/");
    }
}
