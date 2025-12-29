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

    /**
     * 사용자의 여행 목록 조회 (최신순)
     */
    public List<Travel> getUserTravels(User user) {
        return travelRepository.findByUserOrderByStartDateDesc(user);
    }

    /**
     * 여행 상세 조회
     */
    public Travel getTravelById(Long travelId) {
        return travelRepository.findById(travelId)
            .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다. ID: " + travelId));
    }

    /**
     * 여행 생성 (현재는 SNS에서 직접 생성, 추후 가계부 연동 시 수정 예정)
     */
    @Transactional
    public Travel createTravel(User user, String country, String city,
        LocalDate startDate, LocalDate endDate, String memo) {
        Travel travel = Travel.builder()
            .user(user)
            .country(country)
            .city(city)
            .startDate(startDate)
            .endDate(endDate)
            .memo(memo)
            .build();

        return travelRepository.save(travel);
    }

    /**
     * 여행 수정 (현재는 SNS에서 직접 수정, 추후 가계부 연동 시 수정 예정)
     */
    @Transactional
    public void updateTravel(Long travelId, User currentUser, String country, String city,
        LocalDate startDate, LocalDate endDate, String memo) {
        Travel travel = getTravelById(travelId);

        // 권한 체크
        if (!travel.isOwner(currentUser)) {
            throw new IllegalArgumentException("여행을 수정할 권한이 없습니다.");
        }

        travel.updateInfo(country, city, startDate, endDate, memo);
    }

    /**
     * 여행 삭제
     */
    @Transactional
    public void deleteTravel(Long travelId, User currentUser) {
        Travel travel = getTravelById(travelId);

        // 권한 체크
        if (!travel.isOwner(currentUser)) {
            throw new IllegalArgumentException("여행을 삭제할 권한이 없습니다.");
        }

        travelRepository.delete(travel);
    }

    /**
     * 인기 도시 조회 (통계)
     */
    public List<Object[]> getPopularCities() {
        return travelRepository.findPopularCities();
    }

    /**
     * 사용자의 여행 수 조회
     */
    public long countUserTravels(User user) {
        return travelRepository.countByUser(user);
    }
}









