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

package org.siglus.siglusapi.service;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.custom.DefaultRequisitionSearchParams;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.ProofOfDeliveryService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.ApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.IdealStockAmountReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.requisition.service.stockmanagement.StockOnHandRetrieverBuilderFactory;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.web.RequisitionController;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.SiglusProgramDto;
import org.siglus.siglusapi.util.Pagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class SiglusRequisitionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SiglusRequisitionService.class);

  @Autowired
  private RequisitionController requisitionController;

  @Autowired
  private RequisitionService requisitionService;

  @Autowired
  private PermissionService permissionService;

  @Autowired
  private SiglusOrderableService siglusOrderableService;

  @Autowired
  private ProgramExtensionService programExtensionService;

  @Autowired
  private PeriodService periodService;

  @Autowired
  private StockCardRangeSummaryStockManagementService stockCardRangeSummaryStockManagementService;

  @Autowired
  private StockOnHandRetrieverBuilderFactory stockOnHandRetrieverBuilderFactory;

  @Autowired
  private ProofOfDeliveryService proofOfDeliveryService;

  @Autowired
  private IdealStockAmountReferenceDataService idealStockAmountReferenceDataService;

  @Autowired
  private ApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Value("${service.url}")
  private String serviceUrl;

  public List<RequisitionLineItemV2Dto> createRequisitionLineItem(
      UUID requisitonId,
      List<UUID> orderableIds) {

    Profiler profiler = requisitionController
        .getProfiler("ADD_NEW_REQUISITION_LINE_ITEM_FOR_SPEC_ORDERABLE");

    Requisition existedRequisition = requisitionController.findRequisition(requisitonId, profiler);

    for (UUID orderableId : orderableIds) {
      boolean alreadyHaveCurrentOrderable = existedRequisition.getRequisitionLineItems().stream()
          .anyMatch(
              requisitionLineItem -> requisitionLineItem.getOrderable().getId().equals(orderableId)
          );
      if (alreadyHaveCurrentOrderable) {
        throw new ValidationMessageException(
            new Message(MessageKeys.ERROR_ORDERABLE_ALREADY_IN_GIVEN_REQUISITION));
      }
    }

    UUID programId = existedRequisition.getProgramId();
    ProgramDto program = requisitionController.findProgram(programId, profiler);

    UUID facilityId = existedRequisition.getFacilityId();
    FacilityDto facility = requisitionController.findFacility(facilityId, profiler);

    permissionService.canInitOrAuthorizeRequisition(programId, facilityId);

    List<RequisitionLineItem> lineItemList = constructLineItem(
        existedRequisition, program, facility, orderableIds);

    return buildLineItemV2Dto(lineItemList);
  }

  public List<RequisitionLineItem> constructLineItem(Requisition requisition,
      ProgramDto program,
      FacilityDto facility,
      List<UUID> orderableIds) {

    Profiler profiler = new Profiler("REQUISITION_INITIATE_SERVICE");
    profiler.setLogger(LOGGER);
    profiler.start("BUILD_REQUISITION");

    RequisitionTemplate requisitionTemplate = requisition.getTemplate();

    Integer numberOfPreviousPeriodsToAverage = decrementOrZero(requisitionTemplate
        .getNumberOfPeriodsToAverage());

    profiler.start("GET_PREV_REQUISITIONS_FOR_AVERAGING");

    List<StockCardRangeSummaryDto> stockCardRangeSummaryDtos = null;
    List<StockCardRangeSummaryDto> stockCardRangeSummariesToAverage = null;
    List<ProcessingPeriodDto> periods = null;
    List<Requisition> previousRequisitions = requisition.getPreviousRequisitions();

    SiglusProgramDto programDto = programExtensionService.getProgram(program.getId());
    UUID virtualProgramId = programDto.getIsVirtual() ? programDto.getId()
        : programDto.getParentId();
    if (requisitionTemplate.isPopulateStockOnHandFromStockCards()) {
      ProcessingPeriodDto period = periodService.getPeriod(requisition.getProcessingPeriodId());
      stockCardRangeSummaryDtos =
          stockCardRangeSummaryStockManagementService
              .search(virtualProgramId, facility.getId(), null,
                  requisition.getActualStartDate(),
                  requisition.getActualEndDate());

      LocalDate startDateForCalculateAvg;
      LocalDate endDateForCalculateAvg = requisition.getActualEndDate();
      if (!CollectionUtils.isEmpty(previousRequisitions)) {
        Set<UUID> periodIds = previousRequisitions.stream()
            .map(Requisition::getProcessingPeriodId)
            .collect(toSet());
        periods = periodService.getPeriods(periodIds);
        periods.add(period);

        startDateForCalculateAvg = previousRequisitions.stream()
            .min(Comparator.comparing(Requisition::getActualStartDate))
            .get()
            .getActualStartDate();
        if (requisition.getEmergency()) {
          List<Requisition> requisitions =
              searchAuthorizedRequisitions(requisition.getFacilityId(),
                  requisition.getProgramId(),
                  period.getId(), false);
          endDateForCalculateAvg = requisitions.get(0).getActualEndDate();
        }
      } else {
        startDateForCalculateAvg = period.getStartDate();
        periods = Lists.newArrayList(period);
      }

      stockCardRangeSummariesToAverage =
          stockCardRangeSummaryStockManagementService
              .search(virtualProgramId, facility.getId(), null,
                  startDateForCalculateAvg,
                  endDateForCalculateAvg);


    } else if (numberOfPreviousPeriodsToAverage > previousRequisitions.size()) {
      numberOfPreviousPeriodsToAverage = previousRequisitions.size();
    }

    profiler.start("FIND_APPROVED_PRODUCTS");

    profiler.start("FIND_STOCK_ON_HANDS");
    Map<UUID, Integer> orderableSoh = getOrderableSohMap(requisitionTemplate, virtualProgramId,
        facility.getId(), requisition.getActualEndDate());

    profiler.start("FIND_BEGINNING_BALANCES");
    Map<UUID, Integer> orderableBeginning = getOrderableBegingningMap(requisitionTemplate,
        virtualProgramId, facility.getId(), requisition.getActualStartDate().minusDays(1));

    ProofOfDeliveryDto pod = null;
    if (!isEmpty(previousRequisitions)) {
      pod = proofOfDeliveryService.get(previousRequisitions.get(0));
    }

    profiler.start("FIND_IDEAL_STOCK_AMOUNTS");
    final Map<UUID, Integer> idealStockAmounts = idealStockAmountReferenceDataService
        .search(requisition.getFacilityId(), requisition.getProcessingPeriodId())
        .stream()
        .collect(toMap(isa -> isa.getCommodityType().getId(), IdealStockAmountDto::getAmount));

    ApproveProductsAggregator approvedProducts = approvedProductReferenceDataService
        .getApprovedProducts(facility.getId(), program.getId());

    List<RequisitionLineItem> lineItemList = new ArrayList<>();
    for (ApprovedProductDto approvedProductDto : approvedProducts.getAllProducts().values()) {
      UUID orderableId = approvedProductDto.getOrderable().getId();

      if (orderableIds.contains(orderableId)) {
        Integer stockOnHand = orderableSoh.get(orderableId);
        Integer beginningBalances = orderableBeginning.get(orderableId);

        lineItemList.add(requisition.constructLineItem(requisitionTemplate, stockOnHand,
            beginningBalances, approvedProductDto, numberOfPreviousPeriodsToAverage,
            idealStockAmounts, stockCardRangeSummaryDtos, stockCardRangeSummariesToAverage,
            periods, pod, approvedProducts.getFullSupplyProducts()));
      }
    }

    //requisitionRepository.save(requisition);

    profiler.stop().log();
    return lineItemList;
  }

  private Map<UUID, Integer> getOrderableBegingningMap(RequisitionTemplate requisitionTemplate,
      UUID programId, UUID facilityId, LocalDate actualEndDate) {
    return stockOnHandRetrieverBuilderFactory.getInstance(requisitionTemplate,
        RequisitionLineItem.BEGINNING_BALANCE)
        .forProgram(programId)
        .forFacility(facilityId)
        .asOfDate(actualEndDate)
        .build().get();
  }

  private Map<UUID, Integer> getOrderableSohMap(RequisitionTemplate requisitionTemplate,
      UUID programId, UUID facilityId, LocalDate actualEndDate) {
    return stockOnHandRetrieverBuilderFactory.getInstance(requisitionTemplate,
        RequisitionLineItem.STOCK_ON_HAND)
        .forProgram(programId)
        .forFacility(facilityId)
        .asOfDate(actualEndDate)
        .build().get();
  }

  public List<Requisition> searchAuthorizedRequisitions(UUID facilityId, UUID programId,
      UUID periodId, boolean emergency) {
    RequisitionSearchParams params = new DefaultRequisitionSearchParams(
        facilityId, programId, periodId, null, emergency,
        null, null, null, null,
        EnumSet.of(AUTHORIZED));
    PageRequest pageRequest = new PageRequest(Pagination.getDefaultPageNumber(),
        Pagination.getNoPaginationPageSize());
    return searchRequisitions(params, pageRequest, false).getContent();
  }

  public Page<Requisition> searchRequisitions(RequisitionSearchParams params,
      Pageable pageable,
      boolean needMatchStatusAndPermission) {
    if (needMatchStatusAndPermission) {
      Set<RequisitionStatus> canSeeRequisitionStatus = getUserCanSeeRequisitionStatus(
          params.getFacility(),
          params.getProgram());
      params.setRequisitionStatuses(canSeeRequisitionStatus);
    }
    return requisitionService.searchRequisitions(params, pageable);
  }

  private Set<RequisitionStatus> getUserCanSeeRequisitionStatus(UUID facilityId, UUID programId) {
    final boolean canCreate =
        permissionService.canSubmitRequisition(programId, facilityId);
    final boolean canAuth = permissionService.canAuthorizeRequisition(programId, facilityId);
    Set<RequisitionStatus> canSeeRequisitionStatus = Sets.newHashSet();

    if (canCreate) {
      canSeeRequisitionStatus.addAll(Arrays.asList(SUBMITTED, AUTHORIZED, APPROVED));
    }

    if (canAuth) {
      canSeeRequisitionStatus.addAll(Arrays.asList(AUTHORIZED, APPROVED));
    }

    if (canCreate && canAuth) {
      canSeeRequisitionStatus.clear();
      canSeeRequisitionStatus.addAll(Arrays.asList(AUTHORIZED, APPROVED));
    }
    return canSeeRequisitionStatus;
  }

  private Integer decrementOrZero(Integer numberOfPreviousPeriodsToAverage) {
    // numberOfPeriodsToAverage is always >= 2 or null
    if (numberOfPreviousPeriodsToAverage == null) {
      numberOfPreviousPeriodsToAverage = 0;
    } else {
      numberOfPreviousPeriodsToAverage--;
    }
    return numberOfPreviousPeriodsToAverage;
  }

  private List<OrderableExpirationDateDto> findOrderableIds(
      List<RequisitionLineItem> requisitionLineItems) {
    Set<UUID> orderableIds = requisitionLineItems
        .stream()
        .map(item -> item.getOrderable().getId())
        .collect(toSet());
    return orderableIds.isEmpty() ? new ArrayList<>() :
        siglusOrderableService.getOrderableExpirationDate(orderableIds);
  }

  private List<RequisitionLineItemV2Dto> buildLineItemV2Dto(
      List<RequisitionLineItem> lineItemList) {
    List<OrderableExpirationDateDto> expirationDateDtos = findOrderableIds(lineItemList);
    return lineItemList
        .stream()
        .map(line -> {
          // The whole object is not required here
          OrderableDto orderable = new OrderableDto();
          orderable.setId(line.getOrderable().getId());
          orderable.setMeta(new MetadataDto(line.getOrderable().getVersionNumber(), null));

          ApprovedProductDto approvedProduct = new ApprovedProductDto(null, null,
              null, null, null, null,
              new MetadataDto(line.getFacilityTypeApprovedProduct().getVersionNumber(),
                  null));
          approvedProduct.setId(line.getFacilityTypeApprovedProduct().getId());

          RequisitionLineItemV2Dto lineDto = RequisitionLineItemV2Dto.builder()
              .serviceUrl(serviceUrl)
              .build();
          lineDto.setServiceUrl(serviceUrl);
          line.export(lineDto, orderable, approvedProduct);
          setOrderableExpirationDate(expirationDateDtos, orderable, lineDto);
          return lineDto;
        })
        .collect(Collectors.toList());
  }

  private void setOrderableExpirationDate(List<OrderableExpirationDateDto> expirationDateDtos,
      OrderableDto orderable, RequisitionLineItemV2Dto lineDto) {
    OrderableExpirationDateDto expirationDate = expirationDateDtos
        .stream()
        .filter(expirationDateDto ->
            expirationDateDto.getOrderableId().equals(orderable.getId()))
        .findFirst()
        .orElse(null);
    if (null != expirationDate) {
      lineDto.setExpirationDate(expirationDate.getExpirationDate());
    } else {
      lineDto.setExpirationDate(null);
    }
  }
}
