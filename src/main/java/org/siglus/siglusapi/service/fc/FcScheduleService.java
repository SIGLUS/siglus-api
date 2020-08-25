package org.siglus.siglusapi.service.fc;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class FcScheduleService {

  @Autowired
  RestTemplate remoteRestTemplate;

  @Autowired
  CallFcService callFcService;

  @Scheduled(cron = "${fc.issuevoucher.cron}", zone = "${time.zoneId}")
  public void fetchIssueVouchersFromFc() throws Exception {
    long startTime = System.currentTimeMillis();
    log.info("[FC] fetchIssueVouchersFromFc start");
    callFcService.setIssueVouchers(new ArrayList<>());
    callFcService.setPageInfoDto(new PageInfoDto());
    for (int page = 1; page <= callFcService.getPageInfoDto().getTotalPages(); page++) {
      callFcService.fetchIssueVouchers(page);
    }
    log.info("[FC] issue voucher size: {}", callFcService.getIssueVouchers().size());
    log.info("[FC] fetchIssueVouchersFromFc finish, cost: {}ms",
        System.currentTimeMillis() - startTime);
  }

}
