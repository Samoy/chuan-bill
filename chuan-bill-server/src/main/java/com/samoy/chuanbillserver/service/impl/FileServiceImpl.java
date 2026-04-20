package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IFileService;
import com.samoy.chuanbillserver.vo.TempFileVO;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
public class FileServiceImpl implements IFileService {

    @Resource
    private S3Client s3Client;

    @Value("${r2.bucket-name}")
    private String bucketName;

    @Value("${r2.public-url}")
    private String publicUrl;

    @Override
    public TempFileVO uploadTempFile(MultipartFile file) {
        // 1.判断是否是图片格式
        String contentType = file.getContentType();
        if (!CharSequenceUtil.startWith(contentType, "image")) {
            throw new BusinessException(ResultEnum.FILE_UPLOAD_FAILED, "只能上传图片文件");
        }
        try {
            // 2. 储存临时文件
            String fileId = IdUtil.objectId();
            Path tempPath = Paths.get(SystemConstants.TEMP_FILE_UPLOAD_DIR, fileId);
            PathUtil.copyFile(file.getInputStream(), tempPath);
            TempFileVO tempFileVO = new TempFileVO();
            tempFileVO.setFileId(fileId);
            tempFileVO.setFileSize(file.getSize());
            tempFileVO.setFileExt(FileUtil.extName(file.getOriginalFilename()));
            return tempFileVO;
        } catch (IOException e) {
            throw new BusinessException(ResultEnum.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String uploadFileToR2(MultipartFile file) {
        String fileName = String.format("%s.%s", IdUtil.objectId(), FileUtil.extName(file.getOriginalFilename()));
        String fileKey = "upload/" + fileName;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return publicUrl + "/" + fileKey;
        } catch (S3Exception | IOException e) {
            log.error(e.getMessage(), e);
            throw new BusinessException(ResultEnum.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }
}
