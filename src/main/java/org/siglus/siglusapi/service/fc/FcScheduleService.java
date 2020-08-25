/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

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
    final long startTime = System.currentTimeMillis();
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
