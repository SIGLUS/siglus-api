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
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.siglus.siglusapi.constant.FieldConstants.TRADE_ITEM;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ProgramDto;
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
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.RequestQuantity;
import org.siglus.siglusapi.dto.ProductMovementDto;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
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
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
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
    PHYSICAL_INVENTORY, RECEIVE, ISSUE, ADJUSTMENT, UNPACK_KIT;

    public static MovementType fromString(String type) {
      for (MovementType movementType : values()) {
        if (equalsIgnoreCase(type, movementType.name())) {
          return movementType;
        }
      }
      return null;
    }
  }

  static final String KEY_PROGRAM_CODE = "programCode";
  static final String KIT_LOT = "kitLot";
  static final String TRADE_ITEM_ID = "tradeItemId";

  static final Map<String, String> mapSources = ImmutableMap.of(
      "DISTRICT_DDM", "District(DDM)",
      "PROVINCE_DPM", "Province(DPM)");
  private static final Map<String, String> mapDestination = new HashMap<>();

  static {
    mapDestination.put("PUB_PHARMACY", "Farmácia");
    mapDestination.put("MATERNITY", "Maternidade");
    mapDestination.put("GENERAL_WARD", "Medicina Geral");
    mapDestination.put("ACC_EMERGENCY", "Banco de Socorro");
    mapDestination.put("MOBILE_UNIT", "Unidade Movel");
    mapDestination.put("LABORATORY", "Laboratorio");
    mapDestination.put("PNCTL", "PNCTL");
    mapDestination.put("PAV", "PAV");
    mapDestination.put("DENTAL_WARD", "Medicina Dentaria");
  }

  static final Map<String, String> mapPhysicalInventoryReason = ImmutableMap.of(
      "INVENTORY_POSITIVE", "Correção Positiva",
      "INVENTORY_NEGATIVE", "Correção Negativa");


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
  private final SiglusStockEventsService stockEventsService;
  private final RequestQuantityRepository requestQuantityRepository;
  private final SiglusValidSourceDestinationService siglusValidSourceDestinationService;
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
    ).collect(Collectors.toList());
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

  public ProductSyncResponse getFacilityProducts(Instant lastSyncTime) {
    ProductSyncResponse syncResponse = new ProductSyncResponse();
    syncResponse.setLastSyncTime(System.currentTimeMillis());
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<UUID, OrderableDto> allProducts = getAllProducts(homeFacilityId).stream()
        .collect(Collectors.toMap(OrderableDto::getId, Function.identity()));

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
        .collect(Collectors.toList());
    List<ProductResponse> filteredProducts = approvedProducts.stream()
        .filter(p -> filterByLastUpdated(p, lastSyncTime))
        .map(orderable -> mapper.toResponse(orderable, allProducts))
        .collect(Collectors.toList());
    syncResponse.setProducts(filteredProducts);
    return syncResponse;
  }

  public void createStockCards(List<StockCardCreateRequest> requests) {
    FacilityDto facilityDto = getCurrentFacilityInfo();
    List<org.openlmis.requisition.dto.OrderableDto> orderableDtos = getAllAprovedOrderableDtos();
    Map<String, org.openlmis.requisition.dto.OrderableDto> codeToOrderableDto =
        orderableDtos.stream()
            .collect(Collectors.toMap(org.openlmis.requisition.dto.OrderableDto::getProductCode,
                Function.identity()));
    requests.stream()
        .collect(Collectors.groupingBy(StockCardCreateRequest::getCreatedAt))
        .entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEachOrdered(entry ->
            dealWithStockEvent(entry.getValue(), facilityDto, codeToOrderableDto));
  }

  public Map<String, Map<String, Integer>> getLotOnHand() {
    List<org.openlmis.requisition.dto.OrderableDto> orderableDtos = getAllAprovedOrderableDtos();
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(orderableDtos);
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos = stockCardSummariesService
        .findAllProgramStockSummaries();
    Map<UUID, String> lotIdToLotCode = getLotIdToCode(orderableDtos, stockCardSummaryV2Dtos);
    return getLotsOnHandMap(orderableIdToCode, stockCardSummaryV2Dtos, lotIdToLotCode);
  }

  private Map<UUID, String> getOrderableIdToCode(
      List<org.openlmis.requisition.dto.OrderableDto> orderableDtos) {
    return orderableDtos.stream()
        .collect(Collectors.toMap(
            org.openlmis.requisition.dto.OrderableDto::getId,
            org.openlmis.requisition.dto.OrderableDto::getProductCode));
  }

  private Map<UUID, String> getLotIdToCode(
      List<org.openlmis.requisition.dto.OrderableDto> orderableDtos,
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos) {
    List<UUID> orderableIdsForStockCard = stockCardSummaryV2Dtos.stream()
        .map(stockCardSummaryV2Dto -> stockCardSummaryV2Dto.getOrderable().getId())
        .collect(Collectors.toList());
    List<String> tradeItems = orderableDtos.stream()
        .filter(dto -> orderableIdsForStockCard.contains(dto.getId()))
        .map(dto -> dto.getIdentifiers().get(TRADE_ITEM))
        .collect(Collectors.toList());
    RequestParameters requestParameters = RequestParameters.init();
    requestParameters.set(TRADE_ITEM_ID, tradeItems);
    return lotReferenceDataService.findAllLot(requestParameters)
        .stream()
        .collect(Collectors.toMap(LotDto::getId, LotDto::getLotCode));
  }

  private List<org.openlmis.requisition.dto.OrderableDto> getAllAprovedOrderableDtos() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return programsHelper
        .findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private Map<String, Map<String, Integer>> getLotsOnHandMap(Map<UUID, String> orderableIdToCode,
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos, Map<UUID, String> lotIdToLotCode) {
    return stockCardSummaryV2Dtos.stream()
        .collect(Collectors.toMap(dto -> orderableIdToCode.get(dto.getOrderable().getId()),
            dto -> dto.getCanFulfillForMe()
                .stream()
                .collect(Collectors.toMap(fulfillDto -> fulfillDto.getLot() == null
                        ? KIT_LOT : lotIdToLotCode.get(fulfillDto.getLot().getId()),
                    CanFulfillForMeEntryDto::getStockOnHand))));
  }

  private void dealWithStockEvent(List<StockCardCreateRequest> requests, FacilityDto facilityDto,
      Map<String, org.openlmis.requisition.dto.OrderableDto> codeOrderableToMap) {
    StockCardCreateRequest firstStockCard = requests.get(0);
    MovementType type = MovementType.fromString(firstStockCard.getType());
    getReasonNameAndOrganization(facilityDto, type);
    StockEventDto stockEvent = buildStockEvent(facilityDto, firstStockCard, requests,
        codeOrderableToMap);
    UUID stockEventId = stockEventsService.createStockEventForAllProducts(stockEvent);
    if (type == MovementType.ISSUE) {
      List<RequestQuantity> requestQuantities = requests.stream()
          .filter(stockCardCreateRequest -> stockCardCreateRequest.getRequested() != null)
          .map(request -> buildProductRequest(request, stockEventId, codeOrderableToMap))
          .collect(Collectors.toList());
      requestQuantityRepository.save(requestQuantities);
    }
  }

  private RequestQuantity buildProductRequest(StockCardCreateRequest request, UUID stockEventId,
      Map<String, org.openlmis.requisition.dto.OrderableDto> codeToOrderableDtoMap) {
    return RequestQuantity.builder()
        .orderableId(codeToOrderableDtoMap.get(request.getProductCode()).getId())
        .stockeventId(stockEventId)
        .requestQuantity(request.getRequested())
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
          .collect(Collectors.toMap(ValidReasonAssignmentDto::getProgramId,
              reasonDto -> ImmutableMap.of(reasonDto.getReason().getName(), reasonDto.getId())));
    }
  }

  private void setDestinationNameToId(FacilityDto facilityDto, MovementType type) {
    if (type == MovementType.ISSUE && programToDestinationNameToId.isEmpty()) {
      programToDestinationNameToId = siglusValidSourceDestinationService
          .findDestinationsForAllProducts(facilityDto.getId())
          .stream()
          .collect(Collectors.toMap(ValidSourceDestinationDto::getProgramId,
              destination -> ImmutableMap.of(destination.getName(), destination.getId())));
    }
  }

  private StockEventDto buildStockEvent(FacilityDto facilityDto,
      StockCardCreateRequest firstStockCard,
      List<StockCardCreateRequest> requests,
      Map<String, org.openlmis.requisition.dto.OrderableDto> codeOrderableToMap) {
    StockEventDto eventDto = StockEventDto.builder()
        .facilityId(facilityDto.getId())
        .signature(firstStockCard.getSignature())
        .programId(ALL_PRODUCTS_PROGRAM_ID)
        .build();
    // TODO: kit
    eventDto.setLineItems(requests.stream()
        .map(request -> getStockEventLineItemDto(request, codeOrderableToMap))
        .flatMap(Collection::stream)
        .collect(Collectors.toList()));
    return eventDto;
  }

  private List<StockEventLineItemDto> getStockEventLineItemDto(StockCardCreateRequest request,
      Map<String, org.openlmis.requisition.dto.OrderableDto> codeOrderableToMap) {
    return request.getLotEvents().stream()
        .map(lotItem -> {
          MovementType type = MovementType.fromString(request.getType());
          StockEventLineItemDto stockEventLineItemDto = new StockEventLineItemDto();
          org.openlmis.requisition.dto.OrderableDto orderableDto =
              codeOrderableToMap.get(request.getProductCode());
          stockEventLineItemDto
              .setOrderableId(orderableDto.getId());
          UUID programId = orderableDto.getPrograms().stream().findFirst().get().getProgramId();
          stockEventLineItemDto
              .setProgramId(programId);
          Map<String, String> map = ImmutableMap.of("lotCode", lotItem.getLotNumber(),
              "expirationDate", lotItem.getExpirationDate().toString());
          stockEventLineItemDto.setExtraData(map);
          stockEventLineItemDto.setQuantity(lotItem.getQuantity());
          stockEventLineItemDto.setOccurredDate(lotItem.getOccurredDate());
          stockEventLineItemDto.setDocumentationNo(lotItem.getDocumentationNo());
          stockEventLineItemDto.setDestinationId(getDestinationId(type, lotItem, programId));
          stockEventLineItemDto.setStockAdjustments(getStockAdjustments(type, lotItem, programId));
          return stockEventLineItemDto;
        })
        .collect(Collectors.toList());
  }

  private UUID getDestinationId(MovementType type, StockCardLotEventRequest lotItem,
      UUID programId) {
    if (type == MovementType.ISSUE) {
      return programToDestinationNameToId.get(programId)
          .get(mapDestination.get(lotItem.getReasonName()));
    }
    return null;
  }

  private List<StockEventAdjustmentDto> getStockAdjustments(MovementType type,
      StockCardLotEventRequest lotItem, UUID programId) {
    if (type == MovementType.PHYSICAL_INVENTORY && lotItem.getQuantity() != 0) {
      StockEventAdjustmentDto stockEventAdjustmentDto = new StockEventAdjustmentDto();
      stockEventAdjustmentDto.setReasonId(
          programToReasonNameToId.get(programId)
              .get(mapPhysicalInventoryReason.get(lotItem.getReasonName())));
      stockEventAdjustmentDto.setQuantity(lotItem.getQuantity());
      return Arrays.asList(stockEventAdjustmentDto);
    }
    return Collections.emptyList();
  }

  @Transactional
  public List<ProductMovementDto> getProductMovements(String startTime, String endTime) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return stockCardLineItemService.findProductMovements(facilityId, startTime, endTime);
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
        .collect(Collectors.toList());
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

}
