package com.system.controller;

import com.system.model.Order;
import com.system.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Buyer bấm mua
    @PostMapping("/buy/{postId}")
    public String buyPost(@PathVariable Long postId,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {
        try {
            orderService.createOrder(postId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán thành công! Tiền đã được đưa vào trạng thái tạm giữ.");
            return "redirect:/orders/my-purchases";
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Số dư ví không đủ")) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Số dư ví không đủ. Vui lòng nạp thêm tiền để tiếp tục thanh toán.");
                return "redirect:/wallet/deposit";
            }
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
    }

    // Buyer xác nhận nhận hàng thành công
    @PostMapping("/{orderId}/confirm")
    public String confirmReceived(@PathVariable Long orderId,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            orderService.confirmReceived(orderId, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận thành công! Tiền đã được chuyển cho người bán.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/orders/my-purchases";
    }

    // Danh sách đơn mua của buyer
    @GetMapping("/my-purchases")
    public String myPurchases(Model model, Principal principal) {
        List<Order> orders = orderService.getBuyerOrders(principal.getName());
        model.addAttribute("orders", orders);
        model.addAttribute("pageTitle", "Đơn hàng đã mua");
        return "order/my-purchases";
    }

    // Danh sách đơn bán của seller
    @GetMapping("/my-sales")
    public String mySales(Model model, Principal principal) {
        List<Order> orders = orderService.getSellerOrders(principal.getName());
        model.addAttribute("orders", orders);
        model.addAttribute("pageTitle", "Đơn hàng đã bán");
        return "order/my-sales";
    }

    // Chi tiết đơn hàng
    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable Long orderId,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.getOrderDetail(orderId, principal.getName());
            model.addAttribute("order", order);

            boolean canViewHiddenInfo =
                    order.getBuyer().getUsername().equals(principal.getName())
                            && (order.getStatus() == Order.OrderStatus.ESCROWED
                            || order.getStatus() == Order.OrderStatus.COMPLETED
                            || order.getStatus() == Order.OrderStatus.DISPUTED
                            || order.getStatus() == Order.OrderStatus.ADMIN_INTERVENTION);

            model.addAttribute("canViewHiddenInfo", canViewHiddenInfo);

            return "order/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
    }
}