package com.samoy.chuanbillserver.service;

import com.samoy.chuanbillserver.vo.TempFileVO;
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
     * 上传临时文件到 R2
     *
     * @param file 文件
     * @return 文件url
     */
    String uploadFileToR2(MultipartFile file);
}
