package com.poppy.domain.popupStore.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.popupStore.dto.response.PopupStoreResponseDto;
import com.poppy.domain.popupStore.service.PopupStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/popup-stores")
@RequiredArgsConstructor
public class PopupStoreController {
    private final PopupStoreService popupStoreService;

    // 전체 목록 조회
    @GetMapping
    public RspTemplate<List<PopupStoreResponseDto>> getAllStores() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "팝업스토어 목록 조회 성공",
                popupStoreService.getAllActiveStores()
        );
    }

    // 팝업스토어 상세 조회
    @GetMapping("/{id}")
    public RspTemplate<PopupStoreResponseDto> getStoreDetail(@PathVariable Long id){
        return new RspTemplate<>(
                HttpStatus.OK,
                "팝업스토어 상세 조회 성공",
                popupStoreService.getPopupStore(id)
        );
    }

    // 카테고리별 조회
    @GetMapping("/category/{categoryId}")
    public RspTemplate<List<PopupStoreResponseDto>> getStoresByCategory(
            @PathVariable Long categoryId) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "카테고리별 팝업스토어 조회 성공",
                popupStoreService.getStoresByCategory(categoryId)
        );
    }

    // 위치 기반 검색
    @GetMapping("/location/{location}")
    public RspTemplate<List<PopupStoreResponseDto>> getStoresByLocation(
            @PathVariable String location) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "위치별 팝업스토어 조회 성공",
                popupStoreService.getStoresByLocation(location)
        );
    }

    // 날짜별 검색
    @GetMapping("/date")
    public RspTemplate<List<PopupStoreResponseDto>> getStoresByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "날짜별 팝업스토어 조회 성공",
                popupStoreService.getStoresByDate(startDate, endDate)
        );
    }

    // 이름으로 검색
    @GetMapping("/{name}")
    public RspTemplate<List<PopupStoreResponseDto>> searchStores(
            @PathVariable String name) {
        return new RspTemplate<>(
                HttpStatus.OK,
                "팝업스토어 검색 성공",
                popupStoreService.searchStoresByName(name)
        );
    }

    // 신규 스토어 조회
    @GetMapping("/new")
    public RspTemplate<List<PopupStoreResponseDto>> getNewStores() {
        return new RspTemplate<>(
                HttpStatus.OK,
                "신규 팝업스토어 조회 성공",
                popupStoreService.getNewStores()
        );
    }
}