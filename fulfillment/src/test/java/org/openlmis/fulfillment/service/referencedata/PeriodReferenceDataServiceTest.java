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

package org.openlmis.fulfillment.service.referencedata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.fulfillment.service.PageDto;
import org.openlmis.fulfillment.util.DynamicPageTypeReference;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class PeriodReferenceDataServiceTest
    extends BaseReferenceDataServiceTest<ProcessingPeriodDto> {

  private PeriodReferenceDataService service;

  @Override
  protected BaseReferenceDataService<ProcessingPeriodDto> getService() {
    return new PeriodReferenceDataService();
  }

  @Override
  protected ProcessingPeriodDto generateInstance() {
    return new ProcessingPeriodDto();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    service = (PeriodReferenceDataService) prepareService();
  }

  @Test
  public void shouldReturnOrderablesById() {
    ProcessingPeriodDto period = mockPageResponseEntityAndGetDto();

    String startDate = "2018-04-05";
    String endDate = "2018-05-05";
    List<ProcessingPeriodDto> response = service
        .search(LocalDate.parse(startDate), LocalDate.parse(endDate));

    assertThat(response, hasSize(1));
    assertThat(response, hasItems(period));

    verify(restTemplate).exchange(
        uriCaptor.capture(), eq(HttpMethod.GET), entityCaptor.capture(),
        refEq(new DynamicPageTypeReference<>(ProcessingPeriodDto.class)));

    URI uri = uriCaptor.getValue();
    assertEquals(serviceUrl + service.getUrl() + "?startDate=" + startDate + "&endDate=" + endDate,
        uri.toString());

    assertAuthHeader(entityCaptor.getValue());
    assertNull(entityCaptor.getValue().getBody());
  }

  @Test
  public void shouldFindPeriodsByIds() {
    // given
    UUID id = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    List<UUID> ids = Arrays.asList(id, id2);

    ProcessingPeriodDto period = generateInstance();
    period.setId(id);
    ProcessingPeriodDto anotherPeriod = generateInstance();
    anotherPeriod.setId(id2);

    Map<String, Object> payload = new HashMap<>();
    payload.put("id", ids);
    ResponseEntity response = mock(ResponseEntity.class);

    // when
    when(response.getBody()).thenReturn(
        new PageDto<>(new PageImpl<>(Arrays.asList(period, anotherPeriod)))
    );
    when(restTemplate.exchange(
        any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class),
        any(ParameterizedTypeReference.class)
    )).thenReturn(response);

    List<ProcessingPeriodDto> periods = service.findByIds(ids);

    // then
    verify(restTemplate).exchange(
        uriCaptor.capture(), eq(HttpMethod.GET),
        entityCaptor.capture(), any(ParameterizedTypeReference.class)
    );
    assertTrue(periods.contains(period));
    assertTrue(periods.contains(anotherPeriod));

    String actualUrl = uriCaptor.getValue().toString();
    assertTrue(actualUrl.startsWith(service.getServiceUrl() + service.getUrl()));
    assertTrue(actualUrl.contains(id.toString()));
    assertTrue(actualUrl.contains(id2.toString()));

    assertAuthHeader(entityCaptor.getValue());
  }

  @Test
  public void shouldReturnEmptyListWhenFindingPeriodsWithNoIdsProvided() {
    // given
    checkAuth = false;
    // when
    List<ProcessingPeriodDto> periods = service.findByIds(Collections.emptyList());
    // then
    Assert.assertThat(periods, empty());
  }
}
