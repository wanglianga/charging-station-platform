package com.charging.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String role;
        private String name;
    }
}
