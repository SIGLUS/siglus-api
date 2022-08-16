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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Orderable;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.repository.OrderableRepository;
import org.siglus.siglusapi.repository.ProcessingPeriodRepository;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class CalculateWebCmmService {

  private final FacilityExtensionRepository facilityExtensionRepository;
  private final StockCardRepository stockCardRepository;
  private final ProcessingPeriodRepository processingPeriodRepository;
  private final FacilityCmmsRepository facilityCmmsRepository;
  private final OrderableRepository orderableRepository;
  private final SiglusStockCardService siglusStockCardService;

  public void calculateAllPeriod() {
    calculate(Boolean.TRUE);
  }

  public void calculateCurrentPeriod() {
    calculate(Boolean.FALSE);
  }

  private void calculate(boolean isFirstTime) {
    log.info("calculate start....");
    long startTime = System.currentTimeMillis();

    List<FacilityExtension> webFacilityExtensions = getWebFacilityExtensions();
    Set<UUID> webFacilityIds = webFacilityExtensions.stream().map(FacilityExtension::getFacilityId)
        .collect(Collectors.toSet());
    Map<UUID, String> facilityIdToCode = webFacilityExtensions.stream()
        .collect(Collectors.toMap(FacilityExtension::getFacilityId, FacilityExtension::getFacilityCode));

    List<StockCard> allStockCards = stockCardRepository.findAll();
    Map<UUID, List<StockCard>> facilityIdToStockCars = allStockCards.stream()
        .collect(Collectors.groupingBy(StockCard::getFacilityId));

    Map<UUID, Code> orderableIdToCode = getOrderableIdToCode(
        allStockCards.stream().map(StockCard::getOrderableId).collect(Collectors.toSet()));
    List<ProcessingPeriod> processingPeriods = getPeriods();

    facilityIdToStockCars.forEach((facilityId, stockCards) -> {
      if (!webFacilityIds.contains(facilityId)) {
        return;
      }
      calculateAndSaveCmms(orderableIdToCode, facilityIdToCode, processingPeriods, facilityId, stockCards, isFirstTime);
    });

    long endTime = System.currentTimeMillis();
    log.info("....calculate end, cost={}", endTime - startTime);
  }

  private void calculateAndSaveCmms(Map<UUID, Code> orderableIdToCode, Map<UUID, String> facilityIdToCode,
      List<ProcessingPeriod> processingPeriods, UUID facilityId, List<StockCard> stockCards, boolean isFirstTime) {
    Set<UUID> alreadyCalculatedOrderableIds = Sets.newHashSet();
    for (StockCard stockCard : stockCards) {
      if (alreadyCalculatedOrderableIds.contains(stockCard.getOrderableId())) {
        log.info(
            "duplicate facility and orderable, facilityCode:{}, orderableCode:{}",
            facilityIdToCode.get(facilityId), orderableIdToCode.get(stockCard.getOrderableId()));
        continue;
      }
      alreadyCalculatedOrderableIds.add(stockCard.getOrderableId());

      try {
        List<StockMovementResDto> productMovements = siglusStockCardService.getProductMovements(
            stockCard.getFacilityId(),
            stockCard.getOrderableId(), null, LocalDate.now());
        if (CollectionUtils.isEmpty(productMovements)) {
          log.warn("no product movement, do not calculate cmm, stockCard:{}", stockCard);
          break;
        }

        LocalDate firstMovementPeriodStart = queryFirstMovementPeriodStart(productMovements, processingPeriods);
        if (Objects.isNull(firstMovementPeriodStart)) {
          log.warn("product movement do not match any period, do not calculate cmm, stockCardId={}", stockCard.getId());
          break;
        }

        Map<LocalDate, Long> periodStartDateToIssue = calculatePeriodStartDateToIssue(processingPeriods,
            productMovements, firstMovementPeriodStart);
        List<ProcessingPeriod> toBeCalculatedPeriods = getToBeCalculatedPeriods(isFirstTime, processingPeriods,
            firstMovementPeriodStart);

        List<HfCmm> hfCmms = buildHfCmms(orderableIdToCode, facilityIdToCode,
            toBeCalculatedPeriods, facilityId, stockCard, firstMovementPeriodStart, periodStartDateToIssue);
        log.info("save hf cmms, size={}", hfCmms.size());
        facilityCmmsRepository.save(hfCmms);
      } catch (Exception e) {
        log.error("calculate and save failed, ", e);
      }
    }
  }

  private List<ProcessingPeriod> getToBeCalculatedPeriods(boolean isFirstTime, List<ProcessingPeriod> allPeriods,
      LocalDate firstMovementPeriodStart) {
    if (isFirstTime) {
      return allPeriods.stream().filter(period -> !period.getStartDate().isBefore(firstMovementPeriodStart)).collect(
          Collectors.toList());
    } else {
      return Lists.newArrayList(getTodayProcessingPeriod(allPeriods));
    }
  }

  private ProcessingPeriod getTodayProcessingPeriod(List<ProcessingPeriod> processingPeriods) {
    return processingPeriods.stream()
        .filter(period -> isDateInPeriod(period, LocalDate.now())).findFirst().orElse(null);
  }

  private List<HfCmm> buildHfCmms(Map<UUID, Code> orderableIdToCode, Map<UUID, String> facilityIdToCode,
      List<ProcessingPeriod> processingPeriods, UUID facilityId, StockCard stockCard,
      LocalDate firstMovementPeriodStart, Map<LocalDate, Long> periodStartDateToIssue) {
    List<HfCmm> hfCmms = Lists.newArrayList();
    for (ProcessingPeriod period : processingPeriods) {
      if (period.getStartDate().isBefore(firstMovementPeriodStart)) {
        continue;
      }

      if (period.getStartDate().isEqual(firstMovementPeriodStart)) {
        hfCmms.add(buildHfCmm(-1d, orderableIdToCode.get(stockCard.getOrderableId()),
            facilityIdToCode.get(facilityId), period));
        continue;
      }

      // calculate cmm
      LocalDate pointPeriodStartDate = period.getStartDate().minusMonths(1);
      int usedPeriodIssue = 0;
      long totalIssue = 0;
      while (!pointPeriodStartDate.isBefore(firstMovementPeriodStart) && usedPeriodIssue < 3) {
        Long issue = periodStartDateToIssue.get(pointPeriodStartDate);
        if (issue != null && issue != -1) {
          totalIssue += issue;
          usedPeriodIssue++;
        }
        pointPeriodStartDate = pointPeriodStartDate.minusMonths(1);
      }
      hfCmms.add(buildHfCmm(totalIssue * 1d / usedPeriodIssue, orderableIdToCode.get(stockCard.getOrderableId()),
          facilityIdToCode.get(facilityId), period));
    }
    return hfCmms;
  }

  private HfCmm buildHfCmm(Double cmm, Code orderableCode, String facilityCode, ProcessingPeriod period) {
    return HfCmm.builder()
        .cmm(cmm)
        .periodBegin(period.getStartDate())
        .periodEnd(period.getEndDate())
        .productCode(orderableCode.toString())
        .facilityCode(facilityCode)
        .build();
  }

  private Map<LocalDate, Long> calculatePeriodStartDateToIssue(List<ProcessingPeriod> processingPeriods,
      List<StockMovementResDto> productMovements, LocalDate firstMovementPeriodStart) {
    Map<LocalDate, Long> periodStartDateToIssue = Maps.newHashMap();
    // calculate total issue of every period which is after or equals to first stock movement period
    for (ProcessingPeriod period : processingPeriods) {
      if (period.getStartDate().isBefore(firstMovementPeriodStart)) {
        continue;
      }
      if (periodHasStockOut(productMovements, period)) {
        periodStartDateToIssue.put(period.getStartDate(), -1L);
        continue;
      }
      periodStartDateToIssue.put(period.getStartDate(), calculateTotalIssueInPeriod(productMovements, period));
    }
    return periodStartDateToIssue;
  }

  private List<ProcessingPeriod> getPeriods() {
    // TODO: 是否可以根据 M1 去重?
    return processingPeriodRepository.findAll().stream()
        .filter(e -> e.getProcessingSchedule().getCode().toString().equals("M1"))
        .filter(e -> !e.getStartDate().isAfter(LocalDate.now()))
        .sorted(Comparator.comparing(ProcessingPeriod::getStartDate))
        .collect(Collectors.toList());
  }

  private Map<UUID, Code> getOrderableIdToCode(Set<UUID> orderableIds) {
    return orderableRepository.findLatestByIds(orderableIds).stream()
        .collect(Collectors.toMap(Orderable::getId, Orderable::getProductCode));
  }

  private List<FacilityExtension> getWebFacilityExtensions() {
    Example<FacilityExtension> example = Example.of(FacilityExtension.builder().isAndroid(Boolean.FALSE).build());
    return facilityExtensionRepository.findAll(example);
  }

  private Long calculateTotalIssueInPeriod(List<StockMovementResDto> productMovements, ProcessingPeriod period) {
    long totalIssued = 0;
    for (StockMovementResDto productMovement : productMovements) {
      if (isDateInPeriodAndMovementTypeIsIssue(productMovement, period)) {
        totalIssued += productMovement.getMovementQuantity();
      }
    }
    return Math.abs(totalIssued);
  }

  private boolean periodHasStockOut(List<StockMovementResDto> productMovements, ProcessingPeriod period) {
    for (StockMovementResDto productMovement : productMovements) {
      if (isDateInPeriod(period, productMovement.getDateOfMovement()) && productMovement.getProductSoh() == 0) {
        return true;
      }
    }
    return false;
  }

  private boolean isDateInPeriodAndMovementTypeIsIssue(StockMovementResDto productMovement, ProcessingPeriod period) {
    return isDateInPeriod(period, productMovement.getDateOfMovement()) && MovementType.ISSUE.name()
        .equals(productMovement.getType());
  }

  private LocalDate queryFirstMovementPeriodStart(List<StockMovementResDto> productMovements,
      List<ProcessingPeriod> processingPeriods) {
    productMovements.sort(Comparator.comparing(StockMovementResDto::getDateOfMovement));
    LocalDate firstMovementDate = productMovements.get(0).getDateOfMovement();
    ProcessingPeriod firstMovementPeriod = processingPeriods.stream()
        .filter(period -> isDateInPeriod(period, firstMovementDate)).findFirst().orElse(null);
    if (Objects.isNull(firstMovementPeriod)) {
      return null;
    }
    return firstMovementPeriod.getStartDate();
  }

  private boolean isDateInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return !period.getStartDate().isAfter(localDate) && !period.getEndDate().isBefore(localDate);
  }
}
