package com.emailai.responder.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class EmailResponse {
    private String emailId;
    private String responseText;
    private boolean sent;
    private String errorMessage;
}