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
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;
import static org.siglus.siglusapi.service.android.SiglusMeService.KEY_PROGRAM_CODE;
import static org.siglus.siglusapi.service.android.SiglusMeService.TRADE_ITEM_ID;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.StockEventAdjustmentDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.RequestParameters;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.domain.StockEventProductRequested;
import org.siglus.siglusapi.dto.android.ProductMovement;
import org.siglus.siglusapi.dto.android.ProductMovementKey;
import org.siglus.siglusapi.dto.android.StockOnHand;
import org.siglus.siglusapi.dto.android.enumeration.MovementType;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.LotsOnHandResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.dto.android.response.SiglusLotResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;
import org.siglus.siglusapi.repository.StockEventProductRequestedRepository;
import org.siglus.siglusapi.repository.StockManagementRepository;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.service.android.context.CreateStockCardContextHolder;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class StockCardSyncService {

  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramReferenceDataService programDataService;
  private final StockManagementRepository stockManagementRepository;
  private final CreateStockCardContextHolder createStockCardContextHolder;
  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final StockEventProductRequestedRepository requestQuantityRepository;
  private final SiglusStockEventsService stockEventsService;
  private final SiglusStockCardSummariesService stockCardSummariesService;
  private final SiglusStockCardLineItemService stockCardLineItemService;
  private final SiglusLotReferenceDataService lotReferenceDataService;

  public FacilityProductMovementsResponse getProductMovements(String startTime, String endTime) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    List<org.openlmis.requisition.dto.OrderableDto> orderableDtos = getAllApprovedProducts();
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(orderableDtos);
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos = stockCardSummariesService.findAllProgramStockSummaries();
    Map<UUID, SiglusLotResponse> siglusLotDtoByLotId = getSiglusLotDtoByLotId(orderableDtos, stockCardSummaryV2Dtos);
    Map<UUID, List<LotsOnHandResponse>> lotsOnHandDtosByOrderableId = getLotsOnHandResponsesMap(stockCardSummaryV2Dtos,
        siglusLotDtoByLotId);
    Map<UUID, List<SiglusStockMovementItemResponse>> productMovementResponsesByOrderableId = stockCardLineItemService
        .getStockMovementByOrderableId(facilityId, startTime, endTime, siglusLotDtoByLotId);
    List<ProductMovementResponse> productMovementResponses = lotsOnHandDtosByOrderableId.entrySet().stream()
        .map(entry -> ProductMovementResponse.builder()
            .stockMovementItems(productMovementResponsesByOrderableId.get(entry.getKey()) == null ? emptyList()
                : productMovementResponsesByOrderableId.get(entry.getKey()))
            .productCode(orderableIdToCode.get(entry.getKey()))
            .lotsOnHand(judgeReturnLotsOnHandDtos(entry.getValue()))
            .stockOnHand(calculateStockOnHandByLot(entry.getValue()))
            .build())
        .collect(toList());
    productMovementResponses.forEach(m -> m.getStockMovementItems().sort(Comparator
        .comparing(SiglusStockMovementItemResponse::getOccurredDate)
        .thenComparing(SiglusStockMovementItemResponse::getProcessedDate)));
    return FacilityProductMovementsResponse.builder().productMovements(productMovementResponses).build();
  }

  @Transactional
  public void createStockCards(List<StockCardCreateRequest> requests) {
    FacilityDto facilityDto = getCurrentFacilityInfo();
    createStockCardContextHolder.initContext(facilityDto);
    try {
      Map<String, OrderableDto> allApprovedProducts = getAllApprovedProducts().stream()
          .collect(toMap(BasicOrderableDto::getProductCode, Function.identity()));
      List<ProductMovementKey> existed = stockManagementRepository.getLatestProductMovements(facilityDto.getId())
          .stream().map(ProductMovement::getProductMovementKey).collect(toList());
      requests.stream()
          .filter(r -> !existed.contains(r.getProductMovementKey()))
          .collect(groupingBy(StockCardCreateRequest::getRecordedAt))
          .entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> createStockEvent(entry.getValue(), facilityDto, allApprovedProducts));
    } finally {
      CreateStockCardContextHolder.clearContext();
    }
  }

  public List<ProductMovement> getLatestProductMovements() {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return stockManagementRepository.getLatestProductMovements(facilityId);
  }

  public StockOnHand getLatestStockOnHand() {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return stockManagementRepository.getStockOnHand(facilityId);
  }

  private void createStockEvent(List<StockCardCreateRequest> requests, FacilityDto facilityDto,
      Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts) {
    String signature = requests.stream().filter(r -> r.getSignature() != null)
        .findFirst().map(StockCardCreateRequest::getSignature).orElse(null);
    buildStockEventForProductMovement(requests, facilityDto, allApprovedProducts, signature);
    buildStockEventForLotMovement(requests, facilityDto, allApprovedProducts, signature);
  }

  private void buildStockEventForProductMovement(List<StockCardCreateRequest> requests,
      FacilityDto facilityDto, Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts,
      String signature) {
    //kit && no stock
    buildStockEvent(true, requests, facilityDto, allApprovedProducts, signature);
  }

  private void buildStockEventForLotMovement(List<StockCardCreateRequest> requests,
      FacilityDto facilityDto, Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts,
      String signature) {
    buildStockEvent(false, requests, facilityDto, allApprovedProducts, signature);
  }

  private void buildStockEvent(boolean isProductMovement, List<StockCardCreateRequest> requests,
      FacilityDto facilityDto, Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts,
      String signature) {
    List<StockCardCreateRequest> requestMovement = requests.stream()
        .filter(request -> request.getLotEvents().isEmpty() == isProductMovement).collect(toList());
    if (!requestMovement.isEmpty()) {
      StockEventDto stockEvent = buildStockEventDto(facilityDto, signature, requestMovement, allApprovedProducts);
      Map<UUID, UUID> programToStockEventIds = stockEventsService.createStockEventForNoDraftAllProducts(stockEvent);
      dealWithIssue(requestMovement, programToStockEventIds, allApprovedProducts);
    }
  }

  private void dealWithIssue(List<StockCardCreateRequest> requests, Map<UUID, UUID> programToStockEventIds,
      Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts) {
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
      Map<String, org.openlmis.requisition.dto.OrderableDto> codeOrderableToMap) {
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
      Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts) {
    org.openlmis.requisition.dto.OrderableDto orderableDto = allApprovedProducts.get(request.getProductCode());
    UUID programId = getProgramId(orderableDto);
    return StockEventProductRequested.builder()
        .orderableId(orderableDto.getId())
        .stockeventId(programToStockEventIds.get(programId))
        .requestedQuantity(request.getRequested())
        .build();
  }

  private List<StockEventLineItemDto> buildEventItems(StockCardCreateRequest request,
      org.openlmis.requisition.dto.OrderableDto product) {
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
      StockCardAdjustment adjustment, org.openlmis.requisition.dto.OrderableDto product) {
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

  private List<org.openlmis.requisition.dto.OrderableDto> getAllApprovedProducts() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return programsHelper
        .findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private List<org.openlmis.requisition.dto.OrderableDto> getProgramProducts(UUID homeFacilityId,
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

  private UUID getProgramId(org.openlmis.requisition.dto.OrderableDto orderable) {
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

  private Map<UUID, String> getOrderableIdToCode(List<org.openlmis.requisition.dto.OrderableDto> orderableDtos) {
    return orderableDtos.stream()
        .collect(toMap(org.openlmis.requisition.dto.OrderableDto::getId,
            org.openlmis.requisition.dto.OrderableDto::getProductCode));
  }

  private Integer calculateStockOnHandByLot(List<LotsOnHandResponse> lotsOnHandResponses) {
    return lotsOnHandResponses.stream().mapToInt(LotsOnHandResponse::getQuantityOnHand).sum();
  }

  private List<LotsOnHandResponse> judgeReturnLotsOnHandDtos(List<LotsOnHandResponse> lotsOnHandResponses) {
    for (LotsOnHandResponse lotsOnHand : lotsOnHandResponses) {
      if (lotsOnHand.getLot() != null) {
        return lotsOnHandResponses;
      }
    }
    return emptyList();
  }

  private Map<UUID, SiglusLotResponse> getSiglusLotDtoByLotId(
      List<org.openlmis.requisition.dto.OrderableDto> orderableDtos,
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos) {
    return getLotList(stockCardSummaryV2Dtos, orderableDtos).stream()
        .collect(Collectors.toMap(LotDto::getId,
            t -> SiglusLotResponse.builder().lotCode(t.getLotCode()).expirationDate(t.getExpirationDate())
                .valid(t.isActive()).build()));
  }

  private Map<UUID, List<LotsOnHandResponse>> getLotsOnHandResponsesMap(
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      Map<UUID, SiglusLotResponse> siglusLotDtoByLotId) {
    return stockCardSummaryV2Dtos.stream()
        .collect(Collectors
            .toMap(dto -> dto.getOrderable().getId(), dto -> getLotsOnHandResponses(dto, siglusLotDtoByLotId)));
  }

  private List<LotDto> getLotList(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      Collection<org.openlmis.requisition.dto.OrderableDto> approvedProducts) {
    List<UUID> orderableIdsForStockCard = stockCardSummaryV2Dtos.stream()
        .map(stockCardSummaryV2Dto -> stockCardSummaryV2Dto.getOrderable().getId())
        .collect(Collectors.toList());
    List<String> tradeItems = approvedProducts.stream()
        .filter(dto -> orderableIdsForStockCard.contains(dto.getId()))
        .map(dto -> dto.getIdentifiers().get(TRADE_ITEM))
        .collect(Collectors.toList());
    RequestParameters requestParameters = RequestParameters.init();
    requestParameters.set(TRADE_ITEM_ID, tradeItems);
    return lotReferenceDataService.findAllLot(requestParameters);
  }

  private List<LotsOnHandResponse> getLotsOnHandResponses(StockCardSummaryV2Dto dto,
      Map<UUID, SiglusLotResponse> sigluslotDtoByLotId) {
    return dto.getCanFulfillForMe().stream()
        .map(fulfillDto -> convertLotsOnHandResponse(fulfillDto, sigluslotDtoByLotId))
        .collect(Collectors.toList());
  }

  private LotsOnHandResponse convertLotsOnHandResponse(CanFulfillForMeEntryDto fulfillDto,
      Map<UUID, SiglusLotResponse> sigluslotResponseByLotId) {
    return LotsOnHandResponse.builder().quantityOnHand(fulfillDto.getStockOnHand())
        .lot(fulfillDto.getLot() == null ? null : sigluslotResponseByLotId.get(fulfillDto.getLot().getId()))
        .effectiveDate(fulfillDto.getOccurredDate()).build();
  }

}
