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

package org.openlmis.referencedata.service;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.OrderableRepository;
import org.openlmis.referencedata.repository.custom.OrderableRepositoryCustom.SearchParams;
import org.openlmis.referencedata.util.Pagination;
import org.openlmis.referencedata.web.QueryOrderableSearchParams;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.TooManyMethods"})
public class OrderableServiceTest {

  private static final String CODE = "code";
  private static final String NAME = "name";
  private static final String PROGRAM_CODE = "program";
  private static final String ID = "id";
  private static final String GMT = "GMT";

  @Mock
  private OrderableRepository orderableRepository;

  @Mock
  private Orderable orderable1;

  @Mock
  private Orderable orderable2;

  @Mock
  private Pageable pageable;

  @Mock
  private Profiler profiler;

  private UUID orderableId = UUID.randomUUID();
  private String programCode = "program-code";
  private List<Orderable> orderableList;
  private MultiValueMap<String, Object> searchParams = new LinkedMultiValueMap<>();
  private ZonedDateTime modifiedDate = ZonedDateTime.now(ZoneId.of(GMT)).withNano(0);

  @InjectMocks
  private OrderableService orderableService = new OrderableService();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    orderableList = Lists.newArrayList(orderable1, orderable2);
    when(orderable2.getId()).thenReturn(orderableId);
    when(orderable1.getLastUpdated()).thenReturn(modifiedDate.minusHours(1));
    when(orderable2.getLastUpdated()).thenReturn(modifiedDate);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfProgramCodeAndNameNotProvidedForSearch() {
    searchParams.add("some-parameter", false);
    orderableService.searchOrderables(new QueryOrderableSearchParams(searchParams), null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfProvidedAnyNotSupportedParameter() {
    searchParams.add("some-parameter", false);
    searchParams.add("code", "123");
    orderableService.searchOrderables(new QueryOrderableSearchParams(searchParams), null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfProvidedIdParameterIsNull() {
    searchParams.add(ID, "null");
    orderableService.getLatestLastUpdatedDate(
            new QueryOrderableSearchParams(searchParams),null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfProvidedIdParameterIsMalformed() {
    searchParams.add(ID, "1234");
    orderableService.getLatestLastUpdatedDate(
            new QueryOrderableSearchParams(searchParams),null);
  }

  @Test
  public void shouldNotThrowValidationExceptionIfQueryMapCanBeParsed() {
    Page<Orderable> thePage = Pagination.getPage(orderableList, null);
    when(orderableRepository.findAllLatest(null)).thenReturn(thePage);

    searchParams.add(CODE, "-1");
    searchParams.add(NAME, "-1");
    searchParams.add(PROGRAM_CODE, "program-code");
    orderableService.searchOrderables(new QueryOrderableSearchParams(searchParams), null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfProgramCodeAndNameNotProvidedForGettingLastUpdatedDate() {
    searchParams.add("parameter", false);
    orderableService.getLatestLastUpdatedDate(
            new QueryOrderableSearchParams(searchParams),null);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionIfProvidedAnyNotSupportedParameterWhenGettingLastUpdatedDate() {
    searchParams.add("parameter", false);
    searchParams.add("code", "123");
    orderableService.getLatestLastUpdatedDate(
            new QueryOrderableSearchParams(searchParams),null);
  }

  @Test
  public void shouldReturnAllElementsIfNoSearchCriteriaProvided() {
    // given
    Page<Orderable> thePage = Pagination.getPage(orderableList, null);
    when(orderableRepository.findAllLatest(any(Pageable.class)))
        .thenReturn(thePage);

    // when
    Page<Orderable> actual = orderableService.searchOrderables(new QueryOrderableSearchParams(
        searchParams), null);

    // then
    verify(orderableRepository).findAllLatest(isNull(Pageable.class));
    assertEquals(thePage, actual);
  }

  @Test
  public void shouldReturnAllElementsIfQueryMapIsNull() {
    // given
    Page<Orderable> thePage = Pagination.getPage(orderableList, null);
    when(orderableRepository.findAllLatest(any(Pageable.class)))
        .thenReturn(thePage);

    // when
    final Page<Orderable> actual =
        orderableService.searchOrderables(new QueryOrderableSearchParams(null), null);

    // then
    verify(orderableRepository).findAllLatest(isNull(Pageable.class));
    assertEquals(thePage, actual);
  }

  @Test
  public void shouldSearchForOrderables() {
    // given
    final String code = "ORD1";
    final String name = "Orderable";

    given(orderableRepository.search(
        any(SearchParams.class),
        any(Pageable.class)))
        .willReturn(Pagination.getPage(Lists.newArrayList(orderable1, orderable2)));

    searchParams.add(CODE, code);
    searchParams.add(NAME, name);
    searchParams.add(PROGRAM_CODE, programCode);

    QueryOrderableSearchParams queryMap = new QueryOrderableSearchParams(searchParams);

    // when
    final Page<Orderable> actual =
        orderableService.searchOrderables(queryMap, pageable);

    //then
    verify(orderableRepository).search(queryMap, pageable);

    assertEquals(2, actual.getTotalElements());
    assertThat(actual, hasItem(orderable1));
    assertThat(actual, hasItem(orderable2));
  }

  @Test
  public void shouldFindOrderablesByIds() {
    // given
    given(orderableRepository.findAllLatestByIds(anySetOf(UUID.class), any(Pageable.class)))
        .willReturn(Pagination.getPage(Lists.newArrayList(orderable2)));

    searchParams.add(ID, orderableId.toString());
    UUID orderableId2 = UUID.randomUUID();
    searchParams.add(ID, orderableId2.toString());

    // when
    final Page<Orderable> actual =
        orderableService.searchOrderables(new QueryOrderableSearchParams(searchParams), pageable);

    // then
    verify(orderableRepository).findAllLatestByIds(
        new HashSet<>(Arrays.asList(orderableId, orderableId2)), pageable);

    assertEquals(1, actual.getTotalElements());
    assertThat(actual, hasItem(orderable2));
  }

  @Test
  public void shouldReturnLatestModifiedDateOfAllElementsIfNoSearchCriteriaProvided() {
    // given
    Timestamp time = Timestamp.valueOf(orderable2.getLastUpdated().toLocalDateTime());
    when(orderableRepository.findLatestModifiedDateOfAll()).thenReturn(time);

    // when
    ZonedDateTime lastUpdated = orderableService.getLatestLastUpdatedDate(
              new QueryOrderableSearchParams(searchParams), profiler);

    // then
    verify(orderableRepository).findLatestModifiedDateOfAll();
    assertEquals(orderable2.getLastUpdated(), lastUpdated);
  }

  @Test
  public void shouldReturnLatestModifiedDateOfAllElementsIfQueryMapIsNull() {
    // given
    Timestamp time = Timestamp.valueOf(orderable2.getLastUpdated().toLocalDateTime());
    when(orderableRepository.findLatestModifiedDateOfAll()).thenReturn(time);

    // when
    ZonedDateTime lastUpdated = orderableService.getLatestLastUpdatedDate(
            new QueryOrderableSearchParams(null), profiler);

    // then
    verify(orderableRepository).findLatestModifiedDateOfAll();
    assertEquals(orderable2.getLastUpdated(), lastUpdated);
  }

  @Test
  public void shouldReturnLatestModifiedDateWhenSearchingForOrderablesWithParams() {
    // given
    final String code = "ORD1";
    final String name = "Orderable";

    given(orderableRepository.findLatestModifiedDateByParams(
            any(QueryOrderableSearchParams.class)))
            .willReturn(modifiedDate);

    searchParams.add(CODE, code);
    searchParams.add(NAME, name);
    searchParams.add(PROGRAM_CODE, programCode);

    QueryOrderableSearchParams queryMap = new QueryOrderableSearchParams(searchParams);

    // when
    ZonedDateTime lastUpdated = orderableService.getLatestLastUpdatedDate(
            queryMap, profiler);

    //then
    verify(orderableRepository).findLatestModifiedDateByParams(queryMap);
    assertEquals(orderable2.getLastUpdated(), lastUpdated);
  }

  @Test
  public void shouldFindLatestModifiedDateByOrderableIds() {
    // given
    Timestamp time = Timestamp.valueOf(orderable2.getLastUpdated().toLocalDateTime());
    given(orderableRepository.findLatestModifiedDateByIds(anySetOf(UUID.class))).willReturn(time);

    searchParams.add(ID, orderableId.toString());
    UUID orderableId2 = UUID.randomUUID();
    searchParams.add(ID, orderableId2.toString());

    QueryOrderableSearchParams queryMap = new QueryOrderableSearchParams(searchParams);

    // when
    ZonedDateTime lastUpdated = orderableService.getLatestLastUpdatedDate(
            queryMap, profiler);

    // then
    verify(orderableRepository).findLatestModifiedDateByIds(queryMap.getIds());
    assertEquals(orderable2.getLastUpdated(), lastUpdated);
  }
}
