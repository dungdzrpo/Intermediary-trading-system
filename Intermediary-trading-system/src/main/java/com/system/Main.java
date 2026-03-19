package com.system;

import com.system.model.User;
import com.system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("===========================================");
        System.out.println("🚀 HỆ THỐNG ESCROW ĐÃ KHỞI ĐỘNG THÀNH CÔNG!");
        System.out.println("👉 Truy cập: http://localhost:8080");
        System.out.println("===========================================");
    }
    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Nếu chưa có tài khoản 'testuser' thì mới tạo
            if (userRepository.findByUsername("admin").isEmpty()) {
                User user = new User();
                user.setUsername("admin");
                user.setPassword(passwordEncoder.encode("123456")); // Spring sẽ tự băm mật khẩu chuẩn
                user.setEmail("admin@escrow.com");
                user.setBalance(new BigDecimal("500000")); // Cho luôn 500k để test nạp/rút
                user.setRole(User.Role.ROLE_ADMIN);

                userRepository.save(user);
                System.out.println("✅ ĐÃ TẠO TÀI KHOẢN TEST THÀNH CÔNG: testuser / 123456");
            }
        };
    }
}
