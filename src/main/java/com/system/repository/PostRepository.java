package com.system.repository;


import com.system.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByVisibilityAndStatusOrderByIdDesc(Post.Visibility visibility, Post.PostStatus status);
    Page<Post> findByVisibilityAndStatusAndTitleContainingIgnoreCase(
            Post.Visibility visibility,
            Post.PostStatus status,
            String title,
            Pageable pageable);
}
