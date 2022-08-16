package org.siglus.siglusapi.task;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.siglus.siglusapi.service.task.report.CalculateWebCmmService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CalculateWebCmmTask {

  private final CalculateWebCmmService calculateWebCmmService;

  @Scheduled(cron = "${cmm.calculate.cron}", zone = "${time.zoneId}")
  @SchedulerLock(name = "calculate_cmm_task")
  public void calculate() {
    calculateWebCmmService.calculateCurrentPeriod();
  }
}
