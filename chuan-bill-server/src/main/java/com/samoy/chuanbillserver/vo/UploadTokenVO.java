package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "上传凭证")
public class UploadTokenVO {
    private String token;
    private String key;
    private String cdnUrl;
    private String uploadUrl;
    private long expireSeconds;
}
