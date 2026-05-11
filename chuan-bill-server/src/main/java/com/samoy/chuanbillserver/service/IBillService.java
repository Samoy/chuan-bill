package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.AddBillDTO;
import com.samoy.chuanbillserver.dto.BatchCreateBillDTO;
import com.samoy.chuanbillserver.dto.BillListDTO;
import com.samoy.chuanbillserver.dto.BillMonthlyStatsDTO;
import com.samoy.chuanbillserver.dto.UpdateBillDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.vo.BatchSyncResultVO;
import com.samoy.chuanbillserver.vo.BillMonthlyStatsVO;
import com.samoy.chuanbillserver.vo.BillVO;
import java.util.List;

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
     * @return 账单列表
     */
    List<BillVO> getBillList(String userId, BillListDTO billListDTO);

    /**
     * 分页获取账单列表
     *
     * @param userId      用户ID
     * @param billListDTO 账单列表DTO
     * @return 账单分页列表
     */
    IPage<BillVO> getBillListByPage(String userId, BillListDTO billListDTO);

    /**
     * 添加账单
     *
     * @param userId     用户ID
     * @param addBillDTO 账单信息
     * @return 是否添加成功
     */
    boolean addBill(String userId, AddBillDTO addBillDTO);

    /**
     * 批量添加账单
     *
     * @param userId 用户ID
     * @param dto    批量账单信息
     * @return 批量同步结果，包含每条账单的同步状态
     */
    BatchSyncResultVO batchCreate(String userId, BatchCreateBillDTO dto);

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

    /**
     * 获取月账单统计
     * @param userId 用户ID
     * @param billMonthlyStatsDTO 月账单统计DTO
     * @return 月账单统计信息
     */
    BillMonthlyStatsVO getMonthlyStats(String userId, BillMonthlyStatsDTO billMonthlyStatsDTO);
}
