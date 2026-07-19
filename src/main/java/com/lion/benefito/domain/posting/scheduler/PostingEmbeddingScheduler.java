package com.lion.benefito.domain.posting.scheduler;

import com.lion.benefito.domain.posting.service.PostingEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PostingEmbeddingScheduler {

    private final PostingEmbeddingService postingEmbeddingService;

    // 기동 1분 후 최초 실행, 이후 10분 주기로 embedding이 없는 공고를 처리한다.
    // initialDelay를 지정하지 않으면(또는 0으로 두면) Spring이 앱 기동 즉시 첫 실행을 트리거해
    // @SpringBootTest(contextLoads 등) 실행 중에도 발동, 테스트할 때마다 실제 OpenAI API를 호출하고
    // DB를 변경해버리는 문제가 있어(실제로 재현됨) 최소한의 지연을 둔다.
    @Scheduled(initialDelay = 60 * 1000, fixedDelay = 10 * 60 * 1000)
    public void generateMissingEmbeddings() {
        log.info("공고 embedding 생성 스케줄러 실행");
        postingEmbeddingService.generateMissingEmbeddings();
    }
}
