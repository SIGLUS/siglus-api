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
import static org.siglus.common.dto.referencedata.OrderableDto.TRADE_ITEM;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ProgramOrderableDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.StockEventAdjustmentDto;
import org.openlmis.stockmanagement.dto.StockEventDto;
import org.openlmis.stockmanagement.dto.StockEventLineItemDto;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.openlmis.stockmanagement.dto.ValidSourceDestinationDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.CanFulfillForMeEntryDto;
import org.openlmis.stockmanagement.web.stockcardsummariesv2.StockCardSummaryV2Dto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.MetadataDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.QueryOrderableSearchParams;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.RequestParameters;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.StockEventExtension;
import org.siglus.siglusapi.dto.FcProductMovementsDto;
import org.siglus.siglusapi.dto.LotsOnHandDto;
import org.siglus.siglusapi.dto.ProductMovementDto;
import org.siglus.siglusapi.dto.SiglusLotDto;
import org.siglus.siglusapi.dto.android.LotStockOnHand;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.ProgramResponse;
import org.siglus.siglusapi.repository.RequestQuantityRepository;
import org.siglus.siglusapi.repository.android.AppInfoRepository;
import org.siglus.siglusapi.repository.android.FacilityCmmsRepository;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusStockCardLineItemService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.web.SiglusStockEventsController;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods"})
public class SiglusMeService {

  public enum MovementType {
    PHYSICAL_INVENTORY() {
      @Override
      UUID getReasonId(CreateStockCardContext context, UUID programId, String reason) {
        return null;
      }
    },
    RECEIVE() {
      @Override
      UUID getSourceId(CreateStockCardContext context, UUID programId, String source) {
        return context.findSourceId(programId, source);
      }
    },
    ISSUE() {
      @Override
      UUID getDestinationId(CreateStockCardContext context, UUID programId, String destination) {
        return context.findDestinationId(programId, destination);
      }
    },
    ADJUSTMENT,
    UNPACK_KIT;

    UUID getSourceId(CreateStockCardContext context, UUID programId, String source) {
      return null;
    }

    UUID getDestinationId(CreateStockCardContext context, UUID programId, String destination) {
      return null;
    }

    UUID getReasonId(CreateStockCardContext context, UUID programId, String reason) {
      return context.findSourceId(programId, reason);
    }

  }

  @RequiredArgsConstructor
  @Getter
  public enum Source {
    DISTRICT_DDM("District(DDM)"),
    INTERMEDIATE_WAREHOUSE("Armazém Intermediário)"),
    PROVINCE_DPM("Province(DPM)");

    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }

  }

  @RequiredArgsConstructor
  @Getter
  public enum Destination {
    ACC_EMERGENCY("Banco de Socorro"),
    DENTAL_WARD("Medicina Dentaria"),
    GENERAL_WARD("Medicina Geral"),
    LABORATORY("Laboratorio"),
    MATERNITY("Maternidade"),
    MOBILE_UNIT("Unidade Movel"),
    PAV("PAV"),
    PNCTL("PNCTL"),
    PUB_PHARMACY("Farmácia");

    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum AdjustmentReason {
    CUSTOMER_RETURN("Devoluções de clientes (US e Enfermarias Dependentes)"),
    DAMAGED("Danificado na Chegada"),
    DONATION("Doação para o Deposito"),
    EXPIRED_RETURN_FROM_CUSTOMER("Devoluções de Expirados (US e Enfermarias de Dependentes)"),
    EXPIRED_RETURN_TO_SUPPLIER("Devolvidos ao Fornecedor por terem Expirados em Quarentena"),
    INVENTORY_NEGATIVE("Correção Negativa"),
    INVENTORY_POSITIVE("Correção Positiva"),
    LOANS_DEPOSIT("Loans made from a health facility deposit"),
    LOANS_RECEIVED("Emprestimo Recebido pela US"),
    PROD_DEFECTIVE("Produto com defeito, movido para quarentena"),
    RETURN_FROM_QUARANTINE("Devoluções da Quarentena"),
    RETURN_TO_DDM("Devolução para o DDM");

    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  static final String KEY_PROGRAM_CODE = "programCode";
  static final String KIT_LOT = "kitLot";
  static final String TRADE_ITEM_ID = "tradeItemId";

  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusArchiveProductService siglusArchiveProductService;
  private final SiglusOrderableService orderableDataService;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramReferenceDataService programDataService;
  private final AppInfoRepository appInfoRepository;
  private final ProductMapper mapper;
  private final FacilityCmmsRepository facilityCmmsRepository;
  private final SiglusStockCardSummariesService stockCardSummariesService;
  private final SiglusLotReferenceDataService lotReferenceDataService;
  private final SiglusStockCardLineItemService stockCardLineItemService;
  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final RequestQuantityRepository requestQuantityRepository;
  private final SiglusValidSourceDestinationService siglusValidSourceDestinationService;
  private final SiglusStockEventsController stockEventsController;
  private Map<UUID, Map<String, UUID>> programToReasonNameToId;
  private Map<UUID, Map<String, UUID>> programToDestinationNameToId;

  public FacilityResponse getCurrentFacility() {
    FacilityDto facilityDto = getCurrentFacilityInfo();
    List<SupportedProgramDto> programs = facilityDto.getSupportedPrograms();
    List<ProgramResponse> programResponses = programs.stream().map(program ->
        ProgramResponse.builder()
            .code(program.getCode())
            .name(program.getName())
            .supportActive(program.isSupportActive())
            .supportStartDate(program.getSupportStartDate())
            .build()
    ).collect(toList());
    return FacilityResponse.builder()
        .code(facilityDto.getCode())
        .name(facilityDto.getName())
        .supportedPrograms(programResponses)
        .build();
  }

  @Transactional
  public void processAppInfo(AppInfo appInfo) {
    AppInfo existAppInfo = appInfoRepository
        .findByFacilityCodeAndUniqueId(appInfo.getFacilityCode(), appInfo.getUniqueId());
    UUID appInfoId = existAppInfo != null ? existAppInfo.getId() : UUID.randomUUID();
    appInfo.setId(appInfoId);
    appInfo.setLastUpdated(Instant.now());
    log.info("process app-info , id: {}", appInfoId);
    appInfoRepository.save(appInfo);
  }

  @Transactional
  public void processHfCmms(List<HfCmmDto> hfCmmDtos) {
    hfCmmDtos.stream().map(this::buildCmm).forEach(this::saveAndUpdateCmm);
  }

  @Transactional
  public void archiveAllProducts(List<String> productCodes) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    siglusArchiveProductService.archiveAllProducts(facilityId, productCodes);
  }

  @Transactional
  public FcProductMovementsDto getProductMovements(String startTime, String endTime) {
    List<ProductMovementDto> productMovementDtos = new ArrayList<>();
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    List<org.openlmis.requisition.dto.OrderableDto> orderableDtos = getAllApprovedProducts();
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(orderableDtos);
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos = stockCardSummariesService
        .findAllProgramStockSummaries();
    Map<UUID, SiglusLotDto> siglusLotDtoByLotId = getSiglusLotDtoByLotId(orderableDtos, stockCardSummaryV2Dtos);
    Map<UUID, List<LotsOnHandDto>> lotsOnHandDtosByOrderableId = getLotsOnHandDtosMap(stockCardSummaryV2Dtos,
        siglusLotDtoByLotId);
    stockCardLineItemService
        .getStockMovementByOrderableId(facilityId, startTime, endTime, siglusLotDtoByLotId)
        .forEach((orderableId, items) ->
            productMovementDtos.add(
                ProductMovementDto.builder().stockMovementItems(items).productCode(orderableIdToCode.get(orderableId))
                    .lotsOnHand(judgeReturnLotsOnHandDtos(lotsOnHandDtosByOrderableId.get(orderableId)))
                    .stockOnHand(calculateStockOnHandByLot(lotsOnHandDtosByOrderableId.get(orderableId)))
                    .build()));
    return FcProductMovementsDto.builder().productMovements(productMovementDtos).build();
  }

  public ProductSyncResponse getFacilityProducts(Instant lastSyncTime) {
    ProductSyncResponse syncResponse = new ProductSyncResponse();
    syncResponse.setLastSyncTime(System.currentTimeMillis());
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<UUID, OrderableDto> allProducts = getAllProducts(homeFacilityId).stream()
        .collect(toMap(OrderableDto::getId, Function.identity()));

    List<OrderableDto> approvedProducts = programsHelper
        .findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .map(orderable -> {
          OrderableDto dto = allProducts.get(orderable.getId());
          dto.getExtraData().put(KEY_PROGRAM_CODE, orderable.getExtraData().get(KEY_PROGRAM_CODE));
          return dto;
        })
        .collect(toList());
    List<ProductResponse> filteredProducts = approvedProducts.stream()
        .filter(p -> filterByLastUpdated(p, lastSyncTime))
        .map(orderable -> mapper.toResponse(orderable, allProducts))
        .collect(toList());
    syncResponse.setProducts(filteredProducts);
    return syncResponse;
  }

  @Transactional
  public void createStockCards(List<StockCardCreateRequest> requests) {
    FacilityDto facilityDto = getCurrentFacilityInfo();
    initCreateStockCardContext(facilityDto.getId());
    try {
      Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts = getAllApprovedProducts().stream()
          .collect(toMap(BasicOrderableDto::getProductCode, Function.identity()));
      requests.stream()
          .collect(groupingBy(StockCardCreateRequest::getCreatedAt))
          .entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(entry -> createStockEvent(entry.getValue(), facilityDto, allApprovedProducts));
    } finally {
      clearCreateStockCardContext();
    }
  }


  public List<LotStockOnHand> getLotStockOnHands() {
    List<StockCardSummaryV2Dto> stockSummaries = stockCardSummariesService
        .findAllProgramStockSummaries();
    List<UUID> productIdsInStock = stockSummaries.stream()
        .map(summary -> summary.getOrderable().getId())
        .collect(toList());
    List<org.openlmis.requisition.dto.OrderableDto> approvedProducts = getAllApprovedProducts();
    Map<String, org.openlmis.requisition.dto.OrderableDto> tradeItemToProduct =
        approvedProducts.stream()
            .filter(product -> productIdsInStock.contains(product.getId()))
            .filter(product -> product.getIdentifiers().containsKey(TRADE_ITEM))
            .collect(
                toMap(product -> product.getIdentifiers().get(TRADE_ITEM), Function.identity()));
    return getLotsList(stockSummaries, approvedProducts).stream()
        .map(lot -> toLotStock(lot, tradeItemToProduct, stockSummaries))
        .collect(toList());
  }

  private List<LotDto> getLotsList(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      List<org.openlmis.requisition.dto.OrderableDto> approvedProducts) {
    List<UUID> orderableIdsForStockCard = stockCardSummaryV2Dtos.stream()
        .map(stockCardSummaryV2Dto -> stockCardSummaryV2Dto.getOrderable().getId())
        .collect(Collectors.toList());
    List<String> tradeItems = approvedProducts.stream()
        .filter(dto -> orderableIdsForStockCard.contains(dto.getId()))
        .map(dto -> dto.getIdentifiers().get(FieldConstants.TRADE_ITEM))
        .collect(Collectors.toList());
    RequestParameters requestParameters = RequestParameters.init();
    requestParameters.set(TRADE_ITEM_ID, tradeItems);
    return lotReferenceDataService.findAllLot(requestParameters);
  }

  private Integer calculateStockOnHandByLot(List<LotsOnHandDto> lotsOnHandDtos) {
    return lotsOnHandDtos.stream().mapToInt(LotsOnHandDto::getQuantityOnHand).sum();
  }

  private List<LotsOnHandDto> judgeReturnLotsOnHandDtos(List<LotsOnHandDto> lotsOnHandDtos) {
    return lotsOnHandDtos.stream().findFirst().get().getLot() == null ? null : lotsOnHandDtos;
  }

  private Map<UUID, SiglusLotDto> getSiglusLotDtoByLotId(
      List<org.openlmis.requisition.dto.OrderableDto> orderableDtos,
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos) {
    return getLotsList(stockCardSummaryV2Dtos, orderableDtos).stream().collect(Collectors.toMap(LotDto::getId,
        t -> SiglusLotDto.builder().lotCode(t.getLotCode()).expirationDate(t.getExpirationDate())
            .valid(t.isActive()).build()));
  }

  private LotStockOnHand toLotStock(LotDto lot, Map<String, ? extends BasicOrderableDto> approvedProducts,
      List<StockCardSummaryV2Dto> stockSummaries) {
    BasicOrderableDto product = approvedProducts.get(lot.getTradeItemId().toString());
    Optional<CanFulfillForMeEntryDto> stockOnHandMap = findStockOnHand(product.getId(),
        lot.getId(), stockSummaries);
    return LotStockOnHand.builder()
        .productId(product.getId()).productCode(product.getProductCode())
        .lotId(lot.getId()).lotCode(lot.getLotCode())
        .stockOnHand(stockOnHandMap.map(CanFulfillForMeEntryDto::getStockOnHand).orElse(null))
        .occurredDate(stockOnHandMap.map(CanFulfillForMeEntryDto::getOccurredDate).orElse(null))
        .build();
  }

  private Map<UUID, String> getOrderableIdToCode(
      List<org.openlmis.requisition.dto.OrderableDto> orderableDtos) {
    return orderableDtos.stream()
        .collect(toMap(org.openlmis.requisition.dto.OrderableDto::getId,
            org.openlmis.requisition.dto.OrderableDto::getProductCode));
  }

  private Map<UUID, List<LotsOnHandDto>> getLotsOnHandDtosMap(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      Map<UUID, SiglusLotDto> siglusLotDtoByLotId) {
    return stockCardSummaryV2Dtos.stream().collect(
        Collectors.toMap(dto -> dto.getOrderable().getId(),
            dto -> getLotsOnHandDtos(dto, siglusLotDtoByLotId)));
  }

  private List<LotsOnHandDto> getLotsOnHandDtos(StockCardSummaryV2Dto dto,
      Map<UUID, SiglusLotDto> sigluslotDtoByLotId) {
    return dto.getCanFulfillForMe()
        .stream()
        .map(fulfillDto -> convertLotsOnHandDto(fulfillDto, sigluslotDtoByLotId))
        .collect(Collectors.toList());
  }

  private LotsOnHandDto convertLotsOnHandDto(CanFulfillForMeEntryDto fulfillDto,
      Map<UUID, SiglusLotDto> sigluslotDtoByLotId) {
    return LotsOnHandDto.builder().quantityOnHand(fulfillDto.getStockOnHand())
        .lot(fulfillDto.getLot() == null ? null : sigluslotDtoByLotId.get(fulfillDto.getLot().getId()))
        .effectiveDate(fulfillDto.getOccurredDate()).build();
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

  private Optional<CanFulfillForMeEntryDto> findStockOnHand(UUID productId, UUID lotId,
      List<StockCardSummaryV2Dto> stockSummaries) {
    for (StockCardSummaryV2Dto summary : stockSummaries) {
      if (!summary.getOrderable().getId().equals(productId)) {
        continue;
      }
      for (CanFulfillForMeEntryDto lot : summary.getCanFulfillForMe()) {
        if (lot.getLot().getId().equals(lotId)) {
          return Optional.of(lot);
        }
      }
    }
    return Optional.empty();
  }

  private void createStockEvent(List<StockCardCreateRequest> requests, FacilityDto facilityDto,
      Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts) {
    MovementType type = requests.stream().findFirst()
        .map(StockCardCreateRequest::getType)
        .map(MovementType::valueOf)
        .orElse(null);
    String signature = requests.stream().findFirst()
        .map(StockCardCreateRequest::getSignature)
        .orElse(null);
    getReasonNameAndOrganization(facilityDto, type);
    StockEventDto stockEvent = buildStockEvent(facilityDto, signature, requests, allApprovedProducts);
    UUID stockEventId = stockEventsController.createStockEvent(stockEvent);
    if (type == MovementType.ISSUE) {
      List<StockEventExtension> requestQuantities = requests.stream()
          .filter(stockCardCreateRequest -> stockCardCreateRequest.getRequested() != null)
          .map(request -> buildProductRequest(request, stockEventId, allApprovedProducts))
          .collect(toList());
      requestQuantityRepository.save(requestQuantities);
    }
  }

  private StockEventExtension buildProductRequest(StockCardCreateRequest request, UUID stockEventId,
      Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts) {
    return StockEventExtension.builder()
        .orderableId(allApprovedProducts.get(request.getProductCode()).getId())
        .stockeventId(stockEventId)
        .requestedQuantity(request.getRequested())
        .build();
  }

  private void getReasonNameAndOrganization(FacilityDto facilityDto, MovementType type) {
    setReasonNameToId(facilityDto, type);
    setDestinationNameToId(facilityDto, type);
    // source
  }

  private void setReasonNameToId(FacilityDto facilityDto, MovementType type) {
    if (type == MovementType.PHYSICAL_INVENTORY && programToReasonNameToId.isEmpty()) {
      programToReasonNameToId = validReasonAssignmentService
          .getValidReasonsForAllProducts(facilityDto.getType().getId(),
              null, ALL_PRODUCTS_PROGRAM_ID)
          .stream()
          .collect(toMap(ValidReasonAssignmentDto::getProgramId,
              reasonDto -> ImmutableMap.of(reasonDto.getReason().getName(), reasonDto.getId())));
    }
  }

  private void setDestinationNameToId(FacilityDto facilityDto, MovementType type) {
    if (type == MovementType.ISSUE && programToDestinationNameToId.isEmpty()) {
      programToDestinationNameToId = siglusValidSourceDestinationService
          .findDestinationsForAllProducts(facilityDto.getId())
          .stream()
          .collect(toMap(ValidSourceDestinationDto::getProgramId,
              destination -> ImmutableMap.of(destination.getName(), destination.getId())));
    }
  }

  private StockEventDto buildStockEvent(FacilityDto facilityDto, String signature,
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

  private List<StockEventLineItemDto> buildEventItems(StockCardCreateRequest request,
      org.openlmis.requisition.dto.OrderableDto product) {
    MovementType type = MovementType.valueOf(request.getType());
    List<StockCardLotEventRequest> lotEventRequests = request.getLotEvents();
    LocalDate occurredDate = request.getOccurredDate();
    Instant createdAt = request.getCreatedAt();
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
    Integer quantity = adjustment.getQuantity();
    stockEventLineItem.setQuantity(quantity);
    stockEventLineItem.setDocumentationNo(adjustment.getDocumentationNo());
    String reasonName = adjustment.getReasonName();
    stockEventLineItem.setSourceId(type.getSourceId(getCreateStockCardContext(), programId, reasonName));
    stockEventLineItem.setDestinationId(type.getDestinationId(getCreateStockCardContext(), programId, reasonName));
    stockEventLineItem.setReasonId(type.getReasonId(getCreateStockCardContext(), programId, reasonName));
    stockEventLineItem.setStockAdjustments(getStockAdjustments(type, reasonName, quantity, programId));
    return stockEventLineItem;
  }

  private UUID getProgramId(org.openlmis.requisition.dto.OrderableDto orderable) {
    return orderable.getPrograms().stream().findFirst()
        .map(ProgramOrderableDto::getProgramId)
        .orElseThrow(() -> new IllegalArgumentException("program Not Exist for product"));
  }

  private List<StockEventAdjustmentDto> getStockAdjustments(MovementType type, String reason,
      Integer quantity, UUID programId) {
    if (type != MovementType.PHYSICAL_INVENTORY || quantity == 0) {
      return Collections.emptyList();
    }
    UUID reasonId = getCreateStockCardContext().findReasonId(programId, reason);
    StockEventAdjustmentDto stockEventAdjustmentDto = new StockEventAdjustmentDto();
    stockEventAdjustmentDto.setReasonId(reasonId);
    stockEventAdjustmentDto.setQuantity(quantity);
    return singletonList(stockEventAdjustmentDto);
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

  private boolean filterByLastUpdated(OrderableDto approvedProduct, Instant lastSyncTime) {
    if (lastSyncTime == null) {
      return true;
    }
    return Optional.of(approvedProduct)
        .map(OrderableDto::getMeta)
        .map(MetadataDto::getLastUpdated)
        .map(ChronoZonedDateTime::toInstant)
        .map(lastUpdated -> !lastUpdated.isBefore(lastSyncTime))
        .orElse(true);
  }

  private List<OrderableDto> getAllProducts(UUID homeFacilityId) {
    QueryOrderableSearchParams params = new QueryOrderableSearchParams(new LinkedMultiValueMap<>());
    Pageable pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, Pagination.NO_PAGINATION);
    return orderableDataService.searchOrderables(params, pageable, homeFacilityId).getContent();
  }

  private HfCmm buildCmm(HfCmmDto hfCmmDto) {
    return HfCmm.builder()
        .facilityCode(getCurrentFacilityInfo().getCode())
        .cmm(hfCmmDto.getCmm())
        .periodEnd(hfCmmDto.getPeriodEnd())
        .periodBegin(hfCmmDto.getPeriodBegin())
        .productCode(hfCmmDto.getProductCode())
        .lastUpdated(Instant.now())
        .build();
  }

  private void saveAndUpdateCmm(HfCmm toBeUpdatedHfCmm) {
    HfCmm hfCmm = facilityCmmsRepository
        .findByFacilityCodeAndProductCodeAndPeriodBeginAndPeriodEnd(
            toBeUpdatedHfCmm.getFacilityCode(),
            toBeUpdatedHfCmm.getProductCode(),
            toBeUpdatedHfCmm.getPeriodBegin(),
            toBeUpdatedHfCmm.getPeriodEnd());
    UUID cmmId = hfCmm == null ? UUID.randomUUID() : hfCmm.getId();
    toBeUpdatedHfCmm.setId(cmmId);
    log.info("save hf_cmm info , id: {}", cmmId);
    facilityCmmsRepository.save(toBeUpdatedHfCmm);
  }

  private FacilityDto getCurrentFacilityInfo() {
    UserDto userDto = authHelper.getCurrentUser();
    UUID homeFacilityId = userDto.getHomeFacilityId();
    return facilityReferenceDataService.getFacilityById(homeFacilityId);
  }

  private final ThreadLocal<CreateStockCardContext> createStockCardContextHolder = new ThreadLocal<>();

  private void initCreateStockCardContext(UUID facilityId) {
    createStockCardContextHolder.set(new CreateStockCardContext(facilityId));
  }

  private CreateStockCardContext getCreateStockCardContext() {
    return createStockCardContextHolder.get();
  }

  private void clearCreateStockCardContext() {
    createStockCardContextHolder.remove();
  }

  private class CreateStockCardContext {

    private final Map<UUID, Map<String, UUID>> programToReasonNameToId;
    private final Map<UUID, Map<String, UUID>> programToDestinationNameToId;
    private final Map<UUID, Map<String, UUID>> programToSourceNameToId;

    CreateStockCardContext(UUID facilityId) {
      programToReasonNameToId = validReasonAssignmentService
          .getValidReasonsForAllProducts(facilityId, null, ALL_PRODUCTS_PROGRAM_ID)
          .stream()
          .collect(toMap(ValidReasonAssignmentDto::getProgramId,
              reasonDto -> ImmutableMap.of(reasonDto.getReason().getName(), reasonDto.getId())));

      programToDestinationNameToId = siglusValidSourceDestinationService
          .findDestinationsForAllProducts(facilityId)
          .stream()
          .collect(toMap(ValidSourceDestinationDto::getProgramId,
              destination -> ImmutableMap.of(destination.getName(), destination.getId())));

      programToSourceNameToId = siglusValidSourceDestinationService
          .findSourcesForAllProducts(facilityId)
          .stream()
          .collect(toMap(ValidSourceDestinationDto::getProgramId,
              source -> ImmutableMap.of(source.getName(), source.getId())));
    }

    @Nonnull
    UUID findReasonId(UUID programId, String value) {
      return programToDestinationNameToId.get(programId).get(AdjustmentReason.valueOf(value).getValue());
    }

    @Nonnull
    UUID findSourceId(UUID programId, String value) {
      return programToDestinationNameToId.get(programId).get(Source.valueOf(value).getValue());
    }

    @Nonnull
    UUID findDestinationId(UUID programId, String value) {
      return programToDestinationNameToId.get(programId).get(Destination.valueOf(value).getValue());
    }
  }
}
