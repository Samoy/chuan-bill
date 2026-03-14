package com.samoy.chuanbillserver.vo;

import lombok.Data;

@Data
public class TokenVO {
    private String token;
    private Long expireTime;
    private String userId;
    private String nickname;
}
