package com.system.repository;


import com.system.model.Post;
import com.system.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByVisibilityAndStatusOrderByIdDesc(Post.Visibility visibility, Post.PostStatus status);
    Page<Post> findByVisibilityAndStatusAndTitleContainingIgnoreCase(
            Post.Visibility visibility,
            Post.PostStatus status,
            String title,
            Pageable pageable);



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);
    List<Post> findBySellerOrderByIdDesc(User seller);
}
