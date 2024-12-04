package com.poppy.domain.review.controller;

import com.poppy.common.api.RspTemplate;
import com.poppy.domain.review.entity.ReviewSortType;
import com.poppy.domain.review.dto.request.ReviewReqDto;
import com.poppy.domain.review.dto.response.ReviewLikeRspDto;
import com.poppy.domain.review.dto.response.ReviewRspDto;
import com.poppy.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 등록
    @PostMapping("/{popupStoreId}")
    public RspTemplate<ReviewRspDto> createReview(@PathVariable Long popupStoreId, @Valid @RequestBody ReviewReqDto reviewCreateReqDto){

        ReviewRspDto reviewRspDto = reviewService.createReview(popupStoreId,reviewCreateReqDto);

        return new RspTemplate<>(HttpStatus.CREATED, "리뷰 작성 완료", reviewRspDto);
    }

    // 리뷰 수정
    @PatchMapping("/{reviewId}")
    public RspTemplate<ReviewRspDto> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReqDto reviewUpdateReqDto) {
        ReviewRspDto reviewRspDto = reviewService.updateReview(reviewId, reviewUpdateReqDto);
        return new RspTemplate<>(HttpStatus.OK, "리뷰 수정 완료", reviewRspDto);
    }

    //리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public RspTemplate<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return new RspTemplate<>(HttpStatus.OK, "리뷰 삭제 완료");
    }

    //리뷰의 좋아요 클릭
    @PostMapping("/{reviewId}/like")
    public RspTemplate<ReviewLikeRspDto> toggleLike(@PathVariable Long reviewId) {
        ReviewLikeRspDto response = reviewService.toggleLike(reviewId);
        return new RspTemplate<>(HttpStatus.OK,
                response.isLiked() ? "좋아요 완료" : "좋아요 취소",
                response);
    }

    //최근 등록순, 좋아요 많은순, 별점 높은순, 별점 낮은순
    @GetMapping("/{popupStoreId}/list")
    public RspTemplate<Page<ReviewRspDto>> getReviews(
            @PathVariable Long popupStoreId,
            @RequestParam(defaultValue = "RECENT") ReviewSortType sortType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ReviewRspDto> reviews = reviewService.getReviews(popupStoreId, sortType, pageRequest);

        return new RspTemplate<>(
                HttpStatus.OK,
                "리뷰 조회 완료",
                reviews
        );
    }
}
