package com.samoy.chuanbillserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
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
}
