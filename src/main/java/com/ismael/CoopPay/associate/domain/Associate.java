package com.ismael.CoopPay.associate.domain;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Associate {
    private UUID id;
    private String name;
    private String email;
    private String password;
    private AssociateType type;
}
