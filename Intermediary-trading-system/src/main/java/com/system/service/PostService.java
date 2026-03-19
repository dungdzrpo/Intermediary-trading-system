package com.system.service;


import com.system.dto.PostCreateDTO;
import com.system.model.Post;
import com.system.model.User;
import com.system.model.WalletTransaction;
import com.system.repository.PostRepository;
import com.system.repository.UserRepository;
import com.system.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PostService {


    private final PostRepository postRepository;


    private final UserRepository userRepository;


    private final WalletTransactionRepository transactionRepository;

    // Phí đăng bài cố định là 5.000 VNĐ
    private static final BigDecimal POST_FEE = new BigDecimal("5000");

    // @Transactional đảm bảo nếu lỗi giữa chừng, tiền sẽ không bị trừ mất
    @Transactional
    public void createPost(PostCreateDTO dto, String username) throws Exception {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Không tìm thấy người dùng"));

        // 1. Kiểm tra số dư ví (Balance)
        if (seller.getBalance().compareTo(POST_FEE) < 0) {
            throw new Exception("Số dư ví không đủ 5.000đ để đăng bài. Vui lòng nạp thêm tiền!");
        }

        // 2. Trừ 5.000đ trong ví
        seller.setBalance(seller.getBalance().subtract(POST_FEE));
        userRepository.save(seller);

        // 3. Tạo bài viết mới
        Post post = new Post();
        post.setSeller(seller);
        post.setTitle(dto.getTitle());
        post.setDescription(dto.getDescription());
        post.setHiddenInfo(dto.getHiddenInfo());
        post.setPrice(dto.getPrice());
        post.setFeePayer(dto.getFeePayer());
        post.setStatus(Post.PostStatus.ACTIVE); // Đủ tiền trừ nên bài viết Active luôn

        post = postRepository.save(post); // Lưu để lấy ID bài viết sinh ra

        // 4. Ghi lại lịch sử biến động số dư (Sao kê)
        WalletTransaction trans = new WalletTransaction();
        trans.setUser(seller);
        trans.setAmount(POST_FEE.negate()); // Số âm (-5000)
        trans.setType(WalletTransaction.TransactionType.POST_FEE);
        trans.setDescription("Trừ phí đăng bài viết #" + post.getId());
        trans.setReferenceId(post.getId());

        transactionRepository.save(trans);
    }
}