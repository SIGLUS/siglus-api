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
import static org.openlmis.requisition.web.ResourceNames.ORDERABLES;
import static org.openlmis.stockmanagement.service.StockmanagementPermissionService.STOCK_CARDS_VIEW;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.service.referencedata.FulfillmentOrderableReferenceDataService;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.ProgramOrderableDto;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.OrderLineItemDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.SiglusOrderLineItemDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class SiglusOrderService {

  @Autowired
  private OrderController orderController;

  @Autowired
  private RequisitionController requisitionController;

  @Autowired
  private RequisitionService requisitionService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Autowired
  private FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private SiglusArchiveProductService siglusArchiveProductService;

  @Autowired
  private SiglusStockCardSummariesService siglusStockCardSummariesService;

  @Autowired
  private FulfillmentOrderableReferenceDataService fulfillmentOrderableReferenceDataService;

  @Value("${service.url}")
  private String serviceUrl;

  public SiglusOrderDto searchOrderById(UUID orderId) {
    OrderDto orderDto = orderController.getOrder(orderId, Collections.emptySet());

    return SiglusOrderDto.builder()
        .order(orderDto)
        .availableProducts(getAllUserAvailableProductAggregator(orderDto)).build();
  }

  public List<SiglusOrderLineItemDto> createOrderLineItem(List<UUID> orderableIds) {
    Map<UUID, StockCardSummaryV2Dto> summaryMap = getOrderableIdSohMap(
        authenticationHelper.getCurrentUser().getHomeFacilityId());

    Map<UUID, OrderableDto> orderableMap = fulfillmentOrderableReferenceDataService
        .findByIds(orderableIds).stream()
        .collect(toMap(OrderableDto::getId, orderableDto -> orderableDto));
    return orderableIds.stream()
        .filter(orderableMap::containsKey)
        .map(orderableId -> buildOrderLineItem(summaryMap, orderableMap, orderableId))
        .collect(toList());
  }

  private SiglusOrderLineItemDto buildOrderLineItem(Map<UUID, StockCardSummaryV2Dto> summaryMap,
      Map<UUID, OrderableDto> orderableMap, UUID orderableId) {
    OrderLineItemDto orderLineItemDto = new OrderLineItemDto();
    OrderableDto orderableDto = orderableMap.get(orderableId);
    orderLineItemDto.setOrderable(convertOrderableDto(orderableDto));
    // no order quantity/packsToOrder, manually input by user
    orderLineItemDto.setOrderedQuantity(0L);

    List<ObjectReferenceDto> lotList = summaryMap.get(orderableId).getCanFulfillForMe()
        .stream()
        .filter(canFulfillForMeEntryDto -> canFulfillForMeEntryDto.getStockOnHand() > 0)
        .map(CanFulfillForMeEntryDto::getLot)
        .collect(Collectors.toList());
    return SiglusOrderLineItemDto
        .builder()
        .orderLineItem(orderLineItemDto)
        .lots(lotList)
        .build();
  }

  private Set<VersionObjectReferenceDto> getAllUserAvailableProductAggregator(OrderDto orderDto) {
    UUID requisitionId = orderDto.getExternalId();
    Requisition requisition = requisitionController.findRequisition(requisitionId,
        requisitionController.getProfiler("GET_ORDER"));

    FacilityDto approverFacility = facilityReferenceDataService.findOne(
        orderDto.getCreatedBy().getHomeFacilityId());
    FacilityDto userHomeFacility = facilityReferenceDataService.findOne(
        authenticationHelper.getCurrentUser().getHomeFacilityId());
    ProgramDto requisitionProgram = programReferenceDataService.findOne(
        requisition.getProgramId());
    // 10+ seconds cost when call following requisitionService.getApproveProduct
    ApproveProductsAggregator approverProductAggregator = requisitionService.getApproveProduct(
        approverFacility, requisitionProgram, requisition.getTemplate());
    ApproveProductsAggregator userProductAggregator = requisitionService.getApproveProduct(
        userHomeFacility, requisitionProgram, requisition.getTemplate());

    Set<UUID> approverOrderableIds = getOrderableIds(approverProductAggregator);
    Set<UUID> userOrderableIds = getOrderableIds(userProductAggregator);

    Set<UUID> archivedOrderableIds = getArchivedOrderableIds(Sets.newHashSet(
        requisition.getFacilityId(), approverFacility.getId(), userHomeFacility.getId()));

    Map<UUID, StockCardSummaryV2Dto> orderableSohMap =
        getOrderableIdSohMap(userHomeFacility.getId());

    return Optional
        .ofNullable(requisition.getAvailableProducts())
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .filter(orderable -> {
          UUID orderableId = orderable.getId();
          if (!archivedOrderableIds.contains(orderableId)
              && approverOrderableIds.contains(orderableId)
              && userOrderableIds.contains(orderableId)
              && orderableSohMap.get(orderableId) != null) {
            Integer soh = orderableSohMap.get(orderableId).getStockOnHand();
            return soh != null && soh > 0;
          }
          return false;
        })
        .map(orderable -> new VersionObjectReferenceDto(
              orderable.getId(), serviceUrl, ORDERABLES, orderable.getVersionNumber())
        ).collect(Collectors.toSet());
  }

  private Set<UUID> getOrderableIds(ApproveProductsAggregator aggregator) {
    return Optional
        .ofNullable(aggregator.getApprovedProductReferences())
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .map(VersionEntityReference::getId)
        .collect(toSet());
  }

  private Set<UUID> getArchivedOrderableIds(Set<UUID> facilityIds) {
    return facilityIds.stream()
        .flatMap(facilityId -> siglusArchiveProductService.searchArchivedProducts(facilityId)
            .stream())
        .map(UUID::fromString)
        .collect(Collectors.toSet());
  }

  private Map<UUID, StockCardSummaryV2Dto> getOrderableIdSohMap(UUID userFacilityId) {
    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
    multiValueMap.set(FACILITY_ID, userFacilityId.toString());
    multiValueMap.set(PROGRAM_ID, ALL_PRODUCTS_PROGRAM_ID.toString());
    multiValueMap.set(RIGHT_NAME, STOCK_CARDS_VIEW);
    Page<StockCardSummaryV2Dto> stockCardSummary = siglusStockCardSummariesService
        .searchStockCardSummaryV2Dtos(multiValueMap, new PageRequest(0, Integer.MAX_VALUE));

    // to map stockCardSummaryV2Dto.getStockOnHand() return null cause NPE
    return stockCardSummary.getContent().stream().collect(Collectors.toMap(
        stockCardSummaryV2Dto -> stockCardSummaryV2Dto.getOrderable().getId(),
        stockCardSummaryV2Dto -> stockCardSummaryV2Dto
    ));
  }

  private org.openlmis.requisition.dto.OrderableDto convertOrderableDto(OrderableDto sourceDto) {
    org.openlmis.requisition.dto.OrderableDto orderableDto = new
        org.openlmis.requisition.dto.OrderableDto();
    BeanUtils.copyProperties(sourceDto, orderableDto);
    orderableDto.setPrograms(sourceDto.getPrograms().stream()
            .map(this::convertProgramOrderable)
            .collect(toSet()));
    return orderableDto;
  }

  private org.openlmis.requisition.dto.ProgramOrderableDto convertProgramOrderable(
      ProgramOrderableDto sourceProgram) {
    org.openlmis.requisition.dto.ProgramOrderableDto program =
        new org.openlmis.requisition.dto.ProgramOrderableDto();
    BeanUtils.copyProperties(sourceProgram, program);
    return program;
  }
}
