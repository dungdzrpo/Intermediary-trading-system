package com.system.controller;


import lombok.RequiredArgsConstructor;
import com.system.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.system.repository.UserRepository;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class HomeController {


    private final UserRepository userRepository;

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        // Principal chứa thông tin username của người đang đăng nhập
        if (principal != null) {
            String username = principal.getName();
            User currentUser = userRepository.findByUsername(username).orElse(null);

            // Đẩy object user ra giao diện Thymeleaf
            model.addAttribute("currentUser", currentUser);
        }

        return "index"; // Trả về file index.html
    }
}
