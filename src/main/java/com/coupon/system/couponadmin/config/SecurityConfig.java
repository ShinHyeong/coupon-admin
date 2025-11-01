package com.coupon.system.couponadmin.config;

// vvv 1. HttpMethod를 꼭 임포트 하세요 vvv

import com.coupon.system.couponadmin.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

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

                //3. 세션 관리 정책을 STATELESS(상태 없음)로 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. 경로별 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // Preflight OPTIONS 요청은 모두 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/index.html").permitAll()

                        // 쿠폰 관리
                        .requestMatchers("/coupons/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/main.html").permitAll()

                        /* 추가 예정 기능 : 아직 개발 안함
                        // 관리자 계정 조회
                        .requestMatchers(HttpMethod.GET, "/admins", "/admins/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // 관리자 계정 생성/수정/삭제
                        .requestMatchers(HttpMethod.POST, "/admins").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/admins/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/admins/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/admins/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/manage.html").permitAll()
                        */

                        .anyRequest().authenticated()
                )

                // Spring Security의 기본 필터(UsernamePasswordAuthenticationFilter)
                // 앞에 내가 만든 'jwtAuthenticationFilter' 추가
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
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