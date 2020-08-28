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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.siglus.siglusapi.dto.fc.IssueVoucherDto;
import org.siglus.siglusapi.dto.fc.ReceiptPlanDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@EnableRetry
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CallFcService.class})
public class CallFcServiceTest {

  @MockBean
  private RestTemplate remoteRestTemplate;

  @Autowired
  private CallFcService callFcService;
  
  public static final String URL = "http://localhost/test/tests?psize=20&page=1";

  @Before
  public void setup() {
    callFcService.setIssueVouchers(new ArrayList<>());
    callFcService.setReceiptPlans(new ArrayList<>());
  }

  @Test(expected = Exception.class)
  public void shouldRetry5TimesAndThrowExceptionWhenFetchDataFailed() {
    // given
    Class<IssueVoucherDto[]> clazz = IssueVoucherDto[].class;
    when(remoteRestTemplate.getForEntity(URL, clazz))
        .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    // when
    callFcService.fetchData(URL, clazz);
  }

  @Test
  public void shouldGetIssueVoucherWhenFetchDataSuccess() {
    // given
    MultiValueMap<String, String> headers = getHeaders("1");
    Class<IssueVoucherDto[]> clazz = IssueVoucherDto[].class;
    IssueVoucherDto[] issueVoucherDtos = {new IssueVoucherDto()};
    when(remoteRestTemplate.getForEntity(URL, clazz))
        .thenReturn(new ResponseEntity<>(issueVoucherDtos, headers, HttpStatus.OK));

    // when
    callFcService.fetchData(URL, clazz);

    // then
    verify(remoteRestTemplate).getForEntity(eq(URL), eq(IssueVoucherDto[].class));
    Assert.assertEquals(1, callFcService.getIssueVouchers().size());
  }

  @Test
  public void shouldGetReceiptPlanWhenFetchDataSuccess() {
    // given
    MultiValueMap<String, String> headers = getHeaders("2");
    Class<ReceiptPlanDto[]> clazz = ReceiptPlanDto[].class;
    ReceiptPlanDto[] receiptPlanDtos = {new ReceiptPlanDto()};
    when(remoteRestTemplate.getForEntity(URL, clazz))
        .thenReturn(new ResponseEntity<>(receiptPlanDtos, headers, HttpStatus.OK));

    // when
    callFcService.fetchData(URL, clazz);

    // then
    verify(remoteRestTemplate).getForEntity(eq(URL), eq(ReceiptPlanDto[].class));
    Assert.assertEquals(1, callFcService.getReceiptPlans().size());
  }

  @Test
  public void shouldNotGetIssueVoucherWhenFetchDataIsEmpty() {
    // given
    MultiValueMap<String, String> headers = getHeaders("1");
    Class<IssueVoucherDto[]> clazz = IssueVoucherDto[].class;
    IssueVoucherDto[] issueVoucherDtos = {};
    when(remoteRestTemplate.getForEntity(URL, clazz))
        .thenReturn(new ResponseEntity<>(issueVoucherDtos, headers, HttpStatus.OK));

    // when
    callFcService.fetchData(URL, clazz);

    // then
    verify(remoteRestTemplate).getForEntity(eq(URL), eq(IssueVoucherDto[].class));
    Assert.assertEquals(0, callFcService.getIssueVouchers().size());
  }

  @Test
  public void shouldNotUpdateListIfClazzIsWrong() {
    // given
    MultiValueMap<String, String> headers = getHeaders("1");
    Class<Object[]> clazz = Object[].class;
    Object[] objects = {new Object()};
    when(remoteRestTemplate.getForEntity(URL, clazz))
        .thenReturn(new ResponseEntity<>(objects, headers, HttpStatus.OK));

    // when
    callFcService.fetchData(URL, clazz);

    // then
    verify(remoteRestTemplate).getForEntity(eq(URL), eq(Object[].class));
    Assert.assertEquals(0, callFcService.getIssueVouchers().size());
    Assert.assertEquals(0, callFcService.getReceiptPlans().size());
  }

  private MultiValueMap<String, String> getHeaders(String s) {
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.add("TotalObjects", "1");
    headers.add("TotalPages", "1");
    headers.add("PageNumber", s);
    headers.add("PSize", "1");
    return headers;
  }

}
