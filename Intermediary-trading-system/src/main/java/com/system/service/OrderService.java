package com.system.service;

import com.system.model.Order;
import com.system.model.Post;
import com.system.model.User;
import com.system.model.WalletTransaction;
import com.system.repository.OrderRepository;
import com.system.repository.PostRepository;
import com.system.repository.UserRepository;
import com.system.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;


    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.05");

    @Transactional
    public void createOrder(Long postId, String buyerUsername) throws Exception {
        User buyer = userRepository.findByUsername(buyerUsername)
                .orElseThrow(() -> new Exception("Không tìm thấy người mua"));

        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new Exception("Không tìm thấy bài đăng"));

        if (post.getStatus() != Post.PostStatus.ACTIVE) {
            throw new Exception("Bài đăng này không còn khả dụng để mua");
        }

        if (post.getSeller().getId().equals(buyer.getId())) {
            throw new Exception("Bạn không thể mua bài đăng của chính mình");
        }

        BigDecimal postPrice = post.getPrice();
        BigDecimal fee = calculatePlatformFee(postPrice);

        // Nếu buyer chịu phí thì buyer trả giá + 5%
        // Nếu seller chịu phí thì buyer chỉ trả đúng giá bài đăng
        BigDecimal buyerMustPay = post.getFeePayer() == Post.FeePayer.BUYER
                ? postPrice.add(fee)
                : postPrice;

        if (buyer.getBalance().compareTo(buyerMustPay) < 0) {
            throw new Exception("Số dư ví không đủ để thanh toán đơn hàng này");
        }

        // 1. Trừ tiền buyer
        buyer.setBalance(buyer.getBalance().subtract(buyerMustPay));
        userRepository.save(buyer);

        // 2. Tạo order escrow
        Order order = new Order();
        order.setPost(post);
        order.setBuyer(buyer);
        order.setAmount(buyerMustPay);
        order.setStatus(Order.OrderStatus.ESCROWED);
        order.setAutoConfirmAt(LocalDateTime.now().plusDays(3));
        order = orderRepository.save(order);

        // 3. Lock bài đăng lại
        post.setStatus(Post.PostStatus.LOCKED);
        postRepository.save(post);

        // 4. Ghi log ví buyer
        WalletTransaction trans = new WalletTransaction();
        trans.setUser(buyer);
        trans.setAmount(buyerMustPay.negate());
        trans.setType(WalletTransaction.TransactionType.PAY_ORDER);
        trans.setDescription("Thanh toán tạm giữ cho đơn hàng #" + order.getId() + " của bài đăng #" + post.getId());
        trans.setReferenceId(order.getId());
        transactionRepository.save(trans);
    }

    @Transactional
    public void confirmReceived(Long orderId, String buyerUsername) throws Exception {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn hàng"));

        if (!order.getBuyer().getUsername().equals(buyerUsername)) {
            throw new Exception("Bạn không có quyền xác nhận đơn hàng này");
        }

        if (order.getStatus() != Order.OrderStatus.ESCROWED) {
            throw new Exception("Chỉ đơn hàng đang ESCROWED mới được xác nhận");
        }

        completeOrderAndReleaseMoney(order);
    }

    @Transactional
    public void autoConfirmExpiredOrders() {
        List<Order> expiredOrders = orderRepository.findByStatusAndAutoConfirmAtBefore(
                Order.OrderStatus.ESCROWED,
                LocalDateTime.now()
        );

        for (Order order : expiredOrders) {
            Order lockedOrder = orderRepository.findByIdForUpdate(order.getId()).orElse(null);
            if (lockedOrder != null && lockedOrder.getStatus() == Order.OrderStatus.ESCROWED) {
                completeOrderAndReleaseMoney(lockedOrder);
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void scheduledAutoConfirmExpiredOrders() {
        autoConfirmExpiredOrders();
    }

    @Transactional(readOnly = true)
    public List<Order> getBuyerOrders(String username) {
        return orderRepository.findByBuyerUsernameOrderByIdDesc(username);
    }

    @Transactional(readOnly = true)
    public List<Order> getSellerOrders(String username) {
        return orderRepository.findByPostSellerUsernameOrderByIdDesc(username);
    }

    @Transactional(readOnly = true)
    public Order getOrderDetail(Long orderId, String username) throws Exception {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn hàng"));

        boolean isBuyer = order.getBuyer().getUsername().equals(username);
        boolean isSeller = order.getPost().getSeller().getUsername().equals(username);

        if (!isBuyer && !isSeller) {
            throw new Exception("Bạn không có quyền xem đơn hàng này");
        }

        return order;
    }

    private void completeOrderAndReleaseMoney(Order order) {
        Post post = order.getPost();
        User seller = post.getSeller();
        BigDecimal postPrice = post.getPrice();
        BigDecimal fee = calculatePlatformFee(postPrice);

        BigDecimal sellerReceive;

        if (post.getFeePayer() == Post.FeePayer.SELLER) {
            // Buyer trả đúng giá, seller bị trừ 5%
            sellerReceive = postPrice.subtract(fee);
        } else {
            // Buyer trả giá + phí, seller nhận đủ giá
            sellerReceive = postPrice;
        }

        seller.setBalance(seller.getBalance().add(sellerReceive));
        userRepository.save(seller);

        order.setStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(order);

        WalletTransaction sellerRevenue = new WalletTransaction();
        sellerRevenue.setUser(seller);
        sellerRevenue.setAmount(sellerReceive);
        sellerRevenue.setType(WalletTransaction.TransactionType.ORDER_REVENUE);
        sellerRevenue.setDescription("Nhận tiền từ đơn hàng #" + order.getId());
        sellerRevenue.setReferenceId(order.getId());
        transactionRepository.save(sellerRevenue);
    }

    private BigDecimal calculatePlatformFee(BigDecimal price) {
        return price.multiply(PLATFORM_FEE_RATE).setScale(0, RoundingMode.HALF_UP);
    }
}