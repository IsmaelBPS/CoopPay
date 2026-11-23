package com.ismael.CoopPay.account.api;

import jakarta.validation.constraints.NotBlank;

public record balanceRequest(
    @NotBlank String document
    

) {
    
}
