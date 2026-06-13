package com.samoy.chuanbillserver.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SortDTO {

    @NotEmpty(message = "排序列表不能为空") private List<String> ids;
}
