package com.example.mentalcompanion.controller;

import com.example.mentalcompanion.common.ApiResponse;
import com.example.mentalcompanion.domain.entity.ChatMessage;
import com.example.mentalcompanion.domain.entity.ChatSession;
import com.example.mentalcompanion.dto.ChatSendRequest;
import com.example.mentalcompanion.dto.ChatSendResponse;
import com.example.mentalcompanion.security.CurrentUserProvider;
import com.example.mentalcompanion.security.JwtUserPrincipal;
import com.example.mentalcompanion.service.ChatWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatWorkflowService chatWorkflowService;
    private final CurrentUserProvider currentUserProvider;

    public ChatController(ChatWorkflowService chatWorkflowService, CurrentUserProvider currentUserProvider) {
        this.chatWorkflowService = chatWorkflowService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/send")
    public ApiResponse<ChatSendResponse> send(@Valid @RequestBody ChatSendRequest request) {
        JwtUserPrincipal user = currentUserProvider.currentUser();
        return ApiResponse.ok(chatWorkflowService.processMessage(user.userId(), request));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> sessions() {
        JwtUserPrincipal user = currentUserProvider.currentUser();
        return ApiResponse.ok(chatWorkflowService.sessions(user.userId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> messages(@PathVariable Long sessionId) {
        JwtUserPrincipal user = currentUserProvider.currentUser();
        return ApiResponse.ok(chatWorkflowService.messages(user.userId(), sessionId));
    }
}

