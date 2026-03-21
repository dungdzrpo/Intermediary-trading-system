package com.system.model;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Giao dịch này thuộc về User nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Số tiền (Ví dụ: 50000 hoặc -5000)
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // Loại giao dịch
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    // Ghi chú chi tiết (Dùng NVARCHAR cho tiếng Việt trong SQL Server)
    @Column(columnDefinition = "NVARCHAR(255)")
    private String description;

    // Lưu ID của Order hoặc Post liên quan để sau này dễ truy vết chéo
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Các loại biến động số dư theo đúng nghiệp vụ
    public enum TransactionType {
        DEPOSIT,          // Nạp tiền vào ví qua VNPay (+)
        POST_FEE,         // Trừ 5k phí đăng bài (-)
        PAY_ORDER,        // Trừ tiền mua hàng (-)
        ORDER_REVENUE,    // Cộng tiền bán hàng thành công (+)
        REFUND,           // Hoàn tiền khi hủy đơn (+)
        DISPUTE_HOLD,     // Đóng băng 50k phí phân xử (-)
        DISPUTE_REFUND,   // Hoàn lại 50k nếu phân xử thắng (+)
        DISPUTE_PENALTY   // Mất 50k nếu phân xử thua (thực chất đã trừ lúc Hold, lưu log để biết)
    }
}
