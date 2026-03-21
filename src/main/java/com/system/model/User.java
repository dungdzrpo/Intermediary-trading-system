package com.system.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data // Tự động sinh Getter/Setter nhờ Lombok
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Số dư khả dụng (Mặc định bằng 0)
    @Column(precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // Số dư đang bị đóng băng (Ví dụ: 50k khiếu nại)
    @Column(precision = 15, scale = 2)
    private BigDecimal holdBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Role role = Role.ROLE_USER;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role {
        ROLE_USER, ROLE_ADMIN
    }
}