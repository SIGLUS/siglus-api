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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.APPROVED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.AUTHORIZED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.IN_APPROVAL;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.RELEASED_WITHOUT_ORDER;
import static org.openlmis.requisition.domain.requisition.RequisitionStatus.SUBMITTED;
import static org.openlmis.requisition.dto.OrderStatus.SHIPPED;
import static org.siglus.siglusapi.constant.PaginationConstants.UNPAGED;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.referencedata.dto.OrderableExpirationDateDto;
import org.openlmis.referencedata.exception.NotFoundException;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem.Importer;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BaseRequisitionDto;
import org.openlmis.requisition.dto.BaseRequisitionLineItemDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.IdealStockAmountDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.ShipmentDto;
import org.openlmis.requisition.dto.ShipmentLineItemDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.dto.stockmanagement.StockCardRangeSummaryDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.PeriodService;
import org.openlmis.requisition.service.PermissionService;
import org.openlmis.requisition.service.ProofOfDeliveryService;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.openlmis.requisition.service.fulfillment.ShipmentFulfillmentService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.ApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.FacilityTypeApprovedProductReferenceDataService;
import org.openlmis.requisition.service.referencedata.IdealStockAmountReferenceDataService;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.service.stockmanagement.StockCardRangeSummaryStockManagementService;
import org.openlmis.requisition.service.stockmanagement.StockOnHandRetrieverBuilderFactory;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.RequisitionAuthenticationHelper;
import org.openlmis.requisition.web.QueryRequisitionSearchParams;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.requisition.web.RequisitionV2Controller;
import org.siglus.common.domain.RequisitionTemplateExtension;
import org.siglus.common.dto.RequisitionTemplateExtensionDto;
import org.siglus.common.repository.RequisitionTemplateExtensionRepository;
import org.siglus.common.util.SimulateAuthenticationHelper;
import org.siglus.siglusapi.domain.RequisitionLineItemExtension;
import org.siglus.siglusapi.dto.SiglusProgramDto;
import org.siglus.siglusapi.dto.SiglusRequisitionDto;
import org.siglus.siglusapi.dto.SiglusRequisitionLineItemDto;
import org.siglus.siglusapi.repository.SiglusRequisitionLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusRequisitionRequisitionService;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusRequisitionService {

  @Autowired
  private RequisitionV2Controller requisitionV2Controller;

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
  private SiglusRequisitionRequisitionService siglusRequisitionRequisitionService;

  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private RequisitionTemplateExtensionRepository requisitionTemplateExtensionRepository;

  @Autowired
  private SiglusRequisitionLineItemExtensionRepository lineItemExtensionRepository;

  @Autowired
  private FacilityTypeApprovedProductReferenceDataService
      facilityTypeApprovedProductReferenceDataService;

  @Autowired
  private RequisitionRepository requisitionRepository;

  @Autowired
  RequisitionAuthenticationHelper authenticationHelper;

  @Autowired
  ApprovedProductReferenceDataService approvedProductReferenceDataService;

  @Autowired
  private SimulateAuthenticationHelper simulateAuthenticationHelper;

  @Autowired
  private SiglusUsageReportService siglusUsageReportService;

  @Autowired
  private OrderFulfillmentService orderFulfillmentService;

  @Autowired
  private ShipmentFulfillmentService shipmentFulfillmentService;

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Value("${service.url}")
  private String serviceUrl;

  @Transactional
  public SiglusRequisitionDto updateRequisition(UUID requisitionId,
      @RequestBody SiglusRequisitionDto requisitionDto,
      HttpServletRequest request, HttpServletResponse response) {
    // call modify OpenLMIS API
    RequisitionV2Dto upadateRequsitionDto = requisitionV2Controller
        .updateRequisition(requisitionId, requisitionDto, request, response);
    saveLineItemExtension(requisitionDto, upadateRequsitionDto);

    return siglusUsageReportService.saveUsageReport(requisitionDto, upadateRequsitionDto);
  }

  private void saveLineItemExtension(RequisitionV2Dto toUpdatedDto, RequisitionV2Dto updatedDto) {
    List<RequisitionLineItem.Importer> lineItems = updatedDto.getRequisitionLineItems();
    if (!lineItems.isEmpty()) {
      List<UUID> lineItemsId = updatedDto.getRequisitionLineItems()
          .stream()
          .map(Importer::getId)
          .collect(Collectors.toList());
      List<RequisitionLineItemExtension> updateExtension = new ArrayList<>();
      List<RequisitionLineItemExtension> extensions =
          lineItemExtensionRepository.findLineItems(lineItemsId);
      lineItems.forEach(lineItem -> {
        RequisitionLineItemV2Dto dto = findDto(lineItem, toUpdatedDto);
        RequisitionLineItemExtension requisitionLineItemExtension =
            findLineItemExtension(extensions, dto);
        if (requisitionLineItemExtension != null) {
          requisitionLineItemExtension.setAuthorizedQuantity(dto.getAuthorizedQuantity());
          updateExtension.add(requisitionLineItemExtension);
        } else if (dto != null && dto.getAuthorizedQuantity() != null) {
          RequisitionLineItemExtension extension = new RequisitionLineItemExtension();
          extension.setRequisitionLineItemId(lineItem.getId());
          extension.setAuthorizedQuantity(dto.getAuthorizedQuantity());
          updateExtension.add(extension);
        }
      });
      lineItemExtensionRepository.save(updateExtension);
    }
  }

  private RequisitionLineItemV2Dto findDto(RequisitionLineItem.Importer lineItem,
      RequisitionV2Dto dto) {
    for (RequisitionLineItem.Importer lineItemV2Dto : dto.getRequisitionLineItems()) {
      if (lineItemV2Dto.getOrderableIdentity().getId()
          .equals(lineItem.getOrderableIdentity().getId())) {
        return (RequisitionLineItemV2Dto) lineItemV2Dto;
      }
    }
    return null;
  }

  @Transactional
  public List<SiglusRequisitionLineItemDto> createRequisitionLineItem(
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

    UserDto userDto = authenticationHelper.getCurrentUser();
    FacilityDto userFacility = requisitionController.findFacility(
        userDto.getHomeFacilityId(), profiler);

    List<RequisitionLineItem> lineItemList = constructLineItem(
        existedRequisition, program, facility, orderableIds, userFacility);

    boolean isApprove = requisitionService
        .validateCanApproveRequisition(existedRequisition, userDto.getId()).isSuccess();
    boolean isInternalFacility = userDto.getHomeFacilityId()
        .equals(existedRequisition.getFacilityId());
    boolean isExternalApprove = isApprove && !isInternalFacility;

    return buildSiglusLineItem(lineItemList, isExternalApprove);
  }

  private List<RequisitionLineItem> constructLineItem(Requisition requisition, ProgramDto program,
      FacilityDto facility, List<UUID> orderableIds, FacilityDto userFacility) {
    RequisitionTemplate requisitionTemplate = requisition.getTemplate();
    Integer numberOfPreviousPeriodsToAverage = decrementOrZero(requisitionTemplate
        .getNumberOfPeriodsToAverage());
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
            .orElseThrow(() -> new NotFoundException("Earlier Rquisition Not Found"))
            .getActualStartDate();
        if (requisition.getEmergency()) {
          List<Requisition> requisitions =
              requisitionService.searchAfterAuthorizedRequisitions(requisition.getFacilityId(),
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

    OAuth2Authentication originAuth = simulateAuthenticationHelper.simulateCrossServiceAuth();

    Map<UUID, Integer> orderableSoh = getOrderableSohMap(requisitionTemplate, virtualProgramId,
        facility.getId(), requisition.getActualEndDate());

    Map<UUID, Integer> orderableBeginning = getOrderableBegingningMap(requisitionTemplate,
        virtualProgramId, facility.getId(), requisition.getActualStartDate().minusDays(1));

    simulateAuthenticationHelper.recoveryAuth(originAuth);

    ProofOfDeliveryDto pod = null;
    if (!isEmpty(previousRequisitions)) {
      pod = proofOfDeliveryService.get(previousRequisitions.get(0));
    }

    final Map<UUID, Integer> idealStockAmounts = idealStockAmountReferenceDataService
        .search(requisition.getFacilityId(), requisition.getProcessingPeriodId())
        .stream()
        .collect(toMap(isa -> isa.getCommodityType().getId(), IdealStockAmountDto::getAmount));

    // including the approved product of associate program, and the user facility
    ApproveProductsAggregator approvedProducts = requisitionService.getApproveProduct(
        userFacility, program, requisitionTemplate);

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
    return lineItemList;
  }

  @Transactional
  public void deleteRequisition(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    List<UUID> ids = findLineItemIds(requisition);
    siglusRequisitionRequisitionService.deleteRequisition(requisitionId);
    log.info("find line item extension: {}", ids);
    List<RequisitionLineItemExtension> extensions = ids.isEmpty() ? new ArrayList<>() :
        lineItemExtensionRepository.findLineItems(ids);
    if (!extensions.isEmpty()) {
      log.info("delete line item extension: {}", extensions);
      lineItemExtensionRepository.delete(extensions);
    }
    siglusUsageReportService.deleteUsageReport(requisitionId);
  }

  private List<UUID> findLineItemIds(Requisition requisition) {
    List<RequisitionLineItem> lineItems = requisition.getRequisitionLineItems();
    return lineItems.stream()
        .map(BaseEntity::getId)
        .collect(Collectors.toList());
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

  private List<SiglusRequisitionLineItemDto> buildSiglusLineItem(
      List<RequisitionLineItem> lineItemList, boolean isExternalApprove) {
    List<OrderableExpirationDateDto> expirationDateDtos = findOrderableIds(lineItemList);

    Set<VersionEntityReference> references = lineItemList.stream()
        .map(line -> {
          VersionEntityReference reference = line.getFacilityTypeApprovedProduct();
          return new VersionEntityReference(reference.getId(), reference.getVersionNumber());
        }).collect(toSet());

    List<ApprovedProductDto> list = facilityTypeApprovedProductReferenceDataService
        .findByIdentities(references);

    Map<UUID, ApprovedProductDto> approvedProductDtoMap = list.stream()
        .collect(Collectors.toMap(ApprovedProductDto::getId, dto -> dto));

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
          UUID approvedProductId = line.getFacilityTypeApprovedProduct().getId();
          approvedProduct.setId(approvedProductId);

          RequisitionLineItemV2Dto lineDto = new RequisitionLineItemV2Dto();
          lineDto.setServiceUrl(serviceUrl);
          line.export(lineDto, orderable, approvedProduct);
          setOrderableExpirationDate(expirationDateDtos, orderable, lineDto);
          if (isExternalApprove) {
            lineDto.setRequestedQuantity(0);
            lineDto.setAuthorizedQuantity(0);
          }

          SiglusRequisitionLineItemDto siglusRequisitionLineItemDto =
              new SiglusRequisitionLineItemDto();
          siglusRequisitionLineItemDto.setLineItem(lineDto);
          siglusRequisitionLineItemDto
              .setApprovedProduct(approvedProductDtoMap.get(approvedProductId));

          return siglusRequisitionLineItemDto;
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

  public SiglusRequisitionDto searchRequisition(UUID requisitionId) {
    // call origin OpenLMIS API
    // reason: 1. set template extension
    //         1. 2. set line item authorized quality extension
    RequisitionV2Dto requisitionDto =
        siglusRequisitionRequisitionService.searchRequisition(requisitionId);
    setTemplateExtension(requisitionDto);
    setLineItemExtension(requisitionDto);

    filterProductsIfEmergency(requisitionDto);
    // set available products in approve page
    setAvailableProductsForApprovePage(requisitionDto);

    SiglusRequisitionDto siglusRequisitionDto = siglusUsageReportService
        .searchUsageReport(requisitionDto);
    return setIsFinalApproval(siglusRequisitionDto);
  }

  private void filterProductsIfEmergency(RequisitionV2Dto requisition) {
    if (!requisition.getEmergency()) {
      return;
    }
    List<RequisitionV2Dto> otherEmergencyReqs = getOtherEmergencyRequisition(requisition);
    if (otherEmergencyReqs.isEmpty()) {
      return;
    }
    Set<UUID> productIdsInProgress = otherEmergencyReqs.stream()
        .filter(this::isRequisitionInProgress)
        .map(RequisitionV2Dto::getLineItems)
        .flatMap(Collection::stream)
        .map(lineItem -> lineItem.getOrderableIdentity().getId())
        .collect(toSet());
    filterProductsInRequisition(requisition, productIdsInProgress);
    Set<UUID> productIdsNotFullyShipped = otherEmergencyReqs.stream()
        .filter(req -> req.getStatus() == RELEASED)
        .map(this::mapToNotFullyShippedProductIds)
        .flatMap(Collection::stream)
        .collect(toSet());
    filterProductsInRequisition(requisition, productIdsNotFullyShipped);
  }

  private List<RequisitionV2Dto> getOtherEmergencyRequisition(BaseRequisitionDto requisition) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.set(QueryRequisitionSearchParams.EMERGENCY, Boolean.TRUE.toString());
    String periodId = requisition.getProcessingPeriod().getId().toString();
    queryParams.set(QueryRequisitionSearchParams.PROCESSING_PERIOD, periodId);
    String facilityId = requisition.getFacility().getId().toString();
    queryParams.set(QueryRequisitionSearchParams.FACILITY, facilityId);
    return siglusRequisitionRequisitionService
        .searchRequisitions(new QueryRequisitionSearchParams(queryParams), UNPAGED)
        .getContent().stream()
        .filter(req -> !req.getId().equals(requisition.getId()))
        .map(BaseDto::getId)
        .map(siglusRequisitionRequisitionService::searchRequisition)
        .collect(toList());
  }

  private boolean isRequisitionInProgress(BaseRequisitionDto requisition) {
    RequisitionStatus status = requisition.getStatus();
    return status == SUBMITTED || status == AUTHORIZED || status == IN_APPROVAL
        || status == APPROVED;
  }

  private void filterProductsInRequisition(RequisitionV2Dto requisition,
      Set<UUID> productIdsToBeFiltered) {
    requisition.getAvailableProducts()
        .removeIf(product -> productIdsToBeFiltered.contains(product.getId()));
  }

  private Set<UUID> mapToNotFullyShippedProductIds(RequisitionV2Dto requisition) {
    HashMap<UUID, Integer> reqProductCountMap = requisition.getLineItems().stream()
        .collect(HashMap::new, this::mergeReqLineItem, this::mergeProductCount);
    searchOrders(requisition);
    HashMap<UUID, Integer> shipmentProductCountMap = searchOrders(requisition).stream()
        .filter(order -> order.getStatus() == SHIPPED)
        .map(BaseDto::getId)
        .map(shipmentFulfillmentService::getShipments)
        .flatMap(Collection::stream)
        .map(ShipmentDto::getLineItems)
        .flatMap(Collection::stream)
        // caution: may cause bad performance
        .map(this::convertFromPacksToDoses)
        .collect(HashMap::new, this::mergeShipmentLineItem, this::mergeProductCount);
    return reqProductCountMap.entrySet().stream()
        .filter(entry -> !shipmentProductCountMap.containsKey(entry.getKey())
            || entry.getValue() > shipmentProductCountMap.get(entry.getKey()))
        .map(Entry::getKey)
        .collect(Collectors.toSet());
  }

  private List<OrderDto> searchOrders(BaseRequisitionDto requisition) {
    return orderFulfillmentService
        .search(requisition.getSupplyingFacility(), requisition.getFacilityId(),
            requisition.getProgramId(), requisition.getProcessingPeriodId(), null/*ignore status*/)
        .stream()
        .filter(order -> requisition.getId().equals(order.getExternalId()))
        .collect(Collectors.toList());
  }

  private ShipmentLineItemDto convertFromPacksToDoses(ShipmentLineItemDto lineItem) {
    org.openlmis.referencedata.dto.OrderableDto product = orderableReferenceDataService
        .findOne(lineItem.getOrderable().getId());
    long shippedQuantity = lineItem.getQuantityShipped() * product.getNetContent();
    ShipmentLineItemDto convertedLineItem = new ShipmentLineItemDto();
    convertedLineItem.setOrderable(lineItem.getOrderable());
    convertedLineItem.setLot(lineItem.getLot());
    convertedLineItem.setQuantityShipped(shippedQuantity);
    return convertedLineItem;
  }

  private void mergeShipmentLineItem(Map<UUID, Integer> map, ShipmentLineItemDto newValue) {
    UUID id = newValue.getOrderable().getId();
    if (map.containsKey(id)) {
      map.put(id, newValue.getQuantityShipped().intValue() + map.get(id));
    } else {
      map.put(id, newValue.getQuantityShipped().intValue());
    }
  }

  private void mergeReqLineItem(Map<UUID, Integer> map, BaseRequisitionLineItemDto newValue) {
    UUID id = newValue.getOrderableIdentity().getId();
    if (map.containsKey(id)) {
      map.put(id, newValue.getApprovedQuantity() + map.get(id));
    } else {
      map.put(id, newValue.getApprovedQuantity());
    }
  }

  private void mergeProductCount(Map<UUID, Integer> map1, Map<UUID, Integer> map2) {
    map2.forEach((k, v) -> {
      if (map1.containsKey(k)) {
        map1.put(k, v + map1.get(k));
      } else {
        map1.put(k, v);
      }
    });
  }

  private void setAvailableProductsForApprovePage(RequisitionV2Dto requisitionDto) {
    UUID requisitionId = requisitionDto.getId();
    Profiler profiler = requisitionController
        .getProfiler("GET_REQUISITION_TO_APPROVE", requisitionId);
    Requisition requisition = requisitionController
        .findRequisition(requisitionId, profiler);
    UserDto userDto = authenticationHelper.getCurrentUser();
    if (requisitionService
        .validateCanApproveRequisition(requisition, userDto.getId()).isSuccess()) {

      Set<VersionObjectReferenceDto> availableProducts = requisitionDto.getAvailableProducts();

      ProgramDto mainProgram = requisitionController
          .findProgram(requisition.getProgramId(), profiler);
      FacilityDto approverFacility = requisitionController
          .findFacility(userDto.getHomeFacilityId(), profiler);

      Set<UUID> approverMainProgramAndAssociateProgramApprovedProducts
          = Optional
          .ofNullable(requisitionService
              .getApproveProduct(approverFacility, mainProgram, requisition.getTemplate())
              .getApprovedProductReferences())
          .orElse(Collections.emptySet())
          .stream()
          .map(ApprovedProductReference::getOrderable)
          .map(VersionEntityReference::getId)
          .collect(toSet());

      // keep only products in approver facility main & associate programs
      // toggle no/full-supply will update the version
      // version mismatch in VersionObjectReferenceDto is not needed here
      requisitionDto.setAvailableProducts(availableProducts.stream()
          .filter(product ->
              approverMainProgramAndAssociateProgramApprovedProducts.contains(product.getId()))
          .collect(toSet()));
    }
  }

  private void setTemplateExtension(RequisitionV2Dto requisitionDto) {
    BasicRequisitionTemplateDto templateDto = requisitionDto.getTemplate();
    RequisitionTemplateExtension extension = requisitionTemplateExtensionRepository
        .findByRequisitionTemplateId(requisitionDto.getTemplate().getId());
    templateDto.setExtension(RequisitionTemplateExtensionDto.from(extension));
    requisitionDto.setTemplate(templateDto);
  }

  private void setLineItemExtension(RequisitionV2Dto requisitionDto) {
    List<RequisitionLineItem.Importer> lineItems = requisitionDto.getRequisitionLineItems();
    List<UUID> lineItemsId = lineItems.stream()
        .map(Importer::getId)
        .collect(Collectors.toList());
    if (!lineItemsId.isEmpty()) {
      log.info("find line item extension: {}", lineItemsId);
      List<RequisitionLineItemExtension> lineItemExtension =
          lineItemExtensionRepository.findLineItems(lineItemsId);
      lineItems.forEach(lineItem -> {
        RequisitionLineItemExtension itemExtension = findLineItemExtension(lineItemExtension,
            (RequisitionLineItemV2Dto) lineItem);
        if (itemExtension != null) {
          RequisitionLineItemV2Dto lineItemV2Dto = (RequisitionLineItemV2Dto) lineItem;
          lineItemV2Dto.setAuthorizedQuantity(itemExtension.getAuthorizedQuantity());
        }
      });
    }
  }

  private SiglusRequisitionDto setIsFinalApproval(SiglusRequisitionDto siglusRequisitionDto) {
    if (siglusRequisitionDto.getStatus().duringApproval()) {
      UUID nodeId = siglusRequisitionDto.getSupervisoryNode();
      SupervisoryNodeDto supervisoryNodeDto = supervisoryNodeService
          .findOne(nodeId);

      if (supervisoryNodeDto != null && supervisoryNodeDto.getParentNode() == null) {
        siglusRequisitionDto.setIsFinalApproval(Boolean.TRUE);
        return siglusRequisitionDto;
      }
    }

    siglusRequisitionDto.setIsFinalApproval(Boolean.FALSE);
    return siglusRequisitionDto;
  }

  private RequisitionLineItemExtension findLineItemExtension(
      List<RequisitionLineItemExtension> extensions,
      RequisitionLineItemV2Dto lineItem) {
    if (lineItem == null || lineItem.getId() == null) {
      return null;
    }

    return extensions.stream().filter(extension ->
        lineItem.getId().equals(extension.getRequisitionLineItemId()))
        .findFirst().orElse(null);
  }

  public void activateArchivedProducts(UUID requisitionId, UUID facilityId) {
    Set<UUID> orderableIds = findLineItemOrderableIds(requisitionId);
    archiveProductService.activateArchivedProducts(orderableIds, facilityId);
  }

  private Set<UUID> findLineItemOrderableIds(UUID requisitionId) {
    Requisition requisition = requisitionRepository.findOne(requisitionId);
    List<RequisitionLineItem> lineItems = requisition.getRequisitionLineItems();
    return lineItems.stream()
        .map(lineItem -> lineItem.getOrderable().getId())
        .collect(Collectors.toSet());
  }

  public Page<BasicRequisitionDto> searchRequisitions(MultiValueMap<String, String> queryParams,
      Pageable pageable) {
    Set<RequisitionStatus> requisitionStatusDisplayInRequisitionHistory =
        getRequisitionStatusDisplayInRequisitionHistory(
            UUID.fromString(queryParams.getFirst(QueryRequisitionSearchParams.FACILITY)),
            UUID.fromString(queryParams.getFirst(QueryRequisitionSearchParams.PROGRAM)));
    requisitionStatusDisplayInRequisitionHistory.forEach(requisitionStatus -> queryParams
        .add(QueryRequisitionSearchParams.REQUISITION_STATUS, requisitionStatus.toString()));
    RequisitionSearchParams params = new QueryRequisitionSearchParams(queryParams);
    return siglusRequisitionRequisitionService.searchRequisitions(params, pageable);
  }

  private Set<RequisitionStatus> getRequisitionStatusDisplayInRequisitionHistory(UUID facilityId,
      UUID programId) {
    Requisition requisition = new Requisition();
    requisition.setProgramId(programId);
    requisition.setFacilityId(facilityId);
    Set<RequisitionStatus> canSeeRequisitionStatus = Sets.newHashSet();

    final boolean canAuth = permissionService.canAuthorizeRequisition(requisition).isSuccess();
    if (canAuth) {
      canSeeRequisitionStatus.addAll(
          Arrays.asList(AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER));
      return canSeeRequisitionStatus;
    }

    final boolean canCreate = permissionService.canSubmitRequisition(requisition).isSuccess();
    if (canCreate) {
      canSeeRequisitionStatus.addAll(Arrays
          .asList(SUBMITTED, AUTHORIZED, IN_APPROVAL, APPROVED, RELEASED, RELEASED_WITHOUT_ORDER));
      return canSeeRequisitionStatus;
    }

    return canSeeRequisitionStatus;
  }

  @Transactional
  public SiglusRequisitionDto initiate(UUID programId, UUID facilityId,
      UUID suggestedPeriod,
      boolean emergency,
      String physicalInventoryDateStr,
      HttpServletRequest request, HttpServletResponse response) {
    RequisitionV2Dto v2Dto = requisitionV2Controller
        .initiate(programId, facilityId, suggestedPeriod, emergency,
            physicalInventoryDateStr, request, response);
    return siglusUsageReportService.initiateUsageReport(v2Dto);
  }
}
