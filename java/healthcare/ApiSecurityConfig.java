package healthcare;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ApiSecurityConfig {
	@Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            // /openai/** 경로는 이 필터 체인에 의해 처리됩니다.
            .securityMatcher("/openai/**")
//            .authorizeHttpRequests(authorize -> authorize
                // /openai/chat-model 경로는 인증 없이 허용
//                .requestMatchers("/openai/chat-model").permitAll()
                // /openai/** 하위의 다른 경로는 인증 필요
//                .anyRequest().authenticated()
//            )
            .csrf(csrf -> csrf.disable()) // API는 보통 CSRF 비활성화
            // 다른 API 관련 설정 추가 가능
            ;
        return http.build();
    }
}