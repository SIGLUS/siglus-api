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
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@Data
public class CallFcService {

  @Autowired
  RestTemplate remoteRestTemplate;

  private List<IssueVoucherDto> issueVouchers = new ArrayList<>();

  private List<ReceiptPlanDto> receiptPlans = new ArrayList<>();

  private PageInfoDto pageInfoDto = new PageInfoDto();

  @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 5000,
      multiplier = 2))
  public <T> void fetchData(String url, Class<T[]> clazz) {
    String param = url.split("psize=20&")[1];
    log.info("[FC] fetch {}: {}", clazz.getSimpleName(), param);
    try {
      ResponseEntity<T[]> responseEntity = remoteRestTemplate.getForEntity(url, clazz);
      T[] body = responseEntity.getBody();
      if (body.length == 0) {
        log.info("[FC] fetch {}: no result returned from fc", clazz.getName());
        return;
      }
      setPageInfo(responseEntity.getHeaders());
      updateResponseResult(clazz, body);
    } catch (Exception e) {
      log.warn("[FC] fetch {} failed: {}, retry...", clazz.getSimpleName(), param);
      throw e;
    }
  }

  @Recover
  public void recover(RuntimeException e) {
    log.error("[FC] call fc api failed with retry 5 times, message: {}", e.getMessage());
    throw e;
  }

  private <T> void updateResponseResult(Class<T[]> clazz, Object[] body) {
    if (clazz.equals(IssueVoucherDto[].class)) {
      this.issueVouchers.addAll(Arrays.asList((IssueVoucherDto[]) body));
    } else if (clazz.equals(ReceiptPlanDto[].class)) {
      this.receiptPlans.addAll(Arrays.asList((ReceiptPlanDto[]) body));
    }
  }

  private void setPageInfo(HttpHeaders headers) {
    int totalObjects = Integer.parseInt(headers.get("TotalObjects").get(0));
    int totalPages = Integer.parseInt(headers.get("TotalPages").get(0));
    int pageNumber = Integer.parseInt(headers.get("PageNumber").get(0));
    int pageSize = Integer.parseInt(headers.get("PSize").get(0));
    this.pageInfoDto = PageInfoDto.builder()
        .totalObjects(totalObjects)
        .totalPages(totalPages)
        .pageNumber(pageNumber)
        .pageSize(pageSize)
        .build();
    if (pageNumber == 1) {
      log.info("[FC] page info: {}", pageInfoDto);
    }
  }

}
