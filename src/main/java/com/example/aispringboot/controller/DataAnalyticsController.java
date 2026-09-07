package com.example.aispringboot.controller;

import com.example.aispringboot.common.Result;
import com.example.aispringboot.dto.response.DataAnalyticsOverviewDTO;
import com.example.aispringboot.service.system.DataAnalyticsService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据看板（管理员）：全站统计与 7 日趋势，Redis 缓存 5 分钟。
 */
@RestController
@RequestMapping("/api/data-analytics")
public class DataAnalyticsController {

    @Resource
    private DataAnalyticsService dataAnalyticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('2')")
    public Result<DataAnalyticsOverviewDTO> getOverview() {
        return Result.ok(dataAnalyticsService.getOverview());
    }
}
