package com.almagest_dev.tacobank_core_server.infrastructure.config;

import com.almagest_dev.tacobank_core_server.infrastructure.security.authentication.JwtAuthenticationFilter;
import com.almagest_dev.tacobank_core_server.infrastructure.security.authentication.JwtProvider;
import com.almagest_dev.tacobank_core_server.infrastructure.security.handler.CustomAccessDeniedHandler;
import com.almagest_dev.tacobank_core_server.infrastructure.security.handler.CustomAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
//@EnableWebSecurity
@EnableWebSecurity(debug = true)
public class SecurityConfig {
    private final JwtProvider jwtProvider;

    private static final String[] PUBLIC_API_URL = { "/auth/**", "/core/health", "/core/groups/**", "/core/friends/**" }; // 인증 없이도 접근 가능한 경로
    private static final String ADMIN_API_URL = "/admin/**"; // 관리자만 접근 가능한 경로

    public SecurityConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);

        http
                .csrf((csrf) -> csrf.disable()) // CSRF 보호 비활성화
                .cors((cors) -> cors.configurationSource(CorsConfig.corsConfigurationSource())) // CORS 설정
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 비활성화
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(PUBLIC_API_URL).permitAll() // 인증 없이 접근 가능한 경로
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // OPTIONS 요청 허용
                        .requestMatchers(ADMIN_API_URL).hasRole("ADMIN") // Admin 페이지 권한 제한
                        .anyRequest().authenticated()) // 이외 요청은 모두 인증 확인
                .exceptionHandling((e) -> e
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint()) // 인증되지 않은 사용자 접근 혹은 유효한 인증정보 부족한 경우(401 Unauthorized)
                        .accessDeniedHandler(new CustomAccessDeniedHandler()) // 403 Forbidden
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }
}
