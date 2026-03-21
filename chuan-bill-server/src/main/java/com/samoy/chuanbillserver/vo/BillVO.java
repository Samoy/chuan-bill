package com.samoy.chuanbillserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BillVO {
    private String id;
    private String name;
    private String categoryId;
    private String categoryName;
    private String paymentMethodId;
    private String paymentMethodName;
    private String type;
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime time;

    private String remark;
    private String source;
    private String familyId;
}
