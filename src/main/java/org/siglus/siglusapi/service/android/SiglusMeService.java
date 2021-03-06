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
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.chrono.ChronoZonedDateTime;
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
import org.openlmis.requisition.domain.requisition.Requisition;
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
import org.siglus.common.dto.referencedata.BaseDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.MetadataDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.QueryOrderableSearchParams;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.RequestParameters;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.domain.StockEventProductRequested;
import org.siglus.siglusapi.dto.android.LotStockOnHand;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardAdjustment;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardLotEventRequest;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.LotsOnHandResponse;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.dto.android.response.ProductResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.ProgramResponse;
import org.siglus.siglusapi.dto.android.response.ReportTypeResponse;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.dto.android.response.SiglusLotResponse;
import org.siglus.siglusapi.dto.android.response.SiglusStockMovementItemResponse;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.StockEventProductRequestedRepository;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusStockCardSummariesService;
import org.siglus.siglusapi.service.SiglusStockEventsService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.SiglusValidSourceDestinationService;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class SiglusMeService {

  public enum MovementType {
    PHYSICAL_INVENTORY() {
      @Override
      UUID getPhysicalReasonId(CreateStockCardContext context, UUID programId, String reason) {
        return context.findReasonId(programId, reason);
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
    ADJUSTMENT() {
      @Override
      UUID getReasonId(CreateStockCardContext context, UUID programId, String reason) {
        return context.findReasonId(programId, reason);
      }
    },
    UNPACK_KIT {
      @Override
      UUID getReasonId(CreateStockCardContext context, UUID programId, String reason) {
        return context.findReasonId(programId, "UNPACK_KIT");
      }
    };

    @SuppressWarnings("unused")
    UUID getSourceId(CreateStockCardContext context, UUID programId, String source) {
      return null;
    }

    @SuppressWarnings("unused")
    UUID getDestinationId(CreateStockCardContext context, UUID programId, String destination) {
      return null;
    }

    @SuppressWarnings("unused")
    UUID getReasonId(CreateStockCardContext context, UUID programId, String reason) {
      return null;
    }

    @SuppressWarnings("unused")
    UUID getPhysicalReasonId(CreateStockCardContext context, UUID programId, String reason) {
      return null;
    }
  }

  @RequiredArgsConstructor
  @Getter
  public enum Source {
    DISTRICT_DDM("District(DDM)"),
    INTERMEDIATE_WAREHOUSE("Armazém Intermediário"),
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
    DENTAL_WARD("Estomatologia"),
    GENERAL_WARD("Medicina Geral"),
    LABORATORY("Laboratorio"),
    MATERNITY("Maternidade"),
    MOBILE_UNIT("Unidade Movel"),
    PAV("PAV"),
    PNCTL("PNCTL"),
    PUB_PHARMACY("Farmácia"),
    UATS("UATS");

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
    LOANS_DEPOSIT("Emprestimo Enviado pela US"),
    LOANS_RECEIVED("Emprestimo Recebido pela US"),
    PROD_DEFECTIVE("Produto com defeito, movido para quarentena"),
    RETURN_FROM_QUARANTINE("Devoluções da Quarentena"),
    RETURN_TO_DDM("Devolução para o DDM"),
    UNPACK_KIT("Unpack Kit");

    private final String value;

    public static String findByValue(String value) {
      return Arrays.stream(values())
          .filter(e -> e.value.equals(value))
          .map(Enum::name)
          .findFirst().orElse(null);
    }
  }

  static final String KEY_PROGRAM_CODE = "programCode";
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
  private final StockEventProductRequestedRepository requestQuantityRepository;
  private final SiglusValidSourceDestinationService siglusValidSourceDestinationService;
  private final SiglusStockEventsService stockEventsService;
  private final AndroidHelper androidHelper;
  private final ReportTypeRepository reportTypeRepository;
  private final SiglusRequisitionRepository requisitionRepository;
  private final AndroidRequisitionService androidRequisitionService;

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
        .isAndroid(androidHelper.isAndroid())
        .supportedReportTypes(findSupportReportTypes(facilityDto.getId(), programs))
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
    initCreateStockCardContext(facilityDto);
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

  public RequisitionResponse getRequisitionResponse(String startDate) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(getAllApprovedProducts());
    return androidRequisitionService
        .getRequisitionResponseByFacilityIdAndDate(facilityId, startDate, orderableIdToCode);
  }

  public List<LotStockOnHand> getLotStockOnHands() {
    List<StockCardSummaryV2Dto> stockSummaries = stockCardSummariesService.findAllProgramStockSummaries();
    Map<UUID, org.openlmis.requisition.dto.OrderableDto> approvedProducts = getAllApprovedProducts().stream()
        .collect(toMap(BasicOrderableDto::getId, Function.identity()));
    Map<UUID, LotDto> storedLots = getLotsList(stockSummaries, approvedProducts.values()).stream()
        .collect(toMap(BaseDto::getId, Function.identity()));
    return stockSummaries.stream()
        .map(StockCardSummaryV2Dto::getCanFulfillForMe)
        .flatMap(Collection::stream)
        .map(summary -> toLotStock(summary, approvedProducts, storedLots))
        .collect(toList());
  }

  public FacilityProductMovementsResponse getProductMovements(String startTime, String endTime) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    List<org.openlmis.requisition.dto.OrderableDto> orderableDtos = getAllApprovedProducts();
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(orderableDtos);
    List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos = stockCardSummariesService
        .findAllProgramStockSummaries();
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
    return FacilityProductMovementsResponse.builder().productMovements(productMovementResponses).build();
  }

  @Transactional
  public void createRequisition(RequisitionCreateRequest request) {
    androidRequisitionService.create(request);
  }

  private List<LotDto> getLotsList(List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      Collection<org.openlmis.requisition.dto.OrderableDto> approvedProducts) {
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
    return getLotsList(stockCardSummaryV2Dtos, orderableDtos).stream()
        .collect(Collectors.toMap(LotDto::getId,
            t -> SiglusLotResponse.builder().lotCode(t.getLotCode()).expirationDate(t.getExpirationDate())
                .valid(t.isActive()).build()));
  }

  private LotStockOnHand toLotStock(CanFulfillForMeEntryDto summary,
      Map<UUID, org.openlmis.requisition.dto.OrderableDto> approvedProducts, Map<UUID, LotDto> storedLots) {
    UUID lotId = null;
    if (summary.getLot() != null) {
      lotId = summary.getLot().getId();
    }
    return LotStockOnHand.builder()
        .productId(summary.getOrderable().getId())
        .productCode(approvedProducts.get(summary.getOrderable().getId()).getProductCode())
        .lotId(lotId)
        .lotCode(lotId == null ? null : storedLots.get(lotId).getLotCode())
        .stockOnHand(summary.getStockOnHand())
        .occurredDate(summary.getOccurredDate())
        .build();
  }

  private Map<UUID, String> getOrderableIdToCode(
      List<org.openlmis.requisition.dto.OrderableDto> orderableDtos) {
    return orderableDtos.stream()
        .collect(toMap(org.openlmis.requisition.dto.OrderableDto::getId,
            org.openlmis.requisition.dto.OrderableDto::getProductCode));
  }

  private Map<UUID, List<LotsOnHandResponse>> getLotsOnHandResponsesMap(
      List<StockCardSummaryV2Dto> stockCardSummaryV2Dtos,
      Map<UUID, SiglusLotResponse> siglusLotDtoByLotId) {
    return stockCardSummaryV2Dtos.stream()
        .collect(Collectors
            .toMap(dto -> dto.getOrderable().getId(), dto -> getLotsOnHandResponses(dto, siglusLotDtoByLotId)));
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

  private List<org.openlmis.requisition.dto.OrderableDto> getAllApprovedProducts() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return programsHelper
        .findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private void createStockEvent(List<StockCardCreateRequest> requests, FacilityDto facilityDto,
      Map<String, org.openlmis.requisition.dto.OrderableDto> allApprovedProducts) {
    String signature = requests.stream().findFirst()
        .map(StockCardCreateRequest::getSignature)
        .orElse(null);
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
    Integer quantity = Math.abs(adjustment.getQuantity());
    if (type == MovementType.PHYSICAL_INVENTORY) {
      Integer stockOnHand = adjustment.getStockOnHand();
      stockEventLineItem.setQuantity(stockOnHand);
    } else {
      stockEventLineItem.setQuantity(quantity);
    }
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

  private List<StockEventAdjustmentDto> getStockAdjustments(MovementType type, String reasonName,
      Integer quantity, UUID programId) {
    if (type != MovementType.PHYSICAL_INVENTORY || quantity == 0
        || reasonName.equalsIgnoreCase("INVENTORY")) {
      return Collections.emptyList();
    }
    StockEventAdjustmentDto stockEventAdjustmentDto = new StockEventAdjustmentDto();
    stockEventAdjustmentDto
        .setReasonId(type.getPhysicalReasonId(getCreateStockCardContext(), programId, reasonName));
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
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return facilityReferenceDataService.getFacilityById(homeFacilityId);
  }

  private final ThreadLocal<CreateStockCardContext> createStockCardContextHolder = new ThreadLocal<>();

  private void initCreateStockCardContext(FacilityDto facilityDto) {
    createStockCardContextHolder.set(new CreateStockCardContext(facilityDto));
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

    CreateStockCardContext(FacilityDto facility) {
      programToReasonNameToId = validReasonAssignmentService
          .getValidReasonsForAllProducts(facility.getType().getId(), null, null)
          .stream()
          .collect(groupingBy(ValidReasonAssignmentDto::getProgramId,
              Collectors.toMap(lineItem -> lineItem.getReason().getName(),
                  validReasonAssignmentDto -> validReasonAssignmentDto.getReason().getId())));

      programToDestinationNameToId = siglusValidSourceDestinationService
          .findDestinationsForAllProducts(facility.getId())
          .stream()
          .collect(groupingBy(ValidSourceDestinationDto::getProgramId,
              Collectors.toMap(ValidSourceDestinationDto::getName,
                  validSourceDestinationDto -> validSourceDestinationDto.getNode().getId())));

      programToSourceNameToId = siglusValidSourceDestinationService
          .findSourcesForAllProducts(facility.getId())
          .stream()
          .collect(groupingBy(ValidSourceDestinationDto::getProgramId,
              Collectors.toMap(ValidSourceDestinationDto::getName,
                  validSourceDestinationDto -> validSourceDestinationDto.getNode().getId())));
    }

    @Nonnull
    UUID findReasonId(UUID programId, String value) {
      return programToReasonNameToId.get(programId).get(AdjustmentReason.valueOf(value).getValue());
    }

    @Nonnull
    UUID findSourceId(UUID programId, String value) {
      return programToSourceNameToId.get(programId).get(Source.valueOf(value).getValue());
    }

    @Nonnull
    UUID findDestinationId(UUID programId, String value) {
      return programToDestinationNameToId.get(programId).get(Destination.valueOf(value).getValue());
    }
  }

  private List<ReportTypeResponse> findSupportReportTypes(UUID facilityId, List<SupportedProgramDto> programs) {
    List<Requisition> requisitions = requisitionRepository.findLatestRequisitionByFacilityId(facilityId);
    Map<UUID, String> programIdToCode = programs.stream()
        .collect(toMap(SupportedProgramDto::getId, SupportedProgramDto::getCode));
    Map<String, Requisition> programCodeToRequisition = requisitions.stream()
        .collect(toMap(requisition -> programIdToCode.get(requisition.getProgramId()), Function.identity()));
    return reportTypeRepository.findByFacilityId(facilityId).stream()
        .map(
            reportType -> ReportTypeResponse.builder().name(reportType.getName())
                .supportActive(reportType.getActive())
                .supportStartDate(reportType.getStartDate())
                .programCode(reportType.getProgramCode())
                .lastReportDate(findLastReportDate(reportType, programCodeToRequisition))
                .build())
        .collect(Collectors.toList());
  }

  private LocalDate findLastReportDate(ReportType reportType, Map<String, Requisition> programCodeToRequisition) {
    Requisition requisition = programCodeToRequisition.get(reportType.getProgramCode());
    if (requisition == null || requisition.getExtraData() == null) {
      return null;
    }
    return LocalDate.parse(String.valueOf(requisition.getExtraData().get(ACTUAL_END_DATE)));
  }
}
