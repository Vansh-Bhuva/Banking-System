package com.banking.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class BroadcastRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private List<Long> userIds;   // null/empty = send to all
}