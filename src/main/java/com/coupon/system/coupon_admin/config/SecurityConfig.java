package com.coupon.system.coupon_admin.config;

// vvv 1. HttpMethod를 꼭 임포트 하세요 vvv
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정을 SecurityConfig에 통합
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 1. CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 2. 폼 로그인/Basic 인증 비활성화
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())

                // 3. 경로별 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // Preflight OPTIONS 요청은 모두 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // "/auth/login" 경로는 인증 없이 무조건 허용
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/coupons/**").authenticated()
                        // HTML 파일들도 허용
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/main.html").authenticated()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new
                CorsConfiguration();

        // 63342 포트와 null(파일 직접 실행)을 허용
        config.setAllowedOrigins(Arrays.asList("http://localhost:63342", "null"));

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // 5. (임시 조치) 비밀번호 암호화 안 함
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}