package com.samoy.chuanbillserver.controller;

import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IFileService;
import com.samoy.chuanbillserver.vo.TempFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
    @Operation(summary = "上传临时文件", description = "上传临时文件，返回fileId供ocr使用")
    public Result<TempFileVO> uploadTempFile(MultipartFile file) {
        return Result.success(fileService.uploadTempFile(file));
    }
}
