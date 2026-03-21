package com.system.service;

import com.system.model.Dispute;
import com.system.model.DisputeMessage;
import com.system.model.Order;
import com.system.model.User;
import com.system.model.WalletTransaction;
import com.system.repository.DisputeMessageRepository;
import com.system.repository.DisputeRepository;
import com.system.repository.OrderRepository;
import com.system.repository.UserRepository;
import com.system.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisputeService {

    private static final BigDecimal DISPUTE_HOLD_FEE = new BigDecimal("50000");

    private final DisputeRepository disputeRepository;
    private final DisputeMessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public void createDispute(Long orderId, String buyerUsername, String reason) {
        User buyerUser = getUser(buyerUsername);
        if (buyerUser.getRole() == User.Role.ROLE_ADMIN) {
            throw new IllegalStateException("Admin khong duoc tao khieu nai. Chi Buyer moi duoc tao khieu nai.");
        }

        Order order = orderRepository.findByIdAndBuyerUsername(orderId, buyerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don hang hoac ban khong co quyen tao khieu nai."));

        if (order.getStatus() != Order.OrderStatus.ESCROWED) {
            throw new IllegalStateException("Chi don dang ESCROWED moi duoc tao khieu nai.");
        }

        if (disputeRepository.existsByOrderId(orderId)) {
            throw new IllegalStateException("Don hang nay da co khieu nai truoc do.");
        }

        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) {
            throw new IllegalStateException("Ly do khieu nai khong duoc de trong.");
        }

        Dispute dispute = new Dispute();
        dispute.setOrder(order);
        dispute.setBuyer(order.getBuyer());
        dispute.setSeller(order.getPost().getSeller());
        dispute.setReason(normalizedReason);
        dispute.setStatus(Dispute.DisputeStatus.OPEN);

        order.setStatus(Order.OrderStatus.DISPUTED);
        orderRepository.save(order);
        disputeRepository.save(dispute);

        appendMessage(dispute, order.getBuyer(), "Buyer tao khieu nai: " + normalizedReason);
    }

    @Transactional
    public void sellerAccept(Long disputeId, String sellerUsername) {
        Dispute dispute = getDisputeForSeller(disputeId, sellerUsername);
        if (dispute.getStatus() != Dispute.DisputeStatus.OPEN) {
            throw new IllegalStateException("Trang thai khieu nai hien tai khong cho phep Seller chap nhan.");
        }

        Order order = dispute.getOrder();
        User buyer = dispute.getBuyer();

        buyer.setBalance(buyer.getBalance().add(order.getAmount()));
        userRepository.save(buyer);

        createWalletTransaction(
                buyer,
                order.getAmount(),
                WalletTransaction.TransactionType.REFUND,
                "Hoan tien do Seller chap nhan khieu nai, don #" + order.getId(),
                order.getId()
        );

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        dispute.setStatus(Dispute.DisputeStatus.SELLER_ACCEPTED);
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        appendMessage(dispute, dispute.getSeller(), "Seller chap nhan khieu nai. He thong da huy don va hoan tien cho Buyer.");
    }

    @Transactional
    public void sellerReject(Long disputeId, String sellerUsername) {
        Dispute dispute = getDisputeForSeller(disputeId, sellerUsername);
        if (dispute.getStatus() != Dispute.DisputeStatus.OPEN) {
            throw new IllegalStateException("Trang thai khieu nai hien tai khong cho phep Seller tu choi.");
        }

        applyHoldForAdminReview(dispute);

        dispute.setStatus(Dispute.DisputeStatus.ADMIN_REVIEW);
        disputeRepository.save(dispute);

        Order order = dispute.getOrder();
        order.setStatus(Order.OrderStatus.ADMIN_INTERVENTION);
        orderRepository.save(order);

        appendMessage(dispute, dispute.getSeller(), "Seller tu choi khieu nai. Vu viec da duoc chuyen sang Admin phan xu.");
    }

    @Transactional
    public void adminResolveForBuyer(Long disputeId, String adminUsername) {
        ensureAdmin(adminUsername);
        Dispute dispute = getDisputeForAdmin(disputeId);
        if (dispute.getStatus() != Dispute.DisputeStatus.ADMIN_REVIEW) {
            throw new IllegalStateException("Chi khieu nai dang ADMIN_REVIEW moi duoc phan xu.");
        }

        releaseHoldAndSettle(dispute, true);

        Order order = dispute.getOrder();
        User buyer = dispute.getBuyer();

        buyer.setBalance(buyer.getBalance().add(order.getAmount()));
        userRepository.save(buyer);

        createWalletTransaction(
                buyer,
                order.getAmount(),
                WalletTransaction.TransactionType.REFUND,
                "Admin phan xu Buyer thang, hoan tien don #" + order.getId(),
                order.getId()
        );

        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        dispute.setStatus(Dispute.DisputeStatus.RESOLVED_FOR_BUYER);
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        appendMessage(dispute, getUser(adminUsername), "Admin ket luan Buyer dung. Don da duoc hoan tien cho Buyer.");
    }

    @Transactional
    public void adminResolveForSeller(Long disputeId, String adminUsername) {
        ensureAdmin(adminUsername);
        Dispute dispute = getDisputeForAdmin(disputeId);
        if (dispute.getStatus() != Dispute.DisputeStatus.ADMIN_REVIEW) {
            throw new IllegalStateException("Chi khieu nai dang ADMIN_REVIEW moi duoc phan xu.");
        }

        releaseHoldAndSettle(dispute, false);

        Order order = dispute.getOrder();
        User seller = dispute.getSeller();

        seller.setBalance(seller.getBalance().add(order.getAmount()));
        userRepository.save(seller);

        createWalletTransaction(
                seller,
                order.getAmount(),
                WalletTransaction.TransactionType.ORDER_REVENUE,
                "Admin phan xu Seller thang, cong tien don #" + order.getId(),
                order.getId()
        );

        order.setStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(order);

        dispute.setStatus(Dispute.DisputeStatus.RESOLVED_FOR_SELLER);
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        appendMessage(dispute, getUser(adminUsername), "Admin ket luan Seller dung. Don da duoc cong tien cho Seller.");
    }

    @Transactional
    public void addMessage(Long disputeId, String username, String content) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khieu nai."));

        User sender = getUser(username);
        boolean isParticipant = sender.getId().equals(dispute.getBuyer().getId())
                || sender.getId().equals(dispute.getSeller().getId())
                || sender.getRole() == User.Role.ROLE_ADMIN;

        if (!isParticipant) {
            throw new IllegalStateException("Ban khong duoc phep tham gia group chat cua khieu nai nay.");
        }

        if (sender.getRole() == User.Role.ROLE_ADMIN && dispute.getStatus() != Dispute.DisputeStatus.ADMIN_REVIEW) {
            throw new IllegalStateException("Admin chi tham gia chat khi vu viec da vao ADMIN_REVIEW.");
        }

        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new IllegalStateException("Noi dung tin nhan khong duoc de trong.");
        }

        appendMessage(dispute, sender, normalizedContent);
    }

    public List<Dispute> getDisputesForUser(String username, boolean isAdmin) {
        if (isAdmin) {
            return disputeRepository.findAllByOrderByCreatedAtDesc();
        }
        return disputeRepository.findByBuyerUsernameOrSellerUsernameOrderByCreatedAtDesc(username, username);
    }

    public Dispute getDisputeForView(Long disputeId, String username, boolean isAdmin) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khieu nai."));

        if (!isAdmin) {
            boolean owner = dispute.getBuyer().getUsername().equals(username)
                    || dispute.getSeller().getUsername().equals(username);
            if (!owner) {
                throw new IllegalStateException("Ban khong co quyen xem khieu nai nay.");
            }
        }
        return dispute;
    }

    public List<DisputeMessage> getMessages(Long disputeId, String username, boolean isAdmin) {
        Dispute dispute = getDisputeForView(disputeId, username, isAdmin);
        return messageRepository.findByDisputeOrderByCreatedAtAsc(dispute);
    }

    private Dispute getDisputeForSeller(Long disputeId, String sellerUsername) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khieu nai."));

        if (!dispute.getSeller().getUsername().equals(sellerUsername)) {
            throw new IllegalStateException("Ban khong phai Seller cua don nay.");
        }
        return dispute;
    }

    private Dispute getDisputeForAdmin(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay khieu nai."));
    }

    private void applyHoldForAdminReview(Dispute dispute) {
        if (dispute.isHoldApplied()) {
            return;
        }

        User buyer = dispute.getBuyer();
        User seller = dispute.getSeller();

        if (buyer.getBalance().compareTo(DISPUTE_HOLD_FEE) < 0 || seller.getBalance().compareTo(DISPUTE_HOLD_FEE) < 0) {
            throw new IllegalStateException("Ca Buyer va Seller phai du so du 50.000d de vao quy trinh Admin phan xu.");
        }

        holdUser(buyer, dispute.getOrder().getId(), "Dong bang 50.000d cho quy trinh phan xu don #");
        holdUser(seller, dispute.getOrder().getId(), "Dong bang 50.000d cho quy trinh phan xu don #");

        dispute.setHoldApplied(true);
        dispute.setHoldAmount(DISPUTE_HOLD_FEE);
    }

    private void holdUser(User user, Long orderId, String messagePrefix) {
        user.setBalance(user.getBalance().subtract(DISPUTE_HOLD_FEE));
        user.setHoldBalance(user.getHoldBalance().add(DISPUTE_HOLD_FEE));
        userRepository.save(user);

        createWalletTransaction(
                user,
                DISPUTE_HOLD_FEE.negate(),
                WalletTransaction.TransactionType.DISPUTE_HOLD,
                messagePrefix + orderId,
                orderId
        );
    }

    private void releaseHoldAndSettle(Dispute dispute, boolean buyerWins) {
        if (!dispute.isHoldApplied()) {
            throw new IllegalStateException("Khieu nai nay chua duoc hold de phan xu.");
        }

        User buyer = dispute.getBuyer();
        User seller = dispute.getSeller();
        Long orderId = dispute.getOrder().getId();

        if (buyer.getHoldBalance().compareTo(DISPUTE_HOLD_FEE) < 0 || seller.getHoldBalance().compareTo(DISPUTE_HOLD_FEE) < 0) {
            throw new IllegalStateException("Du lieu hold khong hop le de phan xu.");
        }

        buyer.setHoldBalance(buyer.getHoldBalance().subtract(DISPUTE_HOLD_FEE));
        seller.setHoldBalance(seller.getHoldBalance().subtract(DISPUTE_HOLD_FEE));

        if (buyerWins) {
            buyer.setBalance(buyer.getBalance().add(DISPUTE_HOLD_FEE));

            createWalletTransaction(
                    buyer,
                    DISPUTE_HOLD_FEE,
                    WalletTransaction.TransactionType.DISPUTE_REFUND,
                    "Admin hoan 50.000d phan xu cho Buyer don #" + orderId,
                    orderId
            );
            createWalletTransaction(
                    seller,
                    BigDecimal.ZERO,
                    WalletTransaction.TransactionType.DISPUTE_PENALTY,
                    "Seller thua phan xu, mat 50.000d giu tai he thong cho don #" + orderId,
                    orderId
            );
        } else {
            seller.setBalance(seller.getBalance().add(DISPUTE_HOLD_FEE));

            createWalletTransaction(
                    seller,
                    DISPUTE_HOLD_FEE,
                    WalletTransaction.TransactionType.DISPUTE_REFUND,
                    "Admin hoan 50.000d phan xu cho Seller don #" + orderId,
                    orderId
            );
            createWalletTransaction(
                    buyer,
                    BigDecimal.ZERO,
                    WalletTransaction.TransactionType.DISPUTE_PENALTY,
                    "Buyer thua phan xu, mat 50.000d giu tai he thong cho don #" + orderId,
                    orderId
            );
        }

        userRepository.save(buyer);
        userRepository.save(seller);
    }

    private void appendMessage(Dispute dispute, User sender, String content) {
        DisputeMessage message = new DisputeMessage();
        message.setDispute(dispute);
        message.setSender(sender);
        message.setContent(content);
        messageRepository.save(message);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user."));
    }

    private void ensureAdmin(String username) {
        User user = getUser(username);
        if (user.getRole() != User.Role.ROLE_ADMIN) {
            throw new IllegalStateException("Chi Admin moi duoc thuc hien thao tac nay.");
        }
    }

    private void createWalletTransaction(User user,
                                         BigDecimal amount,
                                         WalletTransaction.TransactionType type,
                                         String description,
                                         Long referenceId) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transactionRepository.save(transaction);
    }
}

