package com.system.model;



import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết nhiều bài đăng thuộc về 1 người bán
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Thông tin ẩn, chỉ hiện ở Stage 4
    @Column(columnDefinition = "TEXT")
    private String hiddenInfo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeePayer feePayer;

    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum FeePayer {
        SELLER, BUYER
    }

    public enum PostStatus {
        DRAFT, ACTIVE, LOCKED, CANCELLED
    }
    public enum Visibility {
        PUBLIC,     // Công khai trên chợ
        UNLISTED    // Ẩn, chỉ có link mới vào được
    }


    @Enumerated(EnumType.STRING)
    private Visibility visibility;
}