package com.samoy.chuanbillserver.service.impl;

import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.dao.BillMapper;
import com.samoy.chuanbillserver.service.IBillService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 账单表 服务实现类
 * </p>
 *
 * @author Samoy
 * @since 2026-03-14
 */
@Service
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements IBillService {

}
