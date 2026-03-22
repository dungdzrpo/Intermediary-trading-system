package com.system.controller;


import com.system.dto.PostCreateDTO;
import com.system.model.Post;
import com.system.repository.PostRepository;
import com.system.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @GetMapping("/{id}")
    public String viewPostDetail(@PathVariable Long id, Model model) throws Exception {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new Exception("Không tìm thấy bài đăng"));

        model.addAttribute("post", post);
        return "post/detail";
    }

    // 1. Hiển thị form đăng bài
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("postDTO", new PostCreateDTO());
        return "post/create";
    }



    // 2. Xử lý dữ liệu gửi lên
    @PostMapping("/create")
    public String processCreatePost(@ModelAttribute("postDTO") PostCreateDTO postDTO,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        try {
            // principal.getName() lấy ra username của người đang đăng nhập
            postService.createPost(postDTO, principal.getName());

            redirectAttributes.addFlashAttribute("successMessage", "Đăng bài thành công! Đã trừ 5.000đ phí dịch vụ.");
            return "redirect:/"; // Về trang chủ

        } catch (Exception e) {
            // Nếu lỗi (ví dụ không đủ tiền), báo lỗi đỏ và giữ lại dữ liệu họ vừa nhập
            model.addAttribute("errorMessage", e.getMessage());
            return "post/create";
        }
    }


}