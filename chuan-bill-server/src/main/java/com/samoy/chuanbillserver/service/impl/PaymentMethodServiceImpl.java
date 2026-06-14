package com.samoy.chuanbillserver.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.dao.PaymentMethodMapper;
import com.samoy.chuanbillserver.dto.AddPaymentMethodDTO;
import com.samoy.chuanbillserver.dto.UpdatePaymentMethodDTO;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.entity.PaymentMethod;
import com.samoy.chuanbillserver.exception.BusinessException;
import com.samoy.chuanbillserver.result.ResultEnum;
import com.samoy.chuanbillserver.service.IPaymentMethodService;
import com.samoy.chuanbillserver.vo.PaymentMethodVO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Resource
    private BillMapper billMapper;

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

    @Override
    @Transactional
    public PaymentMethodVO addPaymentMethod(String userId, AddPaymentMethodDTO dto) {
        LambdaQueryWrapper<PaymentMethod> maxQuery = new LambdaQueryWrapper<>();
        maxQuery.and(i -> i.eq(PaymentMethod::getUserId, userId).or(j -> j.eq(PaymentMethod::getIsDefault, true)));
        List<PaymentMethod> existing = this.list(maxQuery);
        int maxSortOrder =
                existing.stream().mapToInt(PaymentMethod::getSortOrder).max().orElse(0);

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(IdUtil.fastSimpleUUID());
        paymentMethod.setName(dto.getName());
        paymentMethod.setIcon(dto.getIcon());
        paymentMethod.setSortOrder(maxSortOrder + 1);
        paymentMethod.setIsDefault(false);
        paymentMethod.setUserId(userId);
        paymentMethod.setDeleted(false);
        this.save(paymentMethod);

        return toVO(paymentMethod);
    }

    @Override
    @Transactional
    public PaymentMethodVO updatePaymentMethod(String userId, String id, UpdatePaymentMethodDTO dto) {
        PaymentMethod paymentMethod = this.getById(id);
        if (paymentMethod == null || Boolean.TRUE.equals(paymentMethod.getDeleted())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(paymentMethod.getIsDefault())) {
            throw new BusinessException(ResultEnum.CANNOT_MODIFY_DEFAULT_PAYMENT_METHOD);
        }
        if (!userId.equals(paymentMethod.getUserId())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }

        if (dto.getName() != null) {
            paymentMethod.setName(dto.getName());
        }
        if (dto.getIcon() != null) {
            paymentMethod.setIcon(dto.getIcon());
        }
        this.updateById(paymentMethod);

        return toVO(paymentMethod);
    }

    @Override
    @Transactional
    public void deletePaymentMethod(String userId, String id) {
        PaymentMethod paymentMethod = this.getById(id);
        if (paymentMethod == null || Boolean.TRUE.equals(paymentMethod.getDeleted())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(paymentMethod.getIsDefault())) {
            throw new BusinessException(ResultEnum.CANNOT_MODIFY_DEFAULT_PAYMENT_METHOD);
        }
        if (!userId.equals(paymentMethod.getUserId())) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_NOT_FOUND);
        }

        LambdaQueryWrapper<Bill> billQuery = new LambdaQueryWrapper<>();
        billQuery.eq(Bill::getPaymentMethodId, id).last("LIMIT 1");
        if (billMapper.selectCount(billQuery) > 0) {
            throw new BusinessException(ResultEnum.PAYMENT_METHOD_HAS_BILLS);
        }

        this.removeById(id);
    }

    @Override
    @Transactional
    public void sortPaymentMethods(String userId, List<String> ids) {
        LambdaQueryWrapper<PaymentMethod> presetQuery = new LambdaQueryWrapper<>();
        presetQuery.eq(PaymentMethod::getIsDefault, true);
        List<PaymentMethod> presetMethods = this.list(presetQuery);
        int maxPresetSortOrder =
                presetMethods.stream().mapToInt(PaymentMethod::getSortOrder).max().orElse(0);

        int sortOrder = maxPresetSortOrder + 1;
        for (String id : ids) {
            PaymentMethod paymentMethod = this.getById(id);
            if (paymentMethod != null
                    && userId.equals(paymentMethod.getUserId())
                    && Boolean.FALSE.equals(paymentMethod.getIsDefault())) {
                paymentMethod.setSortOrder(sortOrder++);
                this.updateById(paymentMethod);
            }
        }
    }

    private PaymentMethodVO toVO(PaymentMethod paymentMethod) {
        PaymentMethodVO vo = new PaymentMethodVO();
        vo.setId(paymentMethod.getId());
        vo.setName(paymentMethod.getName());
        vo.setIcon(paymentMethod.getIcon());
        vo.setSortOrder(paymentMethod.getSortOrder());
        vo.setIsDefault(paymentMethod.getIsDefault());
        vo.setUserId(paymentMethod.getUserId());
        return vo;
    }
}
