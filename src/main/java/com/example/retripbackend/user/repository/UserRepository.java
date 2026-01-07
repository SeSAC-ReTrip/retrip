package com.example.retripbackend.user.repository;

import com.example.retripbackend.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 사용자 조회
    Optional<User> findByEmail(String email);

    // 이름으로 사용자 조회
    Optional<User> findByName(String name);

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 이름 중복 체크 (선택사항 - 필요시 사용)
    boolean existsByName(String name);
}