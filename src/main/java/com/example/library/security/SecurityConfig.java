package com.example.library.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置 — JWT 无状态认证 + 方法级鉴权。
 *
 * <p>核心设计：
 * <ul>
 *   <li><b>无状态 Session</b>：SessionCreationPolicy.STATELESS，不创建 HttpSession</li>
 *   <li><b>CSRF 禁用</b>：前后端分离 + JWT 天然防 CSRF</li>
 *   <li><b>白名单</b>：登录、注册、Swagger 文档等无需认证</li>
 *   <li><b>JWT 过滤器</b>：在 UsernamePasswordAuthenticationFilter 之前执行</li>
 *   <li><b>方法级鉴权</b>：@EnableMethodSecurity 开启 @PreAuthorize</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 无需认证的白名单路径 */
    private static final String[] WHITE_LIST = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/admin/login",
            "/api/v1/auth/refresh",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/actuator/health",
            "/error"
    };

    /**
     * 安全过滤链配置。
     *
     * <p>请求处理流程：
     * <ol>
     *   <li>CSRF 过滤器（已禁用）</li>
     *   <li>CORS 过滤器（允许前端跨域）</li>
     *   <li>JwtAuthenticationFilter → 校验 Token，设置 SecurityContext</li>
     *   <li>AuthorizationFilter → 检查 URL 权限</li>
     *   <li>@PreAuthorize → 检查方法级权限</li>
     * </ol>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离 + JWT 已具备 CSRF 防护）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态 Session
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // URL 权限配置
                .authorizeHttpRequests(auth -> auth
                        // 白名单 — 无需认证
                        .requestMatchers(WHITE_LIST).permitAll()
                        // OPTIONS 预检请求放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 管理员接口 — 需 ADMIN 角色
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // 其余接口需认证
                        .anyRequest().authenticated()
                )
                // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 异常处理：未认证返回 401，权限不足返回 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":10002,\"message\":\"未登录或Token已过期\",\"data\":null}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":10003,\"message\":\"权限不足\",\"data\":null}");
                        })
                );

        return http.build();
    }

    /**
     * BCrypt 密码编码器，强度 cost=12。
     * cost=12 在安全和性能之间取得平衡，每次哈希约 200-300ms。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * CORS 跨域配置 — 允许前端开发服务器（localhost:5173）跨域请求。
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // 开发阶段允许所有来源
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Request-Id", "X-RateLimit-Remaining", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
