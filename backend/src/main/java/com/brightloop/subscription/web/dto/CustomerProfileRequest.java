package com.brightloop.subscription.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Contact name is required")
    private String contactName;

    private String phone;
    private String billingAddress;
    private String taxId;
}
