package com.system.controller;



import com.system.dto.UserRegisterDTO;
import com.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // Trả về file login.html
    }
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("userDTO", new UserRegisterDTO());
        return "register";
    }

    // 2. Xử lý khi người dùng bấm nút Đăng Ký
    @PostMapping("/register")
    public String processRegister(@ModelAttribute("userDTO") UserRegisterDTO userDTO,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.registerNewUser(userDTO);

            // Nếu thành công, đẩy thông báo sang trang login và chuyển hướng
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";

        } catch (Exception e) {
            // Nếu có lỗi (trùng tên, sai pass...), báo lỗi và giữ người dùng lại trang đăng ký
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }
}