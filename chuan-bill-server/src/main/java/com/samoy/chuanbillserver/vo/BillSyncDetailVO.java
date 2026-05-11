package com.samoy.chuanbillserver.vo;

import lombok.Data;

/**
 * 单条账单同步详情
 */
@Data
public class BillSyncDetailVO {
    /** 批次中的索引（0-based） */
    private int index;
    /** 同步状态：SUCCESS / FAILED */
    private String status;
    /** 成功时返回服务器生成的 ID */
    private Long billId;
    /** 失败时的错误原因 */
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
