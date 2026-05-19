package com.example.mentalcompanion.dto;

import com.example.mentalcompanion.domain.enums.IntentType;
import com.example.mentalcompanion.domain.enums.RiskLevel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatSendResponse {
    private Long sessionId;
    private String reply;
    private IntentType intent;
    private RiskLevel riskLevel;
    private String riskType;
    private boolean ragHit;
    private List<RagReference> references = new ArrayList<>();
    private List<String> actions = new ArrayList<>();
}

