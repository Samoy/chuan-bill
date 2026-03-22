package com.samoy.chuanbillserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.PaymentMethodMapper;
import com.samoy.chuanbillserver.entity.PaymentMethod;
import com.samoy.chuanbillserver.service.IPaymentMethodService;
import com.samoy.chuanbillserver.vo.PaymentMethodVO;
import java.util.List;
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
public class PaymentMethodServiceImpl extends ServiceImpl<PaymentMethodMapper, PaymentMethod>
        implements IPaymentMethodService {
    @Override
    public List<PaymentMethodVO> getPaymentMethods(String userId) {
        LambdaQueryWrapper<PaymentMethod> queryWrapper = new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getUserId, userId)
                .or(i -> i.eq(PaymentMethod::getIsDefault, true))
                .orderByAsc(PaymentMethod::getSortOrder);
        List<PaymentMethod> paymentMethods = this.list(queryWrapper);

        return paymentMethods.stream()
                .map(paymentMethod -> {
                    PaymentMethodVO vo = new PaymentMethodVO();
                    vo.setId(paymentMethod.getId());
                    vo.setName(paymentMethod.getName());
                    vo.setIcon(paymentMethod.getIcon());
                    vo.setSortOrder(paymentMethod.getSortOrder());
                    vo.setIsDefault(paymentMethod.getIsDefault());
                    vo.setUserId(paymentMethod.getUserId());
                    return vo;
                })
                .toList();
    }
}
