package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddPaymentMethodDTO {

    @NotBlank(message = "支付方式名称不能为空") private String name;

    @NotBlank(message = "支付方式图标不能为空") private String icon;
}
