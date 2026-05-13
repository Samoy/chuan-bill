package com.samoy.chuanbillserver.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.samoy.chuanbillserver.entity.Bill;
import com.samoy.chuanbillserver.entity.UserPreference;
import com.samoy.chuanbillserver.service.IBillService;
import com.samoy.chuanbillserver.service.IMessageService;
import com.samoy.chuanbillserver.service.IUserPreferenceService;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillReminderJob implements Job {

    private static final String KEY_MASTER = "notification.master.enabled";
    private static final String KEY_ENABLED = "notification.billReminder.enabled";
    private static final String KEY_TIME = "notification.billReminder.time";
    private static final String KEY_LAST_SENT = "notification.billReminder.lastSentDate";

    @Resource
    private IUserPreferenceService userPreferenceService;

    @Resource
    private IBillService billService;

    @Resource
    private IMessageService messageService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        log.debug("BillReminderJob 执行，当前时间: {}, 今天: {}", currentTime, today);

        // 查询所有开启提醒且时间匹配当前时间的用户
        List<UserPreference> enabledPrefs = userPreferenceService.list(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getPrefKey, KEY_ENABLED)
                .eq(UserPreference::getPrefValue, "true"));

        for (UserPreference enabledPref : enabledPrefs) {
            String userId = enabledPref.getUserId();

            try {
                // 检查总开关是否开启
                String masterEnabled = userPreferenceService.getValue(userId, KEY_MASTER);
                if (!"true".equals(masterEnabled)) {
                    continue;
                }

                // 检查提醒时间是否匹配
                String reminderTime = userPreferenceService.getValue(userId, KEY_TIME);
                if (!currentTime.equals(reminderTime)) {
                    continue;
                }

                // 检查是否今天已发送过
                String lastSentDate = userPreferenceService.getValue(userId, KEY_LAST_SENT);
                if (today.equals(lastSentDate)) {
                    continue;
                }

                // 检查今天是否已有账单
                LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
                LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
                long billCount = billService.count(new LambdaQueryWrapper<Bill>()
                        .eq(Bill::getUserId, userId)
                        .ge(Bill::getTime, startOfDay)
                        .le(Bill::getTime, endOfDay));

                if (billCount > 0) {
                    // 今天已有账单，记录已发送避免重复检查
                    userPreferenceService.setValueInternal(userId, KEY_LAST_SENT, today);
                    continue;
                }

                // 发送提醒消息
                messageService.sendMessage(userId, "记账提醒", "您今天还没有记账哦，点击记录一笔", "system", null, null);

                // 记录已发送日期
                userPreferenceService.setValueInternal(userId, KEY_LAST_SENT, today);
                log.info("已发送记账提醒给用户: {}", userId);
            } catch (Exception e) {
                log.error("发送记账提醒失败，用户: {}", userId, e);
            }
        }
    }
}
