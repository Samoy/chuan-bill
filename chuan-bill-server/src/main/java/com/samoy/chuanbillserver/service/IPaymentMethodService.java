package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samoy.chuanbillserver.dto.AddPaymentMethodDTO;
import com.samoy.chuanbillserver.dto.UpdatePaymentMethodDTO;
import com.samoy.chuanbillserver.entity.PaymentMethod;
import com.samoy.chuanbillserver.vo.PaymentMethodVO;
import java.util.List;

/**
 * <p>
 * 支付方式表 服务类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
public interface IPaymentMethodService extends IService<PaymentMethod> {

    /**
     * 获取用户支付方式列表
     *
     * @param userId 用户ID
     * @return 支付方式列表
     */
    List<PaymentMethodVO> getPaymentMethods(String userId);

    /**
     * 新增支付方式
     *
     * @param userId 用户ID
     * @param dto    新增支付方式DTO
     * @return 支付方式VO
     */
    PaymentMethodVO addPaymentMethod(String userId, AddPaymentMethodDTO dto);

    /**
     * 更新支付方式
     *
     * @param userId 用户ID
     * @param id     支付方式ID
     * @param dto    更新支付方式DTO
     * @return 支付方式VO
     */
    PaymentMethodVO updatePaymentMethod(String userId, String id, UpdatePaymentMethodDTO dto);

    /**
     * 删除支付方式
     *
     * @param userId 用户ID
     * @param id     支付方式ID
     */
    void deletePaymentMethod(String userId, String id);

    /**
     * 支付方式排序
     *
     * @param userId 用户ID
     * @param ids    支付方式ID列表（按新顺序）
     */
    void sortPaymentMethods(String userId, List<String> ids);
}
