package com.example.aispringboot.dto.command;


import lombok.Data;

@Data
public class AiProviderSwitchDTO {
    private String provider;
    private String model;

}

