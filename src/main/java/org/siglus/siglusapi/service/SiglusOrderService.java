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

import static java.util.stream.Collectors.toSet;
import static org.openlmis.requisition.web.ResourceNames.ORDERABLES;
import static org.siglus.siglusapi.constant.FieldConstants.FACILITY_ID;
import static org.siglus.siglusapi.constant.FieldConstants.PROGRAM_ID;
import static org.siglus.siglusapi.constant.FieldConstants.RIGHT_NAME;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.fulfillment.util.AuthenticationHelper;
import org.openlmis.fulfillment.web.OrderController;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ApproveProductsAggregator;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.requisition.web.RequisitionController;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.web.SiglusStockCardSummariesSiglusController;
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
  private SiglusStockCardSummariesSiglusController siglusStockCardSummariesSiglusController;

  @Value("${service.url}")
  private String serviceUrl;

  public SiglusOrderDto searchOrderById(UUID orderId) {
    OrderDto orderDto = orderController.getOrder(orderId, Collections.emptySet());

    return SiglusOrderDto.builder()
        .order(orderDto)
        .availableProducts(getAllUserAvailableProductAggregator(orderDto)).build();
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
    ApproveProductsAggregator approverProductAggregator = requisitionService.getApproveProduct(
        approverFacility, requisitionProgram, requisition.getTemplate());
    ApproveProductsAggregator userProductAggregator = requisitionService.getApproveProduct(
        userHomeFacility, requisitionProgram, requisition.getTemplate());

    Set<UUID> approverOrderableIds = getOrderableIds(approverProductAggregator);
    Set<UUID> userOrderableIds = getOrderableIds(userProductAggregator);

    Set<UUID> archivedOrderableIds = getArchivedOrderableIds(Sets.newHashSet(
        requisition.getFacilityId(), approverFacility.getId(), userHomeFacility.getId()));

    Map<UUID, Integer> orderableSohMap = getOrderableIdSohMap(userHomeFacility.getId());

    return Optional
        .ofNullable(requisition.getAvailableProducts())
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .filter(orderable -> {
          UUID orderableId = orderable.getId();
          if (!archivedOrderableIds.contains(orderableId)
              && approverOrderableIds.contains(orderableId)
              && userOrderableIds.contains(orderableId)) {
            Integer soh = orderableSohMap.get(orderableId);
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

  private Map<UUID, Integer> getOrderableIdSohMap(UUID userFacilityId) {
    MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
    multiValueMap.set(FACILITY_ID, userFacilityId.toString());
    multiValueMap.set(PROGRAM_ID, ALL_PRODUCTS_PROGRAM_ID.toString());
    multiValueMap.set(RIGHT_NAME, "STOCK_CARDS_VIEW");
    Page<StockCardSummaryV2Dto> stockCardSummary = siglusStockCardSummariesSiglusController
        .searchStockCardSummaries(multiValueMap, new PageRequest(0, Integer.MAX_VALUE));

    // to map stockCardSummaryV2Dto.getStockOnHand() return null cause NPE
    return stockCardSummary.getContent().stream().collect(Collectors.toMap(
        stockCardSummaryV2Dto -> stockCardSummaryV2Dto.getOrderable().getId(),
        stockCardSummaryV2Dto ->
            stockCardSummaryV2Dto.getStockOnHand() == null ? 0 :
                stockCardSummaryV2Dto.getStockOnHand()
    ));
  }
}
