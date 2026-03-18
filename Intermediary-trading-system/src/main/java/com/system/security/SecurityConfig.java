package com.system.security;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Thuật toán mã hóa mật khẩu 1 chiều (Bắt buộc phải có để bảo mật)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Cấp quyền truy cập tự do cho file tĩnh (CSS, JS) và trang đăng nhập/đăng ký
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/register").permitAll()
                        // Yêu cầu quyền ADMIN cho các URL bắt đầu bằng /admin
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Tất cả các URL còn lại đều yêu cầu phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login") // Báo cho Spring biết URL của trang đăng nhập
                        .loginProcessingUrl("/process-login") // URL mà form HTML sẽ submit lên (Spring tự xử lý)
                        .defaultSuccessUrl("/", true) // Đăng nhập thành công thì về trang chủ
                        .failureUrl("/login?error=true") // Đăng nhập sai thì reload lại kèm tham số báo lỗi
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}