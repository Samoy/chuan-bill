package com.samoy.chuanbillserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.result.Result;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.vo.BillVO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bill")
public class BillController {

    @Resource
    private IBillService billService;

    @GetMapping("/list")
    public Result<IPage<BillVO>> getBillList(@Validated @ModelAttribute BillListDTO billListDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.getBillList(userId, billListDTO));
    }

    @GetMapping("/detail")
    public Result<BillVO> getBillDetail(String id) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.getBillDetail(userId, id));
    }

    @PostMapping("/add")
    public Result<Boolean> addBill(@Validated @RequestBody AddBillDTO addBillDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.addBill(userId, addBillDTO));
    }

    @PostMapping("/update")
    public Result<Boolean> updateBill(@Validated @RequestBody UpdateBillDTO updateBillDTO) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.updateBill(userId, updateBillDTO));
    }

    @PostMapping("/delete")
    public Result<Boolean> deleteBill(String id) {
        String userId = StpUtil.getLoginIdAsString();
        return Result.success(billService.deleteBill(userId, id));
    }
}
