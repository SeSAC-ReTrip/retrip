package com.example.retripbackend.SNS.repository;

import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelRepository extends JpaRepository<Travel, Long> {

    // 특정 사용자의 여행 목록 (최신순)
    List<Travel> findByUserOrderByStartDateDesc(User user);

    // 여행 수 조회
    long countByUser(User user);

}









