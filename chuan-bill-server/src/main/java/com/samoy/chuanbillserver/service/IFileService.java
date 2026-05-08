package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.vo.TempFileVO;
import com.samoy.chuanbillserver.vo.UploadTokenVO;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {

    /**
     * 上传临时文件
     *
     * @param file 文件
     * @return 临时文件信息
     */
    TempFileVO uploadTempFile(MultipartFile file);

    /**
     * 获取上传凭证
     *
     * @param userId   用户ID
     * @param fileName 文件名
     * @return 上传凭证
     */
    UploadTokenVO getUploadToken(String userId, String fileName);
}
