package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.vo.BillVO;

/**
 * <p>
 * 账单表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
public interface IBillService extends IService<Bill> {

    /**
     * 获取账单列表
     *
     * @param userId      用户ID
     * @param billListDTO 账单列表DTO
     * @return 账单分页列表
     */
    IPage<BillVO> getBillList(String userId, BillListDTO billListDTO);

    /**
     * 添加账单
     *
     * @param userId     用户ID
     * @param addBillDTO 账单信息
     * @return 是否添加成功
     */
    boolean addBill(String userId, AddBillDTO addBillDTO);

    /**
     * 更新账单
     *
     * @param userId        用户ID
     * @param updateBillDTO 账单信息
     * @return 是否更新成功
     */
    boolean updateBill(String userId, UpdateBillDTO updateBillDTO);

    /**
     * 删除账单
     *
     * @param userId 用户ID
     * @param billId 账单ID
     * @return 是否删除成功
     */
    boolean deleteBill(String userId, String billId);

    /**
     * 获取账单详情
     *
     * @param userId 用户ID
     * @param billId 账单ID
     * @return 账单详情
     */
    BillVO getBillDetail(String userId, String billId);
}
