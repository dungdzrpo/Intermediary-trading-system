package com.system.repository;

import com.system.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerUsernameOrderByIdDesc(String username);

    List<Order> findByPostSellerUsernameOrderByIdDesc(String username);

    List<Order> findByStatusAndAutoConfirmAtBefore(Order.OrderStatus status, LocalDateTime time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
    @Query("select o from Order o " +
            "join fetch o.post p " +
            "join fetch o.buyer b " +
            "join fetch p.seller " +
            "where o.id = :id")
    Optional<Order> findDetailById(@Param("id") Long id);
    Optional<Order> findByIdAndBuyerUsername(Long id, String buyerUsername);
}