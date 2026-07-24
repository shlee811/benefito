package com.lion.benefito.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lion.benefito.domain.chatbot.dto.response.ChatMessageResponse;
import com.lion.benefito.domain.chatbot.dto.response.ChatSessionListResponse;
import com.lion.benefito.domain.chatbot.entity.ChatMessage;
import com.lion.benefito.domain.chatbot.entity.ChatRole;
import com.lion.benefito.domain.chatbot.entity.ChatSession;
import com.lion.benefito.domain.chatbot.repository.ChatMessageRepository;
import com.lion.benefito.domain.chatbot.repository.ChatSessionRepository;
import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 챗봇 대화 기록이 실제 DB(메모리/Redis/프론트 상태가 아니라)에 남는지 검증한다.
 * 일부러 @Transactional을 붙이지 않는다 - 테스트가 끝나도 자동 롤백되지 않고 실제로 커밋되어야만,
 * "애플리케이션을 재시작해도 기록이 유지된다"는 것과 동등한 상황(같은 프로세스가 아니어도 데이터가
 * 살아있음)을 증명할 수 있다. 대신 테스트 종료 시 직접 생성한 데이터만 정리한다.
 */
@SpringBootTest
class ChatbotPersistenceIntegrationTest {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatbotService chatbotService;

    private Long createdUserId;
    private Long createdSessionId;

    @AfterEach
    void cleanUp() {
        if (createdSessionId != null) {
            chatMessageRepository.findByChatSessionIdOrderByCreatedAtAscIdAsc(createdSessionId)
                    .forEach(chatMessageRepository::delete);
            chatSessionRepository.deleteById(createdSessionId);
        }
        if (createdUserId != null) {
            userRepository.deleteById(createdUserId);
        }
    }

    @Test
    void 메시지는_실제_DB에_커밋되고_재조회해도_생성순으로_유지된다() {
        User user = userRepository.save(User.builder()
                .loginId("chatPersistTestUser")
                .password("password")
                .name("영속성테스트")
                .build());
        createdUserId = user.getId();

        ChatSession session = chatSessionRepository.save(ChatSession.builder().user(user).build());
        createdSessionId = session.getId();

        ChatMessage first = chatMessageRepository.save(ChatMessage.builder()
                .chatSession(session).role(ChatRole.USER).message("첫 번째 질문").build());
        ChatMessage second = chatMessageRepository.save(ChatMessage.builder()
                .chatSession(session).role(ChatRole.ASSISTANT).message("첫 번째 답변").build());
        ChatMessage third = chatMessageRepository.save(ChatMessage.builder()
                .chatSession(session).role(ChatRole.USER).message("두 번째 질문").build());

        // 새로운 리포지토리 호출로 "프로세스가 새로 조회하는 상황"을 흉내낸다 - 인메모리 캐시가 아니라
        // 실제 DB 라운드트립으로 값을 가져온다.
        List<ChatMessageResponse> messages = chatbotService.getMessages(user.getId(), session.getId());

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).id()).isEqualTo(first.getId());
        assertThat(messages.get(0).role()).isEqualTo(ChatRole.USER);
        assertThat(messages.get(1).id()).isEqualTo(second.getId());
        assertThat(messages.get(1).role()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(messages.get(2).id()).isEqualTo(third.getId());

        // 실제 테이블에 존재하는지 리포지토리를 통해 다시 한번 직접 확인한다(응답 DTO만이 아니라 원본 엔티티).
        List<ChatMessage> raw = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAscIdAsc(session.getId());
        assertThat(raw).hasSize(3);
    }

    @Test
    void 다른_사용자_소유_세션은_getMessages로_조회할_수_없다() {
        User owner = userRepository.save(User.builder()
                .loginId("chatPersistOwner").password("password").name("소유자").build());
        User other = userRepository.save(User.builder()
                .loginId("chatPersistOther").password("password").name("타인").build());
        createdUserId = owner.getId();

        ChatSession session = chatSessionRepository.save(ChatSession.builder().user(owner).build());
        createdSessionId = session.getId();

        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> chatbotService.getMessages(other.getId(), session.getId()))
                    .isInstanceOf(com.lion.benefito.domain.chatbot.exception.ChatbotException.class);
        } finally {
            userRepository.deleteById(other.getId());
        }
    }

    @Test
    void getSessions은_세션_목록을_실제_DB에서_조회한다() {
        User user = userRepository.save(User.builder()
                .loginId("chatPersistListUser").password("password").name("목록테스트").build());
        createdUserId = user.getId();

        ChatSession session = chatSessionRepository.save(ChatSession.builder().user(user).build());
        createdSessionId = session.getId();
        chatMessageRepository.save(ChatMessage.builder()
                .chatSession(session).role(ChatRole.USER).message("목록 조회용 첫 질문").build());

        List<ChatSessionListResponse> sessions = chatbotService.getSessions(user.getId());

        assertThat(sessions).extracting(ChatSessionListResponse::sessionId).contains(session.getId());
        assertThat(sessions.stream().filter(s -> s.sessionId().equals(session.getId())).findFirst().orElseThrow()
                .title()).isEqualTo("목록 조회용 첫 질문");
    }
}
