package com.poppy.domain.popupStore.service;

import com.poppy.common.exception.BusinessException;
import com.poppy.common.exception.ErrorCode;
import com.poppy.domain.popupStore.dto.request.PopupStoreSearchReqDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreCalenderRspDto;
import com.poppy.domain.popupStore.dto.response.PopupStoreRspDto;
import com.poppy.domain.popupStore.entity.PopupStore;
import com.poppy.domain.popupStore.entity.PopupStoreView;
import com.poppy.domain.popupStore.repository.PopupStoreRepository;
import com.poppy.domain.popupStore.dto.response.ReservationAvailableSlotRspDto;
import com.poppy.domain.popupStore.repository.PopupStoreViewRepository;
import com.poppy.domain.reservation.entity.PopupStoreStatus;
import com.poppy.domain.reservation.entity.ReservationAvailableSlot;
import com.poppy.domain.reservation.repository.ReservationAvailableSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopupStoreService {
    private final PopupStoreRepository popupStoreRepository;
    private final ReservationAvailableSlotRepository reservationAvailableSlotRepository;
    private final PopupStoreViewRepository popupStoreViewRepository;

    // 전체 목록 조회
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> getAllActiveStores() {
        List<PopupStore> stores = popupStoreRepository.findAllActive();
        return stores.stream()
                .map(PopupStoreRspDto::from)
                .collect(Collectors.toList());
    }

    // 팝업 스토어 상세 조회
    @Transactional
    public PopupStoreRspDto getPopupStore(Long id) {
        PopupStore popupStore = popupStoreRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 조회 기록 추가 (PopupStoreView에 추가)
        PopupStoreView view = PopupStoreView.builder()
                .popupStore(popupStore)
                .viewedAt(LocalDateTime.now())
                .build();
        popupStoreViewRepository.save(view);

        return PopupStoreRspDto.from(popupStore);
    }

    // 팝업 스토어 조회 필터링
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> searchFiltering(PopupStoreSearchReqDto popupStoreSearchReqDto) {
        List<PopupStore> stores = popupStoreRepository.findBySearchCondition(popupStoreSearchReqDto);
        return stores.stream()
                .map(PopupStoreRspDto::from)
                .collect(Collectors.toList());
    }

    // 이름으로 검색
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> searchStoresByName(String name) {
        List<PopupStore> stores = popupStoreRepository.findByKeyword(name);
        if(stores.isEmpty()) throw new BusinessException(ErrorCode.STORE_NOT_FOUND);

        return stores.stream()
                .map(PopupStoreRspDto::from)
                .collect(Collectors.toList());
    }

    // 신규 스토어 조회 (일주일 이내)
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> getNewStores() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<PopupStore> stores = popupStoreRepository.findNewStores(weekAgo);
        if (stores.isEmpty()) {
            throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
        }
        return stores.stream()
                .map(PopupStoreRspDto::from)
                .collect(Collectors.toList());
    }

    // 오픈 예정 팝업 스토어
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> getAllFuturePopupStores() {
        LocalDate today = LocalDate.now();

        List<PopupStore> stores = popupStoreRepository.findAllFuturePopupStores(today);

        return stores.stream()
                .map(PopupStoreRspDto::from)
                .collect(Collectors.toList());
    }

    // 3시간 내 인기 팝업 스토어 조회
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> getPopularPopupStores() {

        LocalDateTime startTime = LocalDateTime.now().minusHours(3);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Object[]> results = popupStoreViewRepository.findPopularPopupStores(startTime, pageable);

        List<Long> popupStoreIds = results.getContent().stream()
                .map(result -> (Long) result[0])
                .collect(Collectors.toList());

        List<PopupStore> stores = popupStoreRepository.findAllById(popupStoreIds);

        return stores.stream()
                .map(PopupStoreRspDto::from)
                .collect(Collectors.toList());
    }

    // 지금 주목해야 할 (카테고리) 팝업
    @Transactional(readOnly = true)
    public List<PopupStoreRspDto> getPopularPopupStoresByCategory(Long categoryId) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(3);
        Pageable pageable = PageRequest.of(0, 10); // 상위 10개 조회

        Page<Object[]> results = popupStoreViewRepository.findPopularPopupStoresByCategory(
                categoryId,
                startTime,
                pageable
        );

        if (results.isEmpty()) throw new BusinessException(ErrorCode.STORE_NOT_FOUND);

        List<Long> popupStoreIds = results.getContent().stream()
                .map(result -> (Long) result[0])
                .collect(Collectors.toList());

        List<PopupStore> stores = popupStoreRepository.findAllById(popupStoreIds);

        return stores.stream()
                .map(PopupStoreRspDto::from)
                .collect(Collectors.toList());
    }

    // 팝업 스토어 캘린더 반환
    @Transactional(readOnly = true)
    public PopupStoreCalenderRspDto getCalender(Long id) {
        PopupStore popupStore = popupStoreRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 오늘 이전 날짜는 예약 가능일 설정 X
        LocalDate now = LocalDate.now();
        LocalDate startDate = popupStore.getStartDate().isBefore(now) ? now : popupStore.getStartDate();

        // 날짜별 상태 조회
        Map<LocalDate, List<ReservationAvailableSlot>> slotsByDate =
                reservationAvailableSlotRepository.findByPopupStoreIdAndDateBetween(id, startDate, popupStore.getEndDate())
                        .stream()
                        .collect(Collectors.groupingBy(ReservationAvailableSlot::getDate));

        Map<LocalDate, PopupStoreStatus> popupStoreStatuses = new HashMap<>();
        LocalDate current = startDate;

        // 예약 가능한 날짜 조회
        while (!current.isAfter(popupStore.getEndDate())) {
            // 오늘 이전의 날짜는 PAST로 갱신
            if(current.isBefore(now)) popupStoreStatuses.put(current, PopupStoreStatus.PAST);
            else {
                List<ReservationAvailableSlot> daySlots = slotsByDate.get(current);
                if (daySlots == null || daySlots.isEmpty())
                    popupStoreStatuses.put(current, PopupStoreStatus.AVAILABLE);
                else {
                    // 해당 날짜의 모든 슬롯 중 하나라도 HOLIDAY면 HOLIDAY
                    if (daySlots.stream().anyMatch(slot -> slot.getStatus() == PopupStoreStatus.HOLIDAY)) {
                        popupStoreStatuses.put(current, PopupStoreStatus.HOLIDAY);
                    }
                    // 모든 슬롯이 예약 마감이면 FULL
                    else if (daySlots.stream().noneMatch(ReservationAvailableSlot::isAvailable)) {
                        popupStoreStatuses.put(current, PopupStoreStatus.FULL);
                    }
                    // 그 외의 경우는 예약 가능
                    else {
                        popupStoreStatuses.put(current, PopupStoreStatus.AVAILABLE);
                    }
                }
            }
            current = current.plusDays(1);
        }

        return PopupStoreCalenderRspDto.builder()
                .id(popupStore.getId())
                .name(popupStore.getName())
                .startDate(startDate)
                .endDate(popupStore.getEndDate())
                .statuses(popupStoreStatuses)
                .build();
    }

    // 팝업 스토어 생성 시 DB에 슬롯 초기화
    @Transactional
    public void initializeSlots(Long storeId) {
        // 팝업 스토어 조회
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 휴무일 목록 조회
        Set<LocalDate> holidays = reservationAvailableSlotRepository
                .findByPopupStoreIdAndStatus(storeId, PopupStoreStatus.HOLIDAY)
                .stream()
                .map(ReservationAvailableSlot::getDate)
                .collect(Collectors.toSet());

        // 전체 슬롯 수
        int totalAvailableSlot = popupStore.getAvailableSlot();

        // 운영 기간 설정
        List<LocalDateTime> slotDateTimeList = new ArrayList<>();
        LocalDate currentDate = popupStore.getStartDate();
        LocalDate endDate = popupStore.getEndDate();

        // 모든 슬롯 시간을 리스트에 추가 (휴무일 제외)
        while (!currentDate.isAfter(endDate)) {
            // 휴무일이 아닌 경우에만 슬롯 추가
            if (!holidays.contains(currentDate)) {
                LocalTime currentTime = popupStore.getOpeningTime();
                LocalTime closingTime = popupStore.getClosingTime();

                while (currentTime.isBefore(closingTime)) {
                    slotDateTimeList.add(LocalDateTime.of(currentDate, currentTime));
                    currentTime = currentTime.plusHours(1); // 1시간 단위
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        // 운영 가능한 슬롯이 없는 경우
        if (slotDateTimeList.isEmpty()) return;

        // 슬롯 리스트를 가까운 날짜, 시간 순으로 정렬
        slotDateTimeList.sort(Comparator.naturalOrder());

        // 휴무일을 제외한 실제 운영 슬롯 수로 계산
        int totalSlots = slotDateTimeList.size();   // 휴무일을 제외한 한 팝업 스토어의 총 슬롯 수
        int slotCapacity = totalAvailableSlot / totalSlots;     // 슬롯당 인원
        int remainingSlots = totalAvailableSlot % totalSlots;   // 나머지 (배분할 잉여 인원)

        // 정렬된 슬롯 리스트 DB에 저장
        LocalDateTime now = LocalDateTime.now();
        List<ReservationAvailableSlot> slotsToSave = new ArrayList<>();

        for (LocalDateTime slotDateTime: slotDateTimeList) {
            // 이미 지난 시간은 저장하지 않음
            if (slotDateTime.isBefore(now)) continue;

            // 각 슬롯에 기본 인원 할당
            int slotCount = slotCapacity;

            // 잉여 인원이 남아 있으면 한 슬롯에 추가
            if (remainingSlots > 0) {
                slotCount += 1;
                remainingSlots -= 1;
            }

            // 슬롯 객체 생성
            ReservationAvailableSlot slot = ReservationAvailableSlot.builder()
                    .popupStore(popupStore)
                    .date(slotDateTime.toLocalDate())
                    .time(slotDateTime.toLocalTime())
                    .availableSlot(slotCount)
                    .totalSlot(slotCount)
                    .status(PopupStoreStatus.AVAILABLE)
                    .build();

            slotsToSave.add(slot);
        }

        // 한 번에 저장
        if(!slotsToSave.isEmpty()) reservationAvailableSlotRepository.saveAll(slotsToSave);
    }

    // 특정 날짜의 예약 가능 시간대 조회
    @Transactional(readOnly = true)
    public List<ReservationAvailableSlotRspDto> getAvailableSlots(Long storeId, LocalDate date) {
        // 팝업 스토어 조회
        PopupStore popupStore = popupStoreRepository.findById(storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // 특정 날짜의 예약 가능한 슬롯 조회
        List<ReservationAvailableSlot> slots = reservationAvailableSlotRepository.findByPopupStoreAndDate(popupStore, date);

        if(slots.isEmpty()) throw new BusinessException(ErrorCode.SLOT_NOT_FOUND); // 슬롯이 없는 경우

        // 슬롯 정보 DTO로 변환
        return slots.stream()
                .filter(slot -> {
                    // 현재 시간 이후 슬롯만 반환
                    LocalDateTime slotDateTime = LocalDateTime.of(slot.getDate(), slot.getTime());
                    return !slotDateTime.isBefore(LocalDateTime.now());
                })
                .map(slot -> ReservationAvailableSlotRspDto.builder()
                        .time(slot.getTime().toString())
                        .availableSlot(slot.getAvailableSlot())
                        .isAvailable(slot.isAvailable())
                        .build()
                )
                .collect(Collectors.toList());
    }
}