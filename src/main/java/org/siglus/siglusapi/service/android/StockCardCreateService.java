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

package org.siglus.siglusapi.service.android;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.service.android.MeService.KEY_PROGRAM_CODE;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.StockEventAdjustmentDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.domain.StockEventProductRequested;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.constraint.stockcard.LotStockConsistentWithExisted;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductConsistentWithAllLots;
import org.siglus.siglusapi.dto.android.constraint.stockcard.ProductMovementConsistentWithExisted;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByLot;
import org.siglus.siglusapi.dto.android.constraint.stockcard.StockOnHandConsistentWithQuantityByProduct;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.group.PerformanceGroup;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.dto.android.sequence.PerformanceSequence;
import org.siglus.siglusapi.repository.StockEventProductRequestedRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusPhysicalInventoryService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class StockCardCreateService {

  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramReferenceDataService programDataService;
  private final StockManagementRepository stockManagementRepository;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final StockEventProductRequestedRepository requestQuantityRepository;
  private final SiglusStockEventsService stockEventsService;
  private final SiglusStockCardSummariesService stockCardSummariesService;
  private final SiglusLotReferenceDataService lotReferenceDataService;
  private final AndroidHelper androidHelper;
  private final SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Transactional
  @Validated(PerformanceSequence.class)
  public void createStockCards(
      @Valid
      @NotEmpty
      @StockOnHandConsistentWithQuantityByProduct
      @StockOnHandConsistentWithQuantityByLot
      @ProductConsistentWithAllLots
      @LotStockConsistentWithExisted(groups = PerformanceGroup.class)
      @ProductMovementConsistentWithExisted(groups = PerformanceGroup.class)
          List<StockCardCreateRequest> requests) {
    FacilityDto facilityDto = getCurrentFacilityInfo();
    deletePhysicalInventoryDraftForAndroidUser(facilityDto.getId());
    Map<String, OrderableDto> allApprovedProducts = getAllApprovedProducts().stream()
        .collect(toMap(BasicOrderableDto::getProductCode, Function.identity()));
    List<ProductMovementKey> existed = stockManagementRepository.getLatestProductMovements(facilityDto.getId())
        .getProductMovements().stream().map(ProductMovement::getProductMovementKey).collect(toList());
    requests.stream()
        .filter(r -> !existed.contains(r.getProductMovementKey()))
        .collect(groupingBy(StockCardCreateRequest::getRecordedAt))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> createStockEvent(entry.getValue(), facilityDto, allApprovedProducts));
  }

  public List<OrderableDto> getAllApprovedProducts() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return programsHelper
        .findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private void createStockEvent(List<StockCardCreateRequest> requests, FacilityDto facilityDto,
      Map<String, OrderableDto> allApprovedProducts) {
    String signature = requests.stream().filter(r -> r.getSignature() != null)
        .findFirst().map(StockCardCreateRequest::getSignature).orElse(null);
    buildStockEventForProductMovement(requests, facilityDto, allApprovedProducts, signature);
    buildStockEventForLotMovement(requests, facilityDto, allApprovedProducts, signature);
  }

  private void buildStockEventForProductMovement(List<StockCardCreateRequest> requests,
      FacilityDto facilityDto, Map<String, OrderableDto> allApprovedProducts,
      String signature) {
    //kit && no stock
    buildStockEvent(true, requests, facilityDto, allApprovedProducts, signature);
  }

  private void buildStockEventForLotMovement(List<StockCardCreateRequest> requests,
      FacilityDto facilityDto, Map<String, OrderableDto> allApprovedProducts,
      String signature) {
    buildStockEvent(false, requests, facilityDto, allApprovedProducts, signature);
  }

  private void buildStockEvent(boolean isProductMovement, List<StockCardCreateRequest> requests,
      FacilityDto facilityDto, Map<String, OrderableDto> allApprovedProducts,
      String signature) {
    List<StockCardCreateRequest> requestMovement = requests.stream()
        .filter(request -> request.getLotEvents().isEmpty() == isProductMovement).collect(toList());
    if (requestMovement.isEmpty()) {
      return;
    }
    StockEventDto stockEvent = buildStockEventDto(facilityDto, signature, requestMovement, allApprovedProducts);
    Map<UUID, UUID> programToStockEventIds = stockEventsService.createStockEventForNoDraftAllProducts(stockEvent);
    dealWithIssue(requestMovement, programToStockEventIds, allApprovedProducts);
  }

  private void dealWithIssue(List<StockCardCreateRequest> requests, Map<UUID, UUID> programToStockEventIds,
      Map<String, OrderableDto> allApprovedProducts) {
    if (programToStockEventIds.isEmpty()) {
      return;
    }
    MovementType type = requests.stream().findFirst()
        .map(StockCardCreateRequest::getType)
        .map(MovementType::valueOf)
        .orElse(null);
    if (type == MovementType.ISSUE) {
      List<StockEventProductRequested> requestQuantities = requests.stream()
          .filter(stockCardCreateRequest -> stockCardCreateRequest.getRequested() != null)
          .map(request -> buildProductRequest(request, programToStockEventIds, allApprovedProducts))
          .collect(toList());
      requestQuantityRepository.save(requestQuantities);
    }
  }

  private StockEventDto buildStockEventDto(FacilityDto facilityDto, String signature,
      List<StockCardCreateRequest> requests,
      Map<String, OrderableDto> codeOrderableToMap) {
    StockEventDto eventDto = StockEventDto.builder()
        .facilityId(facilityDto.getId())
        .signature(signature)
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .build();
    eventDto.setLineItems(requests.stream()
        .map(request -> buildEventItems(request, codeOrderableToMap.get(request.getProductCode())))
        .flatMap(Collection::stream)
        .collect(toList()));
    return eventDto;
  }

  private StockEventProductRequested buildProductRequest(StockCardCreateRequest request,
      Map<UUID, UUID> programToStockEventIds,
      Map<String, OrderableDto> allApprovedProducts) {
    OrderableDto orderableDto = allApprovedProducts.get(request.getProductCode());
    UUID programId = getProgramId(orderableDto);
    return StockEventProductRequested.builder()
        .orderableId(orderableDto.getId())
        .stockeventId(programToStockEventIds.get(programId))
        .requestedQuantity(request.getRequested())
        .build();
  }

  private List<StockEventLineItemDto> buildEventItems(StockCardCreateRequest request,
      OrderableDto product) {
    MovementType type = MovementType.valueOf(request.getType());
    List<StockCardLotEventRequest> lotEventRequests = request.getLotEvents();
    LocalDate occurredDate = request.getOccurredDate();
    Instant createdAt = request.getRecordedAt();
    if (lotEventRequests.isEmpty()) {
      //kit product && no stock
      return singletonList(buildEventItem(type, occurredDate, createdAt, request, product));
    }
    return lotEventRequests.stream()
        .map(lot -> buildEventItem(type, occurredDate, createdAt, lot, product))
        .collect(toList());
  }

  private StockEventLineItemDto buildEventItem(MovementType type, LocalDate occurredDate, Instant createdAt,
      StockCardAdjustment adjustment, OrderableDto product) {
    StockEventLineItemDto stockEventLineItem = new StockEventLineItemDto();
    stockEventLineItem.setOrderableId(product.getId());
    UUID programId = getProgramId(product);
    stockEventLineItem.setProgramId(programId);
    Map<String, String> extraData;
    if (adjustment instanceof StockCardLotEventRequest) {
      StockCardLotEventRequest lotEvent = (StockCardLotEventRequest) adjustment;
      extraData = ImmutableMap.of(
          "lotCode", lotEvent.getLotCode(),
          "expirationDate", lotEvent.getExpirationDate().toString(),
          "originEventTime", createdAt.toString()
      );
    } else {
      extraData = ImmutableMap.of(
          "lotCode", "",
          "expirationDate", "",
          "originEventTime", createdAt.toString()
      );
    }
    stockEventLineItem.setExtraData(extraData);
    stockEventLineItem.setOccurredDate(occurredDate);
    Integer quantity = Math.abs(adjustment.getQuantity());
    if (type == MovementType.PHYSICAL_INVENTORY) {
      Integer stockOnHand = adjustment.getStockOnHand();
      stockEventLineItem.setQuantity(stockOnHand);
    } else {
      stockEventLineItem.setQuantity(quantity);
    }
    stockEventLineItem.setDocumentationNo(adjustment.getDocumentationNo());
    String reasonName = adjustment.getReasonName();
    stockEventLineItem.setSourceId(type.getSourceId(programId, reasonName));
    stockEventLineItem.setDestinationId(type.getDestinationId(programId, reasonName));
    stockEventLineItem.setReasonId(type.getAdjustmentReasonId(programId, reasonName));
    stockEventLineItem.setStockAdjustments(getStockAdjustments(type, reasonName, quantity, programId));
    return stockEventLineItem;
  }

  private List<OrderableDto> getProgramProducts(UUID homeFacilityId,
      ProgramDto program) {
    return approvedProductDataService
        .getApprovedProducts(homeFacilityId, program.getId(), emptyList()).stream()
        .map(ApprovedProductDto::getOrderable)
        .map(orderable -> {
          orderable.getExtraData().put(KEY_PROGRAM_CODE, program.getCode());
          return orderable;
        })
        .collect(toList());
  }

  private FacilityDto getCurrentFacilityInfo() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return facilityReferenceDataService.getFacilityById(homeFacilityId);
  }

  private UUID getProgramId(OrderableDto orderable) {
    return orderable.getPrograms().stream().findFirst()
        .map(ProgramOrderableDto::getProgramId)
        .orElseThrow(() -> new IllegalArgumentException("program Not Exist for product"));
  }

  private List<StockEventAdjustmentDto> getStockAdjustments(MovementType type, String reasonName,
      Integer quantity, UUID programId) {
    if (type != MovementType.PHYSICAL_INVENTORY || quantity == 0 || "INVENTORY".equalsIgnoreCase(reasonName)) {
      return Collections.emptyList();
    }
    StockEventAdjustmentDto stockEventAdjustmentDto = new StockEventAdjustmentDto();
    stockEventAdjustmentDto.setReasonId(type.getInventoryReasonId(programId, reasonName));
    stockEventAdjustmentDto.setQuantity(quantity);
    return singletonList(stockEventAdjustmentDto);
  }

  private void deletePhysicalInventoryDraftForAndroidUser(UUID facilityId) {
    if (androidHelper.isAndroid()) {
      siglusPhysicalInventoryService.deletePhysicalInventoryForAllProducts(facilityId);
    }
  }
}
