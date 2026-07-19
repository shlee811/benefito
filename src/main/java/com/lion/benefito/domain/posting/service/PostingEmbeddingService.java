package com.lion.benefito.domain.posting.service;

import com.lion.benefito.domain.posting.entity.Posting;
import com.lion.benefito.domain.posting.repository.PostingRepository;
import com.lion.benefito.infrastructure.ai.embedding.EmbeddingClient;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostingEmbeddingService {

    private final PostingRepository postingRepository;
    private final EmbeddingClient embeddingClient;

    @Transactional
    public void generateMissingEmbeddings() {
        List<Posting> targets = postingRepository.findByEmbeddingIsNull();
        log.info("임베딩이 없는 공고 {}건을 처리합니다.", targets.size());

        for (Posting posting : targets) {
            try {
                refreshEmbedding(posting);
            } catch (Exception e) {
                log.warn("공고 ID {} 임베딩 생성 실패: {}", posting.getId(), e.getMessage());
            }
        }
    }

    // 공고 제목/내용이 변경될 때(향후 수정 API)도 이 메서드를 그대로 재사용해 임베딩을 갱신하면 된다.
    @Transactional
    public void refreshEmbedding(Posting posting) {
        String embeddingText = buildEmbeddingText(posting);
        List<Float> vector = embeddingClient.embed(embeddingText);
        postingRepository.updateEmbedding(posting.getId(), toVectorLiteral(vector));
    }

    private String buildEmbeddingText(Posting posting) {
        String content = posting.getContent() != null ? posting.getContent() : "";
        return posting.getTitle() + "\n" + content;
    }

    private String toVectorLiteral(List<Float> vector) {
        return vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
