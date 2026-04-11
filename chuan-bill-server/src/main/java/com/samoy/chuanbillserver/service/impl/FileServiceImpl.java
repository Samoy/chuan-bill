package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.samoy.chuanbillserver.constant.SystemConstants;
import com.samoy.chuanbillserver.expection.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IFileService;
import com.samoy.chuanbillserver.vo.TempFileVO;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements IFileService {

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
}
