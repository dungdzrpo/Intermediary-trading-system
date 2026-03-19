package com.system.service;


import com.system.model.User;
import com.system.model.WalletTransaction;
import com.system.repository.UserRepository;
import com.system.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;


    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public void addMoney(String username, BigDecimal amount, String description) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        // 1. Cộng tiền vào ví
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        // 2. Lưu lại lịch sử giao dịch
        WalletTransaction trans = new WalletTransaction();
        trans.setUser(user);
        trans.setAmount(amount);
        trans.setType(WalletTransaction.TransactionType.DEPOSIT);
        trans.setDescription(description);
        transactionRepository.save(trans);
    }
}
