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

package org.siglus.siglusapi.service.task.report;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.referencedata.dto.OrderableDto;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.repository.FacilityCmmNativeRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.repository.SiglusFacilityRepository;
import org.siglus.siglusapi.repository.SiglusStockCardLineItemRepository;
import org.siglus.siglusapi.repository.SiglusStockCardRepository;
import org.siglus.siglusapi.repository.dto.StockCardLineItemDto;
import org.siglus.siglusapi.repository.dto.StockOnHandDto;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class CalculateWebCmmService {

  private final SiglusFacilityRepository siglusFacilityRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;
  private final FacilityCmmNativeRepository facilityCmmNativeRepository;
  private final SiglusStockCardRepository siglusStockCardRepository;
  private final SiglusStockCardLineItemRepository siglusStockCardLineItemRepository;
  private final SiglusOrderableService siglusOrderableService;

  private static final Long SKIP_ISSUE_QUANTITY = -1L;
  private static final Long STOCK_OUT_QUANTITY = 0L;
  private static final double INIT_CMM = -1d;
  private static final String MONTHLY_SCHEDULE_CODE = "M1";
  private static final int MAX_PERIOD_ISSUE_COUNT = 3;

  @Transactional
  public void calculateCmms(LocalDate periodLocalDateRequest) {
    log.info("calculate cmm start");
    long startTime = System.currentTimeMillis();
    LocalDate endDate = Objects.isNull(periodLocalDateRequest) ? LocalDate.now() : periodLocalDateRequest;

    List<Facility> webFacilities = siglusFacilityRepository.findAllWebFacility();
    Set<UUID> webFacilityIds = webFacilities.stream().map(Facility::getId).collect(Collectors.toSet());
    Map<UUID, String> facilityIdToCode = webFacilities.stream()
        .collect(Collectors.toMap(Facility::getId, Facility::getCode));
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode();
    List<ProcessingPeriod> upToNowAllPeriods = getUpToNowAllPeriods();

    ProcessingPeriod startPeriod = getStartProcessingPeriod(upToNowAllPeriods, endDate);
    ProcessingPeriod endPeriod = getEndProcessingPeriod(upToNowAllPeriods, endDate);

    webFacilityIds.forEach(facilityId -> {
      if (!webFacilityIds.contains(facilityId)) {
        return;
      }
      calculateAndSavaCmms(periodLocalDateRequest, facilityIdToCode, orderableIdToCode, upToNowAllPeriods, startPeriod,
          endPeriod, facilityId);
    });

    long endTime = System.currentTimeMillis();
    log.info("calculate cmm end, cost:{}s", (endTime - startTime) / 1000);
  }

  private void calculateAndSavaCmms(LocalDate periodLocalDateRequest, Map<UUID, String> facilityIdToCode,
      Map<UUID, String> orderableIdToCode, List<ProcessingPeriod> upToNowAllPeriods, ProcessingPeriod startPeriod,
      ProcessingPeriod endPeriod, UUID facilityId) {

    Map<UUID, List<StockOnHandDto>> orderableIdToStockCardDtos = getOrderableIdToStockCardDtos(startPeriod, endPeriod,
        facilityId);
    Map<UUID, List<StockCardLineItemDto>> orderableIdToStockCardLineItemDtos = getOrderableIdToStockCardLineItemDtos(
        startPeriod, endPeriod, facilityId);

    List<HfCmm> hfCmms = buildHfCmms(periodLocalDateRequest, Pair.of(facilityId, facilityIdToCode.get(facilityId)),
        orderableIdToCode, upToNowAllPeriods, endPeriod, orderableIdToStockCardDtos,
        orderableIdToStockCardLineItemDtos);
    if (!CollectionUtils.isEmpty(hfCmms)) {
      log.info("save hf cmms, size={}, facilityId:{}", hfCmms.size(), facilityId);
      facilityCmmNativeRepository.batchCreateHfCmms(hfCmms);
    }
  }

  private List<HfCmm> buildHfCmms(LocalDate periodLocalDateRequest, Pair<UUID, String> facilityIdCodePair,
      Map<UUID, String> orderableIdToCode, List<ProcessingPeriod> upToNowAllPeriods, ProcessingPeriod endPeriod,
      Map<UUID, List<StockOnHandDto>> orderableIdToStockCardDtos,
      Map<UUID, List<StockCardLineItemDto>> orderableIdToStockCardLineItemDtos) {

    List<HfCmm> hfCmms = Lists.newArrayList();

    orderableIdToStockCardDtos.forEach((orderableId, stockOnHandDtos) -> {
      LocalDate firstMovementPeriodStart = getFirstMovementPeriodStart(stockOnHandDtos, upToNowAllPeriods);
      if (Objects.isNull(firstMovementPeriodStart)) {
        log.warn("first movement period is null, do not calculate cmm, facilityId:{}, orderableId:{}",
            facilityIdCodePair.getFirst(), orderableId);
        return;
      }

      Set<LocalDate> hasStockOutPeriodStarDate = getHasStockOutPeriodStartDates(upToNowAllPeriods, stockOnHandDtos);
      Map<LocalDate, Long> periodStartDateToIssueQuantity = getPeriodStartDateToIssueQuantity(upToNowAllPeriods,
          orderableIdToStockCardLineItemDtos.get(orderableId));

      List<ProcessingPeriod> toBeCalculatedPeriods = getToBeCalculatedPeriods(periodLocalDateRequest,
          upToNowAllPeriods, firstMovementPeriodStart, endPeriod);

      toBeCalculatedPeriods.forEach(period -> {
        double cmm = calculateCmm(firstMovementPeriodStart, periodStartDateToIssueQuantity,
            hasStockOutPeriodStarDate, period);
        hfCmms.add(buildHfCmm(cmm, orderableIdToCode.get(orderableId), facilityIdCodePair.getSecond(), period));
      });
    });
    return hfCmms;
  }

  private Map<LocalDate, Long> getPeriodStartDateToIssueQuantity(List<ProcessingPeriod> upToNowAllPeriods,
      List<StockCardLineItemDto> lineItemDtos) {
    if (CollectionUtils.isEmpty(lineItemDtos)) {
      return Maps.newHashMap();
    }
    Map<LocalDate, Long> periodStartDateToIssueQuantity = Maps.newHashMap();
    lineItemDtos.forEach(lineItemDto -> {
      ProcessingPeriod period = getDateInPeriod(upToNowAllPeriods, lineItemDto.getOccurredDate());
      Long issueQuantity = periodStartDateToIssueQuantity.get(period.getStartDate());
      if (Objects.isNull(issueQuantity)) {
        periodStartDateToIssueQuantity.put(period.getStartDate(), lineItemDto.getIssueQuantity());
      } else {
        periodStartDateToIssueQuantity.put(period.getStartDate(), lineItemDto.getIssueQuantity() + issueQuantity);
      }
    });
    return periodStartDateToIssueQuantity;
  }

  private Map<UUID, List<StockCardLineItemDto>> getOrderableIdToStockCardLineItemDtos(ProcessingPeriod startPeriod,
      ProcessingPeriod endPeriod, UUID facilityId) {
    List<StockCardLineItemDto> stockCardLineItemDtos = siglusStockCardLineItemRepository.findStockCardLineItemDtos(
        facilityId, startPeriod.getStartDate(), endPeriod.getEndDate());
    return stockCardLineItemDtos.stream().collect(Collectors.groupingBy(StockCardLineItemDto::getOrderableId));
  }

  private Map<UUID, List<StockOnHandDto>> getOrderableIdToStockCardDtos(ProcessingPeriod startPeriod,
      ProcessingPeriod endPeriod, UUID facilityId) {
    List<StockOnHandDto> facilityStockOnHandDtos = siglusStockCardRepository.findStockCardDtos(facilityId,
        startPeriod.getStartDate(), endPeriod.getEndDate());
    return facilityStockOnHandDtos.stream().collect(Collectors.groupingBy(StockOnHandDto::getOrderableId));
  }

  private Set<LocalDate> getHasStockOutPeriodStartDates(List<ProcessingPeriod> upToNowAllPeriods,
      List<StockOnHandDto> stockOnHandDtos) {
    Map<LocalDate, List<StockOnHandDto>> periodStartDateToStockOnHandDtos = Maps.newHashMap();
    stockOnHandDtos.forEach(stockOnHandDto -> {
      ProcessingPeriod period = getDateInPeriod(upToNowAllPeriods, stockOnHandDto.getOccurredDate());
      if (Objects.isNull(periodStartDateToStockOnHandDtos.get(period.getStartDate()))) {
        periodStartDateToStockOnHandDtos.put(period.getStartDate(), Lists.newArrayList(stockOnHandDto));
        return;
      }
      periodStartDateToStockOnHandDtos.get(period.getStartDate()).add(stockOnHandDto);
    });

    Long soh = 0L;
    Set<LocalDate> hasStockOutPeriodStarDate = Sets.newHashSet();
    for (ProcessingPeriod period : upToNowAllPeriods) {
      List<StockOnHandDto> curPeriodStockOnHandDtos = periodStartDateToStockOnHandDtos.get(period.getStartDate());
      if (currentPeriodNoStockOnHandAndLastIsZero(soh, curPeriodStockOnHandDtos)
          || isStockOutExisted(curPeriodStockOnHandDtos)) {
        hasStockOutPeriodStarDate.add(period.getStartDate());
      }
      soh = CollectionUtils.isEmpty(curPeriodStockOnHandDtos) ? soh : getLastStockOnHand(curPeriodStockOnHandDtos);
    }
    return hasStockOutPeriodStarDate;
  }

  private boolean currentPeriodNoStockOnHandAndLastIsZero(Long soh, List<StockOnHandDto> curPeriodStockOnHandDtos) {
    return CollectionUtils.isEmpty(curPeriodStockOnHandDtos) && soh.equals(STOCK_OUT_QUANTITY);
  }

  private boolean isStockOutExisted(List<StockOnHandDto> stockOnHandDtos) {
    if (CollectionUtils.isEmpty(stockOnHandDtos)) {
      return false;
    }
    return stockOnHandDtos.stream()
        .anyMatch(stockOnHandDto -> stockOnHandDto.getStockOnHand().equals(STOCK_OUT_QUANTITY));
  }

  private Long getLastStockOnHand(List<StockOnHandDto> stockOnHandDtos) {
    return stockOnHandDtos.get(stockOnHandDtos.size() - 1).getStockOnHand();
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

  private List<ProcessingPeriod> getToBeCalculatedPeriods(LocalDate periodLocalDateRequest,
      List<ProcessingPeriod> allPeriods, LocalDate firstMovementPeriodStart, ProcessingPeriod endPeriod) {
    if (Objects.isNull(periodLocalDateRequest)) {
      return allPeriods.stream()
          .filter(period -> !period.getStartDate().isBefore(firstMovementPeriodStart)
              && !period.getStartDate().isAfter(endPeriod.getStartDate()))
          .collect(Collectors.toList());
    }
    ProcessingPeriod period = getDateInPeriod(allPeriods, periodLocalDateRequest);
    if (Objects.isNull(period)) {
      log.warn("no period match, request date:{}", periodLocalDateRequest);
      return Lists.newArrayList();
    }
    if (firstMovementPeriodStart.isAfter(period.getStartDate())) {
      log.warn(
          "request period start date is before first movement period start date, request:{}, first movement:{}",
          period.getStartDate(), firstMovementPeriodStart);
      return Lists.newArrayList();
    }
    return Lists.newArrayList(period);
  }

  private ProcessingPeriod getStartProcessingPeriod(List<ProcessingPeriod> processingPeriods,
      LocalDate endDate) {
    ProcessingPeriod relativelyOneYearAgoPeriod = processingPeriods.stream()
        .filter(period -> isDateInPeriod(period, endDate.minusMonths(11))).findFirst().orElse(null);
    if (Objects.isNull(relativelyOneYearAgoPeriod)) {
      return processingPeriods.get(0);
    }
    return relativelyOneYearAgoPeriod;
  }

  private ProcessingPeriod getEndProcessingPeriod(List<ProcessingPeriod> processingPeriods, LocalDate endDate) {
    ProcessingPeriod oneYearAgoPeriod = processingPeriods.stream()
        .filter(period -> isDateInPeriod(period, endDate)).findFirst().orElse(null);
    if (Objects.isNull(oneYearAgoPeriod)) {
      return processingPeriods.get(processingPeriods.size() - 1);
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
        .lastUpdated(OffsetDateTime.now())
        .build();
    hfCmm.setId(UUID.randomUUID());
    return hfCmm;
  }

  private List<ProcessingPeriod> getUpToNowAllPeriods() {
    return processingPeriodRepository.findAll().stream()
        .filter(e -> e.getProcessingSchedule().getCode().toString().equals(MONTHLY_SCHEDULE_CODE))
        .filter(e -> !e.getStartDate().isAfter(LocalDate.now()))
        .sorted(Comparator.comparing(ProcessingPeriod::getStartDate))
        .collect(Collectors.toList());
  }

  private Map<UUID, String> getOrderableIdToCode() {
    return siglusOrderableService.getAllProducts().stream()
        .collect(Collectors.toMap(OrderableDto::getId, OrderableDto::getProductCode, (a, b) -> a));
  }

  private LocalDate getFirstMovementPeriodStart(List<StockOnHandDto> stockOnHandDtos,
      List<ProcessingPeriod> processingPeriods) {
    // first movement date is by product(any movement type)
    stockOnHandDtos.sort(Comparator.comparing(StockOnHandDto::getOccurredDate));
    LocalDate firstMovementDate = stockOnHandDtos.get(0).getOccurredDate();
    return getPeriodStartDate(firstMovementDate, processingPeriods);
  }

  private LocalDate getPeriodStartDate(LocalDate localDate, List<ProcessingPeriod> processingPeriods) {
    ProcessingPeriod dateInPeriod = getDateInPeriod(processingPeriods, localDate);
    if (Objects.isNull(dateInPeriod)) {
      return null;
    }
    return dateInPeriod.getStartDate();
  }

  private ProcessingPeriod getDateInPeriod(List<ProcessingPeriod> processingPeriods, LocalDate localDate) {
    return processingPeriods.stream().filter(period -> isDateInPeriod(period, localDate)).findFirst().orElse(null);
  }

  private boolean isDateInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return !localDate.isBefore(period.getStartDate()) && !localDate.isAfter(period.getEndDate());
  }
}
