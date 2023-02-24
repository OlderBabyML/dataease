package io.dataease.listener;

import io.dataease.service.chart.ChartViewService;
import lombok.SneakyThrows;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Order(value = 2)
public class AlarmInitStartListener implements ApplicationListener<ApplicationReadyEvent> {
    @Resource
    private ChartViewService chartViewService;

    @SneakyThrows
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        chartViewService.addScheduler();
    }
}
