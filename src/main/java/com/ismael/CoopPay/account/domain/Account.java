package com.ismael.CoopPay.account.domain;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor 
public class Account {
    private UUID id;
    private UUID userId;
    private BigDecimal balance;
}
