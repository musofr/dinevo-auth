package it.dinevo.auth.dto;

public record MerchantSignupCredentials(
    String email, 
    String phoneNumber, 
    String password, 
    String displayName,
    String establishmentCode
) {}
