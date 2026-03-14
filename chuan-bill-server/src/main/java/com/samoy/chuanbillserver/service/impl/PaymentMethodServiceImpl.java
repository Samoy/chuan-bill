package com.samoy.chuanbillserver.service.impl;

import com.samoy.chuanbillserver.entity.PaymentMethod;
import com.samoy.chuanbillserver.dao.PaymentMethodMapper;
import com.samoy.chuanbillserver.service.IPaymentMethodService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 支付方式表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class PaymentMethodServiceImpl extends ServiceImpl<PaymentMethodMapper, PaymentMethod> implements IPaymentMethodService {

}
