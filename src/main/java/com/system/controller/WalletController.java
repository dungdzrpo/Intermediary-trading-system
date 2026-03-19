package com.system.controller;


import com.system.service.VNPayService;
import com.system.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {


    private final VNPayService vnPayService;


    private final WalletService walletService;

    // 1. Hiển thị trang nhập số tiền muốn nạp
    @GetMapping("/deposit")
    public String showDepositForm() {
        return "wallet/deposit";
    }

    // 2. Nút "Nạp Tiền" form gửi lên -> Chuyển hướng sang VNPay
    @PostMapping("/deposit")
    public String submitDeposit(@RequestParam("amount") int amount, HttpServletRequest request) {
        // Gọi Service tạo link VNPay
        String orderInfo = "Nap tien vao vi Escrow";
        String vnpayUrl = vnPayService.createOrder(amount, orderInfo);

        // Chuyển hướng người dùng sang web của VNPay
        return "redirect:" + vnpayUrl;
    }

    // 3. VNPay trả kết quả về đường link này (URL Return)
    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Principal principal, Model model) {
        // Lấy thông tin từ URL VNPay trả về
        String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
        String vnp_Amount = request.getParameter("vnp_Amount"); // Tiền này đang bị nhân 100

        // vnp_ResponseCode == "00" nghĩa là khách hàng đã thanh toán thành công
        if ("00".equals(vnp_ResponseCode)) {
            // Chia lại cho 100 để ra số tiền thật
            BigDecimal realAmount = new BigDecimal(vnp_Amount).divide(new BigDecimal("100"));
            String username = principal.getName(); // Lấy username đang đăng nhập

            // Gọi Service cộng tiền vào DB
            walletService.addMoney(username, realAmount, "Nạp tiền qua VNPay");

            model.addAttribute("message", "Nạp thành công " + realAmount + " VNĐ vào ví!");
            return "wallet/success";
        } else {
            model.addAttribute("error", "Giao dịch thất bại hoặc bị hủy!");
            return "wallet/deposit";
        }
    }
}
