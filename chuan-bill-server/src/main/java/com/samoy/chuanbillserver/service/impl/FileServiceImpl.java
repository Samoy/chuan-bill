package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IFileService;
import com.samoy.chuanbillserver.vo.TempFileVO;
import com.samoy.chuanbillserver.vo.UploadTokenVO;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileServiceImpl implements IFileService {

    @Value("${qiniu.bucket}")
    private String bucketName;

    @Value("${qiniu.access_key}")
    private String accessKey;

    @Value("${qiniu.secret_key}")
    private String secretKey;

    @Value("${qiniu.cdn-domain}")
    private String cdnUrl;

    @Value("${qiniu.endpoint}")
    private String endpoint;

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
    public UploadTokenVO getUploadToken(String userId, String fileName) {
        // 1. 动态生成唯一文件路径： /upload/{userId}/{random}.{ext}
        String extName = FileUtil.extName(fileName);
        String saveKey = String.format(
                "upload/%s/%s.%s", userId, IdUtil.fastSimpleUUID(), StrUtil.isEmpty(extName) ? "png" : extName);

        // 2. 构建安全上传策略
        StringMap putPolicy = new StringMap()
                .put("scope", bucketName + ":" + saveKey)
                .put("fsizeLimit", 1024 * 1024 * 10)
                .put("insertOnly", 1)
                .put("mimeLimit", "image/*");

        // 3. 构建上传token
        Auth auth = Auth.create(accessKey, secretKey);
        String token = auth.uploadToken(bucketName, saveKey, SystemConstants.QINIU_UPLOAD_TOKEN_EXPIRE_TIME, putPolicy);
        return UploadTokenVO.builder()
                .token(token)
                .key(saveKey)
                .cdnUrl(cdnUrl + "/" + saveKey)
                .uploadUrl(endpoint)
                .expireSeconds(SystemConstants.QINIU_UPLOAD_TOKEN_EXPIRE_TIME)
                .build();
    }
}
