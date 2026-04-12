package com.samoy.chuanbillserver.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 统计分析控制器
 * </p>
 *
 * @author samoy
 * @since 2026/4/11
 */
@Tag(name = "statistics", description = "统计分析相关接口")
@RestController
@RequestMapping("/statistics")
public class StatisticsController {}
