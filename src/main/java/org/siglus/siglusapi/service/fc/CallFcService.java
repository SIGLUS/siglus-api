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

  @Retryable(value = Exception.class, maxAttempts = 6, backoff = @Backoff(delay = 10000))
  public void fetchIssueVouchers(int page) {
    String url= getUrl("/issueVoucher/issuevouchers");
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
      log.error("[FC] call fetchIssueVouchers error, message: {}", e.getMessage());
      throw e;
    }
  }

  @Recover
  public void recover(RuntimeException e){
    log.error("[FC] call fc api failed with retry, message: {}", e.getMessage());
  }

  private void setPageInfo(HttpHeaders headers) {
    int totalObjects = Integer.parseInt(headers.get("TotalObjects").get(0));
    int totalPages = Integer.parseInt(headers.get("TotalPages").get(0));
    int pageNumber = Integer.parseInt(headers.get("PageNumber").get(0));
    int pSize = Integer.parseInt(headers.get("PSize").get(0));
    this.pageInfoDto = PageInfoDto.builder()
        .totalObjects(totalObjects)
        .totalPages(totalPages)
        .pageNumber(pageNumber)
        .pSize(pSize)
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
      log.info("[FC] query period: {}", period);
      return url + "period=" + period + "&page=";
    }
    if (date == null || date.isEmpty()) {
      date = dateHelper.getYesterdayDateStr();
    }
    log.info("[FC] query date: {}", date);
    return url + "&date=" + date + "&page=";
  }
}
