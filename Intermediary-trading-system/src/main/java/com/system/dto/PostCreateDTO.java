package com.system.dto;


import com.system.model.Post;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PostCreateDTO {
    private String title;
    private String description;
    private String hiddenInfo; // Thông tin mật (Account game, Link drive...)
    private BigDecimal price;
    private Post.FeePayer feePayer; // Ai chịu phí sàn 5% (SELLER hoặc BUYER)
}
