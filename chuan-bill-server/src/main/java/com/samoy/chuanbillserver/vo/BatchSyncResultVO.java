package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

/** 批量同步结果 */
@Data
@Schema(description = "批量同步结果")
public class BatchSyncResultVO {
    @Schema(description = "总数", example = "5")
    private int total;

    @Schema(description = "成功数", example = "3")
    private int successCount;

    @Schema(description = "失败数", example = "2")
    private int failedCount;

    @Schema(description = "状态：ALL_SUCCESS / PARTIAL_SUCCESS / ALL_FAILED", example = "PARTIAL_SUCCESS")
    private String status;

    @Schema(description = "每条账单的同步详情")
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
