package com.samoy.chuanbillserver.vo;

import java.util.List;
import lombok.Data;

/**
 * 批量同步结果
 */
@Data
public class BatchSyncResultVO {
    /** 总数 */
    private int total;
    /** 成功数 */
    private int successCount;
    /** 失败数 */
    private int failedCount;
    /** 状态：ALL_SUCCESS / PARTIAL_SUCCESS / ALL_FAILED */
    private String status;
    /** 每条账单的同步详情 */
    private List<BillSyncDetailVO> details;

    public static BatchSyncResultVO of(List<BillSyncDetailVO> details, int total) {
        BatchSyncResultVO result = new BatchSyncResultVO();
        result.setTotal(total);
        long successCount =
                details.stream().filter(d -> "SUCCESS".equals(d.getStatus())).count();
        result.setSuccessCount((int) successCount);
        result.setFailedCount(total - (int) successCount);
        if (successCount == total) {
            result.setStatus("ALL_SUCCESS");
        } else if (successCount == 0) {
            result.setStatus("ALL_FAILED");
        } else {
            result.setStatus("PARTIAL_SUCCESS");
        }
        result.setDetails(details);
        return result;
    }
}
