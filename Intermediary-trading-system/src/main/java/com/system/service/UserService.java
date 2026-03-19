package com.system.service;



import com.system.model.User;
import com.system.model.UserRegisterDTO;
import com.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {


    private final UserRepository userRepository;


    private final PasswordEncoder passwordEncoder;

    public void registerNewUser(UserRegisterDTO dto) throws Exception {
        // 1. Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new Exception("Tên đăng nhập đã có người sử dụng!");
        }

        // 2. Kiểm tra mật khẩu xác nhận
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new Exception("Mật khẩu xác nhận không trùng khớp!");
        }

        // 3. Tạo tài khoản mới
        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setEmail(dto.getEmail());

        // BẮT BUỘC: Mã hóa mật khẩu trước khi lưu vào SQL Server
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Cài đặt mặc định cho user mới
        newUser.setRole(User.Role.ROLE_USER);
        newUser.setBalance(BigDecimal.ZERO);      // Ví lúc mới tạo là 0đ
        newUser.setHoldBalance(BigDecimal.ZERO);

        userRepository.save(newUser);
    }
}
