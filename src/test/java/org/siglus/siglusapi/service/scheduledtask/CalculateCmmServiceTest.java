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

package org.siglus.siglusapi.service.scheduledtask;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.domain.ProcessingSchedule;
import org.siglus.siglusapi.constant.PeriodConstants;
import org.siglus.siglusapi.dto.HfCmmCountDto;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.StockCardLineItemDto;
import org.siglus.siglusapi.repository.dto.StockOnHandDto;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;


@RunWith(MockitoJUnitRunner.class)
public class CalculateCmmServiceTest {

  @InjectMocks
  private CalculateCmmService calculateCmmService;

  @Mock
  private SiglusFacilityRepository siglusFacilityRepository;
  @Mock
  private FacilityCmmsRepository facilityCmmsRepository;
  @Mock
  private SiglusStockCardRepository siglusStockCardRepository;
  @Mock
  private SiglusOrderableService siglusOrderableService;
  @Mock
  private SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;
  @Mock
  private SiglusProcessingPeriodService periodService;

  private final UUID orderableId = UUID.randomUUID();
  private final UUID facilityId = UUID.randomUUID();
  private final String orderableCode = "orderable code";
  private final String facilityCode = "facility Code";
  private final LocalDate now = LocalDate.of(2023, 3, 21);
  private final LocalDate oneYearAgo = LocalDate.of(2022, 12, 21);
  private final LocalDate periodStartDate = LocalDate.of(2022, 11, 21);

  @Before
  public void setup() {
    when(periodService.getUpToNowMonthlyPeriods()).thenReturn(buildMockPeriods());
    when(siglusOrderableService.getAllProductIdToCode()).thenReturn(buildMockOrderableIdToCode());
    when(siglusStockCardRepository.findStockCardDtos(any(), any(), any())).thenReturn(buildMockStockOhHandDtos());
    when(siglusStockCardLineItemRepository.findStockCardLineItemDtos(any(), any(), any())).thenReturn(
        buildMockStockCardLineItemDtos());
    when(facilityCmmsRepository.findAllFacilityCmmCountDtos(anyList())).thenReturn(buildMockHfCmmCountDtos());
  }

  @Test
  public void shouldSuccessWhenCalculateAllPeriod() {
    // given
    when(siglusFacilityRepository.findAllWebFacility()).thenReturn(buildMockFacilities());

    // when
    calculateCmmService.calculateAllWebCmm(now);

    // then
    verify(facilityCmmsRepository).save(anyList());
  }

  @Test
  public void shouldSuccessWhenCalculateSpecifiedPeriod() {
    // given
    when(siglusFacilityRepository.findAllWebFacility()).thenReturn(buildMockFacilities());

    // when
    calculateCmmService.calculateAllWebCmm(now);

    // then
    verify(facilityCmmsRepository).save(anyList());
  }

  @Test
  public void shouldNotSaveWhenSpecifiedPeriodBeforeFirstMovement() {
    // given
    when(siglusFacilityRepository.findAllWebFacility()).thenReturn(buildMockFacilities());

    // when
    calculateCmmService.calculateAllWebCmm(LocalDate.of(oneYearAgo.getYear(), 8, 21));

    // then
    verify(facilityCmmsRepository, times(0)).save(anyList());
  }

  @Test
  public void shouldSuccessWhenCalculateOneFacilityCmms() {
    // given
    when(siglusFacilityRepository.findOne(facilityId)).thenReturn(buildMockFacility());
    when(facilityCmmsRepository.findOneFacilityCmmCountDtos(anyList(), anyString())).thenReturn(
        buildMockHfCmmCountDtos());

    // when
    calculateCmmService.calculateOneFacilityCmm(now, facilityId);

    // then
    verify(facilityCmmsRepository).save(anyList());
  }

  @Test
  public void shouldNotSaveWhenCalculateOneFacilityCmmsGivenExistedHfCmm() {
    // given
    when(siglusFacilityRepository.findOne(facilityId)).thenReturn(buildMockFacility());
    when(facilityCmmsRepository.findOneFacilityCmmCountDtos(anyList(), anyString())).thenReturn(
        buildMockHfCmmCountDtosWithBiggerThan0());

    // when
    calculateCmmService.calculateOneFacilityCmm(periodStartDate, facilityId);

    // then
    verify(facilityCmmsRepository, times(0)).save(anyList());
  }

  private Map<UUID, String> buildMockOrderableIdToCode() {
    Map<UUID, String> orderableIdToCode = Maps.newHashMap();
    orderableIdToCode.put(orderableId, orderableCode);
    return orderableIdToCode;
  }

  private List<StockCardLineItemDto> buildMockStockCardLineItemDtos() {
    StockCardLineItemDto lineItem1 = StockCardLineItemDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(oneYearAgo.getYear(), oneYearAgo.getMonth(), 21))
        .issueQuantity(30L)
        .build();
    StockCardLineItemDto lineItem2 = StockCardLineItemDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(oneYearAgo.getYear(), oneYearAgo.getMonth(), 22))
        .issueQuantity(30L)
        .build();

    LocalDate plusMonth1 = oneYearAgo.plusMonths(1);
    StockCardLineItemDto lineItem3 = StockCardLineItemDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(plusMonth1.getYear(), plusMonth1.getMonth(), 21))
        .issueQuantity(30L)
        .build();

    LocalDate plusMonth2 = oneYearAgo.plusMonths(2);
    StockCardLineItemDto lineItem4 = StockCardLineItemDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(plusMonth2.getYear(), plusMonth2.getMonth(), 20))
        .issueQuantity(30L)
        .build();
    return Lists.newArrayList(lineItem1, lineItem2, lineItem3, lineItem4);
  }

  private List<StockOnHandDto> buildMockStockOhHandDtos() {
    StockOnHandDto soh1 = StockOnHandDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(oneYearAgo.getYear(), oneYearAgo.getMonth(), 21))
        .stockOnHand(110L)
        .build();
    StockOnHandDto soh2 = StockOnHandDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(oneYearAgo.getYear(), oneYearAgo.getMonth(), 22))
        .stockOnHand(110L)
        .build();

    LocalDate plusMonth1 = oneYearAgo.plusMonths(1);
    StockOnHandDto soh3 = StockOnHandDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(plusMonth1.getYear(), plusMonth1.getMonth(), 21))
        .stockOnHand(110L)
        .build();

    LocalDate plusMonth2 = oneYearAgo.plusMonths(2);
    StockOnHandDto soh4 = StockOnHandDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(plusMonth2.getYear(), plusMonth2.getMonth(), 19))
        .stockOnHand(0L)
        .build();
    StockOnHandDto soh5 = StockOnHandDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(plusMonth2.getYear(), plusMonth2.getMonth(), 20))
        .stockOnHand(100L)
        .build();

    LocalDate plusMonth3 = oneYearAgo.plusMonths(3);
    StockOnHandDto soh6 = StockOnHandDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(plusMonth3.getYear(), plusMonth3.getMonth(), 21))
        .stockOnHand(110L)
        .build();
    StockOnHandDto soh7 = StockOnHandDto.builder()
        .orderableId(orderableId)
        .occurredDate(LocalDate.of(plusMonth3.getYear(), plusMonth3.getMonth(), 22))
        .stockOnHand(110L)
        .build();

    return Lists.newArrayList(soh1, soh2, soh3, soh4, soh5, soh6, soh7);
  }

  private List<ProcessingPeriod> buildMockPeriods() {
    ProcessingSchedule m1Schedule = new ProcessingSchedule();
    m1Schedule.setCode(PeriodConstants.MONTH_SCHEDULE_CODE);
    List<ProcessingPeriod> periods = Lists.newArrayList();
    for (int i = 1; i <= 12; i++) {
      LocalDate startDate = LocalDate.of(oneYearAgo.getYear(), i, 21);
      ProcessingPeriod m1ProcessingPeriod = ProcessingPeriod.newPeriod(i + "-2021", m1Schedule,
          startDate, startDate.plusMonths(1).minusDays(1));
      periods.add(m1ProcessingPeriod);
    }
    for (int i = 1; i <= 12; i++) {
      LocalDate startDate = LocalDate.of(now.getYear(), i, 21);
      ProcessingPeriod m1ProcessingPeriod = ProcessingPeriod.newPeriod(i + "-2022", m1Schedule,
          startDate, startDate.plusMonths(1).minusDays(1));
      periods.add(m1ProcessingPeriod);
    }
    return periods;
  }

  private List<Facility> buildMockFacilities() {
    return Lists.newArrayList(buildMockFacility());
  }

  private Facility buildMockFacility() {
    Facility facility = new Facility();
    facility.setId(facilityId);
    facility.setCode(facilityCode);
    facility.setActive(Boolean.TRUE);
    facility.setEnabled(Boolean.TRUE);
    return facility;
  }

  private List<HfCmmCountDto> buildMockHfCmmCountDtos() {
    HfCmmCountDto dto = new HfCmmCountDto(facilityCode, periodStartDate, 0);
    return Lists.newArrayList(dto);
  }

  private List<HfCmmCountDto> buildMockHfCmmCountDtosWithBiggerThan0() {
    HfCmmCountDto dto = new HfCmmCountDto(facilityCode, periodStartDate, 1);
    return Lists.newArrayList(dto);
  }
}
