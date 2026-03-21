package com.system.repository;

import com.system.model.Dispute;
import com.system.model.DisputeMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisputeMessageRepository extends JpaRepository<DisputeMessage, Long> {
    List<DisputeMessage> findByDisputeOrderByCreatedAtAsc(Dispute dispute);
}
