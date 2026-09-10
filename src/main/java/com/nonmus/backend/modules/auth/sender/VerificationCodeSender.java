package com.nonmus.backend.modules.auth.sender;

import com.nonmus.backend.modules.auth.dto.internal.VerificationCodeSendResult;
import com.nonmus.backend.modules.auth.dto.request.VerificationCodeTarget;

public interface VerificationCodeSender {
    VerificationCodeSendResult send(VerificationCodeTarget target);
}

