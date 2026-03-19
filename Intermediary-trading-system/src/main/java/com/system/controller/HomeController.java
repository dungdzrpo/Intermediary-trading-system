package com.system.controller;


import com.system.model.Post;
import com.system.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.system.model.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.system.repository.UserRepository;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {


    private final UserRepository userRepository;
    private final PostService postService;

    // Sửa lại hàm GET map "/" như sau:
    @GetMapping("/")
    public String home(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size, // THÊM DÒNG NÀY: Mặc định hiện 5 dòng
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sort,
            Model model, Principal principal, HttpServletRequest request) {

        if (principal != null) {
            if (request.isUserInRole("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            }
            User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
            model.addAttribute("currentUser", currentUser);
        }

        // Truyền biến 'size' vào thay vì fix cứng số 10
        Page<Post> postPage = postService.getMarketplacePosts(page, size, keyword, sort);

        model.addAttribute("posts", postPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("size", size); // Đẩy biến size ra HTML để select box chọn đúng

        return "index";
    }
}
