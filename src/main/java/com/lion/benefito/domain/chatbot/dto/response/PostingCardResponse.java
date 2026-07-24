package com.lion.benefito.domain.chatbot.dto.response;

import com.lion.benefito.domain.posting.dto.PostingSimilarityResponse;
import com.lion.benefito.domain.posting.entity.Posting;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "챗봇 답변에 딸린 추천 공고 카드")
public record PostingCardResponse(
        @Schema(description = "공고 ID", example = "1") Long postingId,
        @Schema(description = "제목") String title,
        @Schema(description = "기관/회사명") String organization,
        @Schema(description = "카테고리 코드", example = "CONTEST") String categoryCode,
        @Schema(description = "지역 코드", example = "전국") String region,
        @Schema(description = "마감일") LocalDate deadline,
        @Schema(description = "원문 URL") String link
) {

    // sendMessage()가 프롬프트 context에 사용한 것과 동일한 RAG 검색 결과 객체에서 그대로 변환한다.
    // 별도 조회 없이 이 값을 그대로 응답에 실어야 답변 텍스트와 카드 목록이 항상 일치한다.
    public static PostingCardResponse from(PostingSimilarityResponse similarity) {
        return new PostingCardResponse(
                similarity.postingId(),
                similarity.title(),
                similarity.organization(),
                similarity.category(),
                similarity.region(),
                similarity.deadline(),
                similarity.sourceUrl()
        );
    }

    // 히스토리 조회(getMessages) 시 그 시점에 실제로 저장된 ChatMessagePosting을 통해 복원한다.
    public static PostingCardResponse from(Posting posting) {
        return new PostingCardResponse(
                posting.getId(),
                posting.getTitle(),
                posting.getCompany(),
                posting.getCategoryCode() != null ? posting.getCategoryCode().getCode() : null,
                posting.getRegionCode() != null ? posting.getRegionCode().getCode() : null,
                posting.getEndDate(),
                posting.getHomepageUrl()
        );
    }
}
