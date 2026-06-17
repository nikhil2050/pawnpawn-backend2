package com.pnpn.shared.dto;

import com.pnpn.shared.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String name;
    private String bio;
    private String profilePicUrl;
    private VerificationStatus verificationStatus;
}
