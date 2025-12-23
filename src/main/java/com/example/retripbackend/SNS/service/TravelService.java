package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.repository.TravelRepository;
import com.example.retripbackend.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelService {

    private final TravelRepository travelRepository;

    // 여행 목록 조회
    public List<Travel> getUserTravels(User user) {
        return travelRepository.findByUserOrderByStartDateDesc(user);
    }

    // 여행 상세 조회
    public Travel getTravelById(Long travelId) {
        return travelRepository.findById(travelId)
            .orElseThrow(() -> new RuntimeException("여행을 찾을 수 없습니다."));
    }

    // 여행 생성
    @Transactional
    public Travel createTravel(User user, String country, String city,
        LocalDate startDate, LocalDate endDate, String memo) {
        Travel travel = Travel.of(user, country, city, startDate, endDate);
        if (memo != null && !memo.isBlank()) {
            travel.update(country, city, startDate, endDate, memo);
        }
        return travelRepository.save(travel);
    }

    // 여행 수정
    @Transactional
    public void updateTravel(Travel travel, String country, String city,
        LocalDate startDate, LocalDate endDate, String memo) {
        travel.update(country, city, startDate, endDate, memo);
    }

    // 여행 삭제
    @Transactional
    public void deleteTravel(Travel travel) {
        travelRepository.delete(travel);
    }

    // 인기 도시 조회
    public List<Object[]> getPopularCities() {
        return travelRepository.findPopularCities();
    }
}









