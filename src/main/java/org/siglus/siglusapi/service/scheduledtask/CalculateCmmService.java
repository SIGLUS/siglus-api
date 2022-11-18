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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.dto.HfCmmCountDto;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.StockCardLineItemDto;
import org.siglus.siglusapi.repository.dto.StockOnHandDto;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusProcessingPeriodService;
import org.siglus.siglusapi.util.PeriodUtil;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CalculateCmmService {

  private final SiglusFacilityRepository siglusFacilityRepository;
  private final FacilityCmmsRepository facilityCmmsRepository;
  private final SiglusStockCardRepository siglusStockCardRepository;
  private final SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;
  private final SiglusOrderableService siglusOrderableService;
  private final SiglusProcessingPeriodService periodService;

  private static final Long SKIP_ISSUE_QUANTITY = -1L;
  private static final Long STOCK_OUT_QUANTITY = 0L;
  private static final double INIT_CMM = -1d;
  private static final int MAX_PERIOD_ISSUE_COUNT = 3;

  @Transactional
  public void calculateAllWebCmm(LocalDate requestDate) {
    List<Facility> webFacilities = siglusFacilityRepository.findAllWebFacility();
    Set<UUID> webFacilityIds = webFacilities.stream().map(Facility::getId).collect(Collectors.toSet());
    Map<UUID, String> facilityIdToCode = webFacilities.stream()
        .collect(Collectors.toMap(Facility::getId, Facility::getCode));
    Map<UUID, String> orderableIdToCode = siglusOrderableService.getAllProductIdToCode();
    List<ProcessingPeriod> periods = getOneYearPeriods(periodService.getUpToNowMonthlyPeriods(), requestDate);
    Map<String, List<HfCmmCountDto>> facilityCodeToHfCmmCountDtos = facilityCmmsRepository.findAllFacilityCmmCountDtos(
            periods.stream().map(ProcessingPeriod::getStartDate).collect(Collectors.toList()))
        .stream().collect(Collectors.groupingBy(HfCmmCountDto::getFacilityCode));

    webFacilityIds.forEach(facilityId -> {
      if (!webFacilityIds.contains(facilityId)) {
        return;
      }
      calculateAndSavaCmms(requestDate, Pair.of(facilityId, facilityIdToCode.get(facilityId)), orderableIdToCode,
          periods, facilityCodeToHfCmmCountDtos.get(facilityIdToCode.get(facilityId)));
    });
  }

  @Transactional
  public void calculateOneFacilityCmm(LocalDate requestDate, UUID facilityId) {
    Facility facility = siglusFacilityRepository.findOne(facilityId);
    Map<UUID, String> orderableIdToCode = siglusOrderableService.getAllProductIdToCode();
    List<ProcessingPeriod> periods = getOneYearPeriods(periodService.getUpToNowMonthlyPeriods(), requestDate);
    List<HfCmmCountDto> hfCmmCountDtos = facilityCmmsRepository.findOneFacilityCmmCountDtos(
        periods.stream().map(ProcessingPeriod::getStartDate).collect(Collectors.toList()), facility.getCode());

    calculateAndSavaCmms(requestDate, Pair.of(facilityId, facility.getCode()), orderableIdToCode, periods,
        hfCmmCountDtos);
  }

  private List<ProcessingPeriod> getOneYearPeriods(List<ProcessingPeriod> upToNowAllPeriods,
      LocalDate requestDate) {
    LocalDate endDate = Objects.isNull(requestDate) ? LocalDate.now() : requestDate;
    ProcessingPeriod startPeriod = getStartProcessingPeriod(upToNowAllPeriods, endDate);
    ProcessingPeriod endPeriod = getEndProcessingPeriod(upToNowAllPeriods, endDate);
    return upToNowAllPeriods.stream()
        .filter(period -> PeriodUtil.isPeriodBetween(period, startPeriod.getStartDate(), endPeriod.getEndDate()))
        .sorted(Comparator.comparing(ProcessingPeriod::getStartDate))
        .collect(Collectors.toList());
  }

  private void calculateAndSavaCmms(LocalDate requestDate, Pair<UUID, String> facilityIdToCode,
      Map<UUID, String> orderableIdToCode, List<ProcessingPeriod> periods, List<HfCmmCountDto> hfCmmCountDtos) {

    UUID facilityId = facilityIdToCode.getFirst();
    LocalDate startDate = periods.get(0).getStartDate();
    LocalDate endDate = periods.get(periods.size() - 1).getEndDate();

    Map<UUID, List<StockOnHandDto>> orderableIdToSohDtos = getOrderableIdToSohDtos(startDate, endDate, facilityId);
    Map<UUID, List<StockCardLineItemDto>> orderableIdToStockCardLineItemDtos = getOrderableIdToStockCardLineItemDtos(
        startDate, endDate, facilityId);
    Map<LocalDate, Integer> periodStartDateToCount = getPeriodStartDateToCount(hfCmmCountDtos);

    List<HfCmm> hfCmms = buildHfCmms(requestDate, facilityIdToCode, orderableIdToCode, periods, orderableIdToSohDtos,
        orderableIdToStockCardLineItemDtos, periodStartDateToCount);
    if (CollectionUtils.isNotEmpty(hfCmms)) {
      log.info("save hf cmms, facilityId:{}, size:{}", facilityId, hfCmms.size());
      facilityCmmsRepository.save(hfCmms);
    }
  }

  private Map<LocalDate, Integer> getPeriodStartDateToCount(List<HfCmmCountDto> hfCmmCountDtos) {
    return hfCmmCountDtos.stream().collect(Collectors.toMap(HfCmmCountDto::getPeriodBegin, HfCmmCountDto::getCount));
  }

  private List<HfCmm> buildHfCmms(LocalDate requestDate, Pair<UUID, String> facilityIdCodePair,
      Map<UUID, String> orderableIdToCode, List<ProcessingPeriod> periods,
      Map<UUID, List<StockOnHandDto>> orderableIdToSohDtos,
      Map<UUID, List<StockCardLineItemDto>> orderableIdToStockCardLineItemDtos,
      Map<LocalDate, Integer> periodStartDateToCount) {

    List<HfCmm> hfCmms = Lists.newArrayList();

    orderableIdToSohDtos.forEach((orderableId, sohDtos) -> {
      LocalDate firstMovementPeriodStart = getFirstMovementPeriodStart(sohDtos, periods);
      if (Objects.isNull(firstMovementPeriodStart)) {
        log.warn("first movement period is null, do not calculate cmm, facilityId:{}, orderableId:{}",
            facilityIdCodePair.getFirst(), orderableId);
        return;
      }

      Set<LocalDate> hasStockOutPeriodStarDate = getHasStockOutPeriodStartDates(periods, sohDtos);
      Map<LocalDate, Long> periodStartDateToIssueQuantity = getPeriodStartDateToIssueQuantity(periods,
          orderableIdToStockCardLineItemDtos.get(orderableId));

      List<ProcessingPeriod> toBeCalculatedPeriods = getToBeCalculatedPeriods(requestDate, periods,
          firstMovementPeriodStart);

      toBeCalculatedPeriods.forEach(period -> {
        if (hasCalculatedCmms(periodStartDateToCount, period)) {
          return;
        }

        double cmm = calculateCmm(firstMovementPeriodStart, periodStartDateToIssueQuantity,
            hasStockOutPeriodStarDate, period);
        hfCmms.add(buildHfCmm(cmm, orderableIdToCode.get(orderableId), facilityIdCodePair.getSecond(), period));
      });
    });
    return hfCmms;
  }

  private boolean hasCalculatedCmms(Map<LocalDate, Integer> periodStartDateToCount, ProcessingPeriod period) {
    return periodStartDateToCount.getOrDefault(period.getStartDate(), 0) > 0;
  }

  private Map<LocalDate, Long> getPeriodStartDateToIssueQuantity(List<ProcessingPeriod> periods,
      List<StockCardLineItemDto> lineItemDtos) {
    if (CollectionUtils.isEmpty(lineItemDtos)) {
      return Maps.newHashMap();
    }
    Map<LocalDate, Long> periodStartDateToIssueQuantity = Maps.newHashMap();
    lineItemDtos.forEach(lineItemDto -> {
      ProcessingPeriod period = PeriodUtil.getPeriodDateInDefaultNull(periods, lineItemDto.getOccurredDate());
      Long issueQuantity = periodStartDateToIssueQuantity.getOrDefault(period.getStartDate(), 0L);
      periodStartDateToIssueQuantity.put(period.getStartDate(), lineItemDto.getIssueQuantity() + issueQuantity);
    });
    return periodStartDateToIssueQuantity;
  }

  private Map<UUID, List<StockCardLineItemDto>> getOrderableIdToStockCardLineItemDtos(LocalDate startDate,
      LocalDate endDate, UUID facilityId) {
    List<StockCardLineItemDto> stockCardLineItemDtos = siglusStockCardLineItemRepository.findStockCardLineItemDtos(
        facilityId, startDate, endDate);
    return stockCardLineItemDtos.stream().collect(Collectors.groupingBy(StockCardLineItemDto::getOrderableId));
  }

  private Map<UUID, List<StockOnHandDto>> getOrderableIdToSohDtos(LocalDate startDate,
      LocalDate endDate, UUID facilityId) {
    List<StockOnHandDto> facilitySohDtos = siglusStockCardRepository.findStockCardDtos(facilityId, startDate,
        endDate);
    return facilitySohDtos.stream().collect(Collectors.groupingBy(StockOnHandDto::getOrderableId));
  }

  private Set<LocalDate> getHasStockOutPeriodStartDates(List<ProcessingPeriod> periods, List<StockOnHandDto> sohDtos) {
    Map<LocalDate, List<StockOnHandDto>> periodStartDateToSohDtos = Maps.newHashMap();
    sohDtos.forEach(sohDto -> {
      ProcessingPeriod period = PeriodUtil.getPeriodDateInDefaultNull(periods, sohDto.getOccurredDate());
      if (Objects.isNull(periodStartDateToSohDtos.get(period.getStartDate()))) {
        periodStartDateToSohDtos.put(period.getStartDate(), Lists.newArrayList(sohDto));
        return;
      }
      periodStartDateToSohDtos.get(period.getStartDate()).add(sohDto);
    });

    Long soh = 0L;
    Set<LocalDate> hasStockOutPeriodStarDate = Sets.newHashSet();
    for (ProcessingPeriod period : periods) {
      List<StockOnHandDto> curPeriodSohDtos = periodStartDateToSohDtos.get(period.getStartDate());
      if (currentPeriodNoSohAndLastIsZero(soh, curPeriodSohDtos)
          || isStockOutExisted(curPeriodSohDtos)) {
        hasStockOutPeriodStarDate.add(period.getStartDate());
      }
      soh = CollectionUtils.isEmpty(curPeriodSohDtos) ? soh : getLastStockOnHand(curPeriodSohDtos);
    }
    return hasStockOutPeriodStarDate;
  }

  private boolean currentPeriodNoSohAndLastIsZero(Long soh, List<StockOnHandDto> curPeriodSohDtos) {
    return CollectionUtils.isEmpty(curPeriodSohDtos) && soh.equals(STOCK_OUT_QUANTITY);
  }

  private boolean isStockOutExisted(List<StockOnHandDto> sohDtos) {
    if (CollectionUtils.isEmpty(sohDtos)) {
      return false;
    }
    return sohDtos.stream().anyMatch(stockOnHandDto -> stockOnHandDto.getStockOnHand().equals(STOCK_OUT_QUANTITY));
  }

  private Long getLastStockOnHand(List<StockOnHandDto> sohDtos) {
    return sohDtos.get(sohDtos.size() - 1).getStockOnHand();
  }

  private Double calculateCmm(LocalDate firstMovementPeriodStart, Map<LocalDate, Long> periodStartDateToIssueQuantity,
      Set<LocalDate> hasStockOutPeriodStarDate, ProcessingPeriod period) {
    if (period.getStartDate().isEqual(firstMovementPeriodStart)) {
      return INIT_CMM;
    }
    LocalDate pointPeriodStartDate = period.getStartDate().minusMonths(1);
    int usedPeriodIssueCount = 0;
    long totalIssueQuantity = 0;
    while (!pointPeriodStartDate.isBefore(firstMovementPeriodStart) && usedPeriodIssueCount < MAX_PERIOD_ISSUE_COUNT) {
      Long issueQuantity = getIssueQuantity(periodStartDateToIssueQuantity, pointPeriodStartDate,
          hasStockOutPeriodStarDate);
      if (!SKIP_ISSUE_QUANTITY.equals(issueQuantity)) {
        totalIssueQuantity += issueQuantity;
        usedPeriodIssueCount++;
      }
      pointPeriodStartDate = pointPeriodStartDate.minusMonths(1);
    }
    return usedPeriodIssueCount == 0 ? 0d : totalIssueQuantity * 1d / usedPeriodIssueCount;
  }

  private Long getIssueQuantity(Map<LocalDate, Long> periodStartDateToIssueQuantity, LocalDate periodStartDate,
      Set<LocalDate> hasStockOutPeriodStarDate) {
    if (hasStockOutPeriodStarDate.contains(periodStartDate)) {
      return SKIP_ISSUE_QUANTITY;
    }
    Long issueQuantity = periodStartDateToIssueQuantity.get(periodStartDate);
    if (Objects.nonNull(issueQuantity)) {
      return issueQuantity;
    }
    return 0L;
  }

  private List<ProcessingPeriod> getToBeCalculatedPeriods(LocalDate requestDate, List<ProcessingPeriod> periods,
      LocalDate firstMovementPeriodStart) {
    if (Objects.isNull(requestDate)) {
      return periods.stream()
          .filter(period -> !period.getStartDate().isBefore(firstMovementPeriodStart))
          .collect(Collectors.toList());
    }
    ProcessingPeriod period = PeriodUtil.getPeriodDateInDefaultNull(periods, requestDate);
    if (Objects.isNull(period)) {
      log.warn("no period match, request date:{}", requestDate);
      return Lists.newArrayList();
    }
    if (firstMovementPeriodStart.isAfter(period.getStartDate())) {
      log.warn(
          "request period start date is before first movement period start date, request:{}, first movement:{}",
          requestDate, firstMovementPeriodStart);
      return Lists.newArrayList();
    }
    return Lists.newArrayList(period);
  }

  private ProcessingPeriod getStartProcessingPeriod(List<ProcessingPeriod> periods, LocalDate endDate) {
    ProcessingPeriod relativelyOneYearAgoPeriod = periods.stream()
        .filter(period -> PeriodUtil.isDateInPeriod(period, endDate.minusMonths(11))).findFirst().orElse(null);
    if (Objects.isNull(relativelyOneYearAgoPeriod)) {
      return periods.get(0);
    }
    return relativelyOneYearAgoPeriod;
  }

  private ProcessingPeriod getEndProcessingPeriod(List<ProcessingPeriod> periods, LocalDate endDate) {
    ProcessingPeriod oneYearAgoPeriod = periods.stream()
        .filter(period -> PeriodUtil.isDateInPeriod(period, endDate)).findFirst().orElse(null);
    if (Objects.isNull(oneYearAgoPeriod)) {
      return periods.get(periods.size() - 1);
    }
    return oneYearAgoPeriod;
  }

  private HfCmm buildHfCmm(double cmm, String orderableCode, String facilityCode, ProcessingPeriod period) {
    HfCmm hfCmm = HfCmm.builder()
        .cmm(cmm)
        .periodBegin(period.getStartDate())
        .periodEnd(period.getEndDate())
        .productCode(orderableCode)
        .facilityCode(facilityCode)
        .lastUpdated(ZonedDateTime.now())
        .build();
    hfCmm.setId(UUID.randomUUID());
    return hfCmm;
  }

  private LocalDate getFirstMovementPeriodStart(List<StockOnHandDto> sohDtos, List<ProcessingPeriod> periods) {
    // first movement date is by product(any movement type)
    sohDtos.sort(Comparator.comparing(StockOnHandDto::getOccurredDate));
    LocalDate firstMovementDate = sohDtos.get(0).getOccurredDate();
    return getPeriodStartDate(firstMovementDate, periods);
  }

  private LocalDate getPeriodStartDate(LocalDate localDate, List<ProcessingPeriod> periods) {
    ProcessingPeriod dateInPeriod = PeriodUtil.getPeriodDateInDefaultNull(periods, localDate);
    if (Objects.isNull(dateInPeriod)) {
      return null;
    }
    return dateInPeriod.getStartDate();
  }
}
