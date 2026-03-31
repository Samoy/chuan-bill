package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "临时文件信息")
public class TempFileVO {
    @Schema(description = "临时文件ID", example = "xxxx.png")
    private String fileId;

    @Schema(description = "临时文件大小", example = "1024")
    private long fileSize;
}
