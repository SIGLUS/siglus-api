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

import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.siglus.common.util.SiglusDateHelper;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.PageInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${fc.domain}")
  private String domain;

  @Value("${fc.key}")
  private String key;

  @Value("${fc.query.date}")
  private String date;

  @Autowired
  RestTemplate remoteRestTemplate;

  @Autowired
  SiglusDateHelper dateHelper;

  private List<IssueVoucherDto> issueVouchers;

  private PageInfoDto pageInfoDto;

  @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 5000,
      multiplier = 2))
  public void fetchIssueVouchers(int page) throws Exception {
    String url = getUrl("/issueVoucher/issuevouchers");
    log.info("[FC] call fetchIssueVouchers, date: {}, page: {}", date, page);
    try {
      ResponseEntity<IssueVoucherDto[]> issueVoucherResponse =
          remoteRestTemplate.getForEntity(url + page, IssueVoucherDto[].class);

      IssueVoucherDto[] body = issueVoucherResponse.getBody();
      if (body.length == 0) {
        log.info("[FC] fetchIssueVouchersFromFc: no result returned from fc");
        return;
      }
      if (page == 1) {
        setPageInfo(issueVoucherResponse.getHeaders());
      }
      this.issueVouchers.addAll(Arrays.asList(body));
    } catch (Exception e) {
      log.warn("[FC] call fetchIssueVouchers failed, date: {}, page: {}, retry...", date, page);
      throw e;
    }
  }

  @Recover
  public void recover(RuntimeException e) {
    log.error("[FC] call fc api failed with retry 5 times, message: {}", e.getMessage());
    throw e;
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
    log.info("[FC] page info: {}", pageInfoDto);
  }

  private String getUrl(String path) {
    return getUrl(path, false);
  }

  private String getUrl(String path, boolean isCmmOrCp) {
    String url = domain + path + "?key=" + key + "&psize=20";
    if (isCmmOrCp) {
      String period = dateHelper.getCurrentMonthStr();
      return url + "period=" + period + "&page=";
    }
    if (date == null || date.isEmpty()) {
      date = dateHelper.getYesterdayDateStr();
    }
    return url + "&date=" + date + "&page=";
  }
}
