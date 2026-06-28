package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.samoy.chuanbillserver.annotation.Idempotent;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IFileService;
import com.samoy.chuanbillserver.vo.TempFileVO;
import com.samoy.chuanbillserver.vo.UploadTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@Tag(name = "file", description = "文件相关接口")
public class FileController {
    @Resource
    private IFileService fileService;

    @Idempotent
    @PostMapping("/temp/upload")
    @Operation(summary = "上传临时文件", description = "上传临时文件到本地，返回fileId供ocr使用")
    public Result<TempFileVO> uploadTempFile(MultipartFile file) {
        return Result.success(fileService.uploadTempFile(file));
    }

    @GetMapping("/upload-token")
    @Operation(summary = "获取上传凭证", description = "获取上传凭证，用于前端上传文件")
    public Result<UploadTokenVO> getUploadToken(@Parameter(description = "文件名") String fileName) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(fileService.getUploadToken(userId, fileName));
    }
}
