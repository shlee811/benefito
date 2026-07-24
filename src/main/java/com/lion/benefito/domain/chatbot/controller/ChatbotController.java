package com.lion.benefito.domain.chatbot.controller;

import com.lion.benefito.domain.chatbot.dto.request.ChatMessageRequest;
import com.lion.benefito.domain.chatbot.dto.response.ChatMessageResponse;
import com.lion.benefito.domain.chatbot.dto.response.ChatSessionCreateResponse;
import com.lion.benefito.domain.chatbot.dto.response.ChatSessionListResponse;
import com.lion.benefito.domain.chatbot.service.ChatbotService;
import com.lion.benefito.global.common.CustomResponse;
import com.lion.benefito.global.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chatbot", description = "AI 챗봇 대화 API")
@RestController
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "대화 세션 생성", description = "로그인 사용자 기준으로 새 대화 세션을 생성한다.")
    @PostMapping("/chatbot/sessions")
    public ResponseEntity<CustomResponse<ChatSessionCreateResponse>> createSession() {
        Long userId = currentUserProvider.getCurrentUserId();
        ChatSessionCreateResponse response = chatbotService.createSession(userId);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    @Operation(summary = "메시지 전송", description = "세션에 사용자 메시지를 저장하고 AI 응답을 생성하여 반환한다.")
    @PostMapping("/chatbot/sessions/{sessionId}/messages")
    public ResponseEntity<CustomResponse<ChatMessageResponse>> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody ChatMessageRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        ChatMessageResponse response = chatbotService.sendMessage(userId, sessionId, request);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    @Operation(summary = "메시지 목록 조회", description = "세션의 메시지를 생성 순서대로 조회한다.")
    @GetMapping("/chatbot/sessions/{sessionId}/messages")
    public ResponseEntity<CustomResponse<List<ChatMessageResponse>>> getMessages(@PathVariable Long sessionId) {
        Long userId = currentUserProvider.getCurrentUserId();
        List<ChatMessageResponse> response = chatbotService.getMessages(userId, sessionId);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    @Operation(summary = "대화 세션 목록 조회", description = "로그인 사용자의 기존 대화 세션을 최근 대화 순으로 조회한다.")
    @GetMapping("/chatbot/sessions")
    public ResponseEntity<CustomResponse<List<ChatSessionListResponse>>> getSessions() {
        Long userId = currentUserProvider.getCurrentUserId();
        List<ChatSessionListResponse> response = chatbotService.getSessions(userId);
        return ResponseEntity.ok(CustomResponse.onSuccess(response));
    }

    @Operation(summary = "메시지 삭제", description = "세션 안의 특정 메시지 1건을 삭제한다.")
    @DeleteMapping("/chatbot/sessions/{sessionId}/messages/{messageId}")
    public ResponseEntity<CustomResponse<Void>> deleteMessage(
            @PathVariable Long sessionId, @PathVariable Long messageId) {
        Long userId = currentUserProvider.getCurrentUserId();
        chatbotService.deleteMessage(userId, sessionId, messageId);
        return ResponseEntity.ok(CustomResponse.onSuccess(null));
    }

    @Operation(summary = "대화 세션 삭제", description = "세션(대화방)을 메시지창에 들어가지 않고 세션 목록에서 통째로 삭제한다.")
    @DeleteMapping("/chatbot/sessions/{sessionId}")
    public ResponseEntity<CustomResponse<Void>> deleteSession(@PathVariable Long sessionId) {
        Long userId = currentUserProvider.getCurrentUserId();
        chatbotService.deleteSession(userId, sessionId);
        return ResponseEntity.ok(CustomResponse.onSuccess(null));
    }
}
