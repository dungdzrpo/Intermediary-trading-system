package com.system.controller;

import com.system.model.Dispute;
import com.system.model.DisputeMessage;
import com.system.model.User;
import com.system.repository.PostRepository;
import com.system.repository.UserRepository;
import com.system.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @GetMapping
    public String listDisputes(Model model, Principal principal) {
        String username = principal.getName();
        User currentUser = getCurrentUser(username);
        boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

        List<Dispute> disputes = disputeService.getDisputesForUser(username, isAdmin);
        model.addAttribute("disputes", disputes);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUser", currentUser);

        return "dispute/list";
    }

    @GetMapping("/{id}")
    public String disputeDetail(@PathVariable Long id, Model model, Principal principal) {
        String username = principal.getName();
        User currentUser = getCurrentUser(username);
        boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

        Dispute dispute = disputeService.getDisputeForView(id, username, isAdmin);
        List<DisputeMessage> messages = disputeService.getMessages(id, username, isAdmin);

        model.addAttribute("dispute", dispute);
        model.addAttribute("messages", messages);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUser", currentUser);

        return "dispute/detail";
    }

    @PostMapping("/create")
    public String createDispute(@RequestParam("orderId") Long orderId,
                                @RequestParam("reason") String reason,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(principal.getName());
            if (currentUser.getRole() == User.Role.ROLE_ADMIN) {
                throw new IllegalStateException("Admin khong duoc tao khieu nai. Hay dang nhap tai khoan Buyer.");
            }

            disputeService.createDispute(orderId, principal.getName(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Tao khieu nai thanh cong.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/disputes";
    }

    @PostMapping("/{id}/seller-accept")
    public String sellerAccept(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            disputeService.sellerAccept(id, principal.getName());

            redirectAttributes.addFlashAttribute("successMessage", "Seller da chap nhan khieu nai, don da huy va hoan tien.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/disputes/" + id;
    }

    @PostMapping("/{id}/seller-reject")
    public String sellerReject(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            disputeService.sellerReject(id, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Seller da tu choi. Vu viec chuyen qua Admin phan xu va da hold 50.000d moi ben.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/disputes/" + id;
    }

    @PostMapping("/{id}/admin-decision")
    public String adminDecision(@PathVariable Long id,
                                @RequestParam("winner") String winner,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            if ("BUYER".equalsIgnoreCase(winner)) {
                disputeService.adminResolveForBuyer(id, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Admin da phan xu Buyer thang.");
            } else if ("SELLER".equalsIgnoreCase(winner)) {
                disputeService.adminResolveForSeller(id, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "Admin da phan xu Seller thang.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Gia tri winner khong hop le. Chi nhan BUYER hoac SELLER.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/disputes/" + id;
    }

    @PostMapping("/{id}/messages")
    public String sendMessage(@PathVariable Long id,
                              @RequestParam("content") String content,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            disputeService.addMessage(id, principal.getName(), content);
            redirectAttributes.addFlashAttribute("successMessage", "Da gui tin nhan vao group chat.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/disputes/" + id;
    }

    private User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user dang dang nhap."));
    }
}
