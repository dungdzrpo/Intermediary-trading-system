package com.system.repository;

import com.system.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndBuyerUsername(Long id, String buyerUsername);
}
