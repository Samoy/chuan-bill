package com.samoy.chuanbillserver.controller;

import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IFileService;
import com.samoy.chuanbillserver.vo.TempFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/temp/upload")
    @Operation(summary = "上传临时文件", description = "上传临时文件到本地，返回fileId供ocr使用")
    public Result<TempFileVO> uploadTempFile(MultipartFile file) {
        return Result.success(fileService.uploadTempFile(file));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传文件到R2", description = "上传文件到Cloudflare R2，返回文件URL")
    public Result<String> uploadTempFileToR2(HttpServletRequest request, MultipartFile file) {
        String url = fileService.uploadFileToR2(file);
        return Result.success(url);
    }
}
