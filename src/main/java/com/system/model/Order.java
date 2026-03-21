package com.system.model;



import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders") // Trong SQL Server, 'Order' là từ khóa cấm, nên ta phải dùng 'orders'
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với Bài đăng (1 bài đăng có thể có 1 giao dịch thành công)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // Người mua
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    // Tổng tiền người mua đã thanh toán tạm giữ
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Trạng thái của đơn hàng bám sát Flowchart
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.ESCROWED;

    // Thời gian đếm ngược 3 ngày để tự động hoàn thành đơn (nếu người mua quên xác nhận)
    @Column(name = "auto_confirm_at")
    private LocalDateTime autoConfirmAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Enum quản lý các Stage (Đã bỏ qua thỏa thuận ngoài luồng)
    public enum OrderStatus {
        ESCROWED,             // Stage 4: Đã tạm giữ tiền, chờ kiểm tra hàng
        COMPLETED,            // Stage 5: Giao dịch thành công, đã chia tiền
        DISPUTED,             // Stage 6: Người mua khiếu nại, chờ người bán phản hồi
        ADMIN_INTERVENTION,   // Stage 8: Admin can thiệp phân xử
        CANCELLED             // Stage 3: Hủy (Trả lại tiền)
    }
}