package com.samoy.chuanbillserver.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 单条账单同步详情 */
@Data
@Schema(description = "单条账单同步详情")
public class BillSyncDetailVO {
    @Schema(description = "批次中的索引（0-based）", example = "0")
    private int index;

    @Schema(description = "同步状态：SUCCESS / FAILED", example = "SUCCESS")
    private String status;

    @Schema(description = "成功时返回服务器生成的 ID", example = "123456")
    private Long billId;

    @Schema(description = "失败时的错误原因", example = "分类不存在")
    private String reason;

    public static BillSyncDetailVO success(int index, Long billId) {
        BillSyncDetailVO detail = new BillSyncDetailVO();
        detail.setIndex(index);
        detail.setStatus("SUCCESS");
        detail.setBillId(billId);
        return detail;
    }

    public static BillSyncDetailVO failed(int index, String reason) {
        BillSyncDetailVO detail = new BillSyncDetailVO();
        detail.setIndex(index);
        detail.setStatus("FAILED");
        detail.setReason(reason);
        return detail;
    }
}
