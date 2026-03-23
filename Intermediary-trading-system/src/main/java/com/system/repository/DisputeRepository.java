package com.system.repository;

import com.system.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    boolean existsByOrderId(Long orderId);

    List<Dispute> findByBuyerUsernameOrSellerUsernameOrderByCreatedAtDesc(String buyerUsername, String sellerUsername);

    List<Dispute> findAllByOrderByCreatedAtDesc();

    List<Dispute> findByStatusOrderByCreatedAtAsc(Dispute.DisputeStatus status);
}
