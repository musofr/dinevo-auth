package it.dinevo.auth.dto;

public record LoginCredentials(String emailOrPhone, String password) {
}
