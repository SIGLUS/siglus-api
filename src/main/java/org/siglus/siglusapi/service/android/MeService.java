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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;
import javax.persistence.EntityManager;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.openlmis.fulfillment.domain.BaseEntity;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.dto.referencedata.BaseDto;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.MetadataDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.QueryOrderableSearchParams;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.config.AndroidTemplateConfigProperties;
import org.siglus.siglusapi.constant.PodConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.PodRequestBackup;
import org.siglus.siglusapi.domain.ReportType;
import org.siglus.siglusapi.domain.RequisitionRequestBackup;
import org.siglus.siglusapi.domain.StockCardRequestBackup;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.siglus.siglusapi.dto.android.request.HfCmmDto;
import org.siglus.siglusapi.dto.android.request.PodRequest;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardCreateRequest;
import org.siglus.siglusapi.dto.android.request.StockCardDeleteRequest;
import org.siglus.siglusapi.dto.android.response.CreateStockCardResponse;
import org.siglus.siglusapi.dto.android.response.FacilityProductMovementsResponse;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.PodLotLineResponse;
import org.siglus.siglusapi.dto.android.response.PodProductLineResponse;
import org.siglus.siglusapi.dto.android.response.PodResponse;
import org.siglus.siglusapi.dto.android.response.ProductResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.ProgramResponse;
import org.siglus.siglusapi.dto.android.response.ReportTypeResponse;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.errorhandling.exception.OrderNotFoundException;
import org.siglus.siglusapi.errorhandling.exception.PodInternalErrorException;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.PodRequestBackupRepository;
import org.siglus.siglusapi.repository.ReportTypeRepository;
import org.siglus.siglusapi.repository.RequisitionRequestBackupRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.StockCardRequestBackupRepository;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;
import org.siglus.siglusapi.service.android.mapper.PodLotLineMapper;
import org.siglus.siglusapi.service.android.mapper.PodMapper;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.HashEncoder;
import org.siglus.siglusapi.validator.android.StockCardCreateRequestValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class MeService {

  static final String KEY_PROGRAM_CODE = "programCode";
  static final String TRADE_ITEM_ID = "tradeItemId";

  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusArchiveProductService siglusArchiveProductService;
  private final SiglusOrderableService orderableService;
  private final SiglusApprovedProductReferenceDataService approvedProductDataService;
  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramAdditionalOrderableRepository additionalProductRepo;
  private final ProgramReferenceDataService programDataService;
  private final AppInfoRepository appInfoRepository;
  private final ProductMapper mapper;
  private final FacilityCmmsRepository facilityCmmsRepository;
  private final SiglusLotReferenceDataService lotReferenceDataService;
  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final AndroidHelper androidHelper;
  private final ReportTypeRepository reportTypeRepository;
  private final SiglusRequisitionRepository requisitionRepository;
  private final RequisitionCreateService requisitionCreateService;
  private final RequisitionSearchService requisitionSearchService;
  private final AndroidTemplateConfigProperties androidTemplateConfigProperties;
  private final SiglusProofOfDeliveryRepository podRepo;
  private final SiglusOrderService orderService;
  private final PodMapper podMapper;
  private final PodLotLineMapper podLotLineMapper;
  private final SiglusOrderableReferenceDataService orderableDataService;
  private final RequisitionRequestBackupRepository requisitionRequestBackupRepository;
  private final StockCardRequestBackupRepository stockCardRequestBackupRepository;
  private final StockCardCreateRequestValidator stockCardCreateRequestValidator;
  private final StockCardCreateContextHolder stockCardCreateContextHolder;
  private final StockCardDeleteService stockCardDeleteService;
  private final StockCardCreateService stockCardCreateService;
  private final StockCardSearchService stockCardSearchService;
  private final PodConfirmService podConfirmService;
  private final PodRequestBackupRepository podBackupRepository;
  private final SiglusProofOfDeliveryRepository podRepository;
  private final EntityManager entityManager;

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
    if (lastSyncTime != null) {
      syncResponse.setLastSyncTime(lastSyncTime.toEpochMilli());
    }
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<UUID, OrderableDto> allProducts = getAllProducts(homeFacilityId).stream()
        .collect(toMap(OrderableDto::getId, Function.identity()));

    Map<UUID, String> programIdToCode = programsHelper.findUserSupportedPrograms().stream()
        .map(programDataService::findOne)
        .collect(toMap(ProgramDto::getId, BasicProgramDto::getCode));

    Map<UUID, String> productIdToAdditionalProgramCode = additionalProductRepo.findAll().stream()
        .collect(
            toMap(ProgramAdditionalOrderable::getAdditionalOrderableId, p -> programIdToCode.get(p.getProgramId())));

    List<OrderableDto> approvedProducts = programIdToCode.keySet().stream()
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
        .map(orderable -> mapper.toResponse(orderable, allProducts, productIdToAdditionalProgramCode))
        .collect(toList());
    syncResponse.setProducts(filteredProducts);
    filteredProducts.stream()
        .map(ProductResponse::getLastUpdated)
        .mapToLong(Instant::toEpochMilli)
        .max()
        .ifPresent(syncResponse::setLastSyncTime);
    return syncResponse;
  }

  public CreateStockCardResponse createStockCards(List<StockCardCreateRequest> requests) {
    ValidatedStockCards validatedStockCards = ValidatedStockCards.builder()
        .validStockCardRequests(requests)
        .invalidProducts(Collections.emptyList())
        .build();
    CreateStockCardResponse createStockCardResponse;
    FacilityDto facilityDto = getCurrentFacilityInfo();
    LocalDate earliest = requests.stream().map(StockCardCreateRequest::getEventTime).map(EventTime::getOccurredDate)
        .min(Comparator.naturalOrder()).orElseThrow(IllegalStateException::new);
    stockCardCreateContextHolder.initContext(facilityDto, earliest);
    try {
      validatedStockCards = stockCardCreateRequestValidator.validateStockCardCreateRequest(requests);
      createStockCardResponse = CreateStockCardResponse.from(validatedStockCards);
      if (!CollectionUtils.isEmpty(validatedStockCards.getInvalidProducts())) {
        backupStockCardRequest(requests, createStockCardResponse.getDetails());
      }
      if (!CollectionUtils.isEmpty(validatedStockCards.getValidStockCardRequests())) {
        stockCardCreateService.createStockCards(validatedStockCards.getValidStockCardRequests());
      }
      return createStockCardResponse;
    } catch (Exception e) {
      try {
        backupStockCardRequest(validatedStockCards.getValidStockCardRequests(), e.getMessage());
      } catch (NullPointerException backupError) {
        log.warn("backup stock card request error", backupError);
      }
      throw e;
    } finally {
      StockCardCreateContextHolder.clearContext();
    }
  }

  public void deleteStockCardByProduct(List<StockCardDeleteRequest> stockCardDeleteRequests) {
    stockCardDeleteService.deleteStockCardByProduct(stockCardDeleteRequests);
  }

  @ParametersAreNullableByDefault
  public FacilityProductMovementsResponse getProductMovements(LocalDate since, LocalDate tillExclusive) {
    return stockCardSearchService.getProductMovementsByTime(since, tillExclusive);
  }

  public RequisitionResponse getRequisitionResponse(String startDate) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(getAllApprovedProducts());
    return requisitionSearchService
        .getRequisitionResponseByFacilityIdAndDate(facilityId, startDate, orderableIdToCode);
  }

  public void createRequisition(RequisitionCreateRequest request) {
    try {
      requisitionCreateService.createRequisition(request);
    } catch (Exception e) {
      try {
        backupRequisitionRequest(request, e);
      } catch (NullPointerException backupError) {
        log.warn("backup requisition request error", backupError);
      }
      throw e;
    }
  }

  public List<PodResponse> getProofsOfDelivery(@Nullable LocalDate since, boolean shippedOnly) {
    FacilityDto homeFacility = getCurrentFacilityInfo();
    UUID homeFacilityId = homeFacility.getId();
    if (since == null) {
      since = YearMonth.now().minusMonths(13L).atDay(1);
    }
    List<ProofOfDelivery> pods;
    if (shippedOnly) {
      pods = podRepo.findAllByFacilitySince(homeFacilityId, since, OrderStatus.SHIPPED);
    } else {
      pods = podRepo.findAllByFacilitySince(homeFacilityId, since, OrderStatus.SHIPPED, OrderStatus.RECEIVED);
    }
    Map<UUID, OrderDto> allOrders = pods.stream()
        .map(ProofOfDelivery::getShipment)
        .map(Shipment::getOrder)
        .map(BaseEntity::getId)
        .map(orderService::searchOrderByIdWithoutProducts)
        .collect(toMap(o -> o.getOrder().getId(), SiglusOrderDto::getOrder));
    Collection<UUID> productIds = pods.stream()
        .map(ProofOfDelivery::getShipment)
        .map(Shipment::getLineItems).flatMap(Collection::stream)
        .map(ShipmentLineItem::getOrderable)
        .map(VersionEntityReference::getId)
        .collect(Collectors.toSet());
    Map<UUID, String> productCodesById = orderableDataService.findByIds(productIds).stream()
        .collect(toMap(BaseDto::getId, OrderableDto::getProductCode));
    Collection<UUID> lotIds = pods.stream()
        .map(ProofOfDelivery::getShipment)
        .map(Shipment::getLineItems).flatMap(Collection::stream)
        .map(ShipmentLineItem::getLotId)
        .collect(Collectors.toSet());
    Map<UUID, LotDto> lotsById = lotReferenceDataService.findByIds(lotIds).stream()
        .collect(toMap(BaseDto::getId, Function.identity()));
    Map<UUID, String> reasonNamesById = validReasonAssignmentService
        .getValidReasonsForAllProducts(homeFacility.getType().getId(), null, null).stream()
        .collect(toMap(ValidReasonAssignmentDto::getId, r -> r.getReason().getName()));
    return pods.stream()
        .map(pod -> toPodResponse(pod, allOrders, productCodesById, lotsById, reasonNamesById))
        .collect(toList());
  }

  public PodResponse confirmPod(PodRequest podRequest) {
    UserDto user = authHelper.getCurrentUser();
    ProofOfDelivery toUpdate = podRepository.findInitiatedPodByOrderCode(podRequest.getOrderCode());
    if (toUpdate == null) {
      log.warn("Pod orderCode: {} not found:", podRequest.getOrderCode());
      backupPodRequest(podRequest, PodConstants.NOT_EXIST_MESSAGE, user);
      throw new OrderNotFoundException("siglusapi.pod.order.notFoundByCode", podRequest.getOrderCode());
    }
    if (!StringUtils.isEmpty(podRequest.getOriginNumber())) {
      log.info("Pod orderCode: {} has originNumber {},backup request", podRequest.getOrderCode(),
          podRequest.getOriginNumber());
      backupPodRequest(podRequest, "replace the old order number " + podRequest.getOriginNumber()
          + " successfully.", user);
    }
    try {
      podConfirmService.confirmPod(podRequest, toUpdate, user);
      return getPodByOrderCode(podRequest.getOrderCode());
    } catch (Exception e) {
      backupPodRequest(podRequest, PodConstants.ERROR_MESSAGE + e.getMessage(), authHelper.getCurrentUser());
      throw new PodInternalErrorException(e, "siglusapi.pod.internal.error.excepotion", podRequest.getOrderCode());
    }
  }

  private PodResponse getPodByOrderCode(String orderCode) {
    entityManager.unwrap(Session.class).clear();
    List<PodResponse> podResponses = getProofsOfDelivery(null, false);
    PodResponse podResponse = podResponses.stream().filter(p -> p.getOrder().getCode().equals(orderCode)).findFirst()
        .orElse(null);
    if (podResponse != null) {
      List<PodProductLineResponse> products = podResponse.getProducts().stream()
          .filter(p -> !CollectionUtils.isEmpty(p.getLots())).collect(Collectors.toList());
      podResponse.setProducts(products);
    }
    return podResponse;
  }

  private PodResponse toPodResponse(ProofOfDelivery pod, Map<UUID, OrderDto> allOrders,
      Map<UUID, String> productCodesById, Map<UUID, LotDto> lotsById, Map<UUID, String> reasonNamesById) {
    PodResponse podResponse = podMapper.toResponse(pod, allOrders);
    podResponse.getProducts().forEach(productLine -> {
      Map<UUID, ShipmentLineItem> shipmentLineMap = pod.getShipment().getLineItems().stream()
          .filter(podLine -> productCodesById.get(podLine.getOrderable().getId()).equals(productLine.getCode()))
          .collect(toMap(ShipmentLineItem::getLotId, Function.identity()));
      List<PodLotLineResponse> lotLines = pod.getLineItems().stream()
          .filter(podLine -> productCodesById.get(podLine.getOrderable().getId()).equals(productLine.getCode()))
          .map(l -> podLotLineMapper.toLotResponse(l, shipmentLineMap.get(l.getLotId()), lotsById, reasonNamesById))
          .collect(toList());
      productLine.setLots(lotLines);
    });
    return podResponse;
  }

  private Map<UUID, String> getOrderableIdToCode(List<org.openlmis.requisition.dto.OrderableDto> orderableDtos) {
    return orderableDtos.stream()
        .collect(toMap(org.openlmis.requisition.dto.OrderableDto::getId,
            org.openlmis.requisition.dto.OrderableDto::getProductCode));
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

  private List<org.openlmis.requisition.dto.OrderableDto> getProgramProducts(UUID homeFacilityId, ProgramDto program) {
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
        .map(lastUpdated -> lastUpdated.isAfter(lastSyncTime))
        .orElse(true);
  }

  private List<OrderableDto> getAllProducts(UUID homeFacilityId) {
    QueryOrderableSearchParams params = new QueryOrderableSearchParams(new LinkedMultiValueMap<>());
    Pageable pageable = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER, Pagination.NO_PAGINATION);
    return orderableService.searchOrderables(params, pageable, homeFacilityId).getContent();
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

  private List<ReportTypeResponse> findSupportReportTypes(UUID facilityId, List<SupportedProgramDto> programs) {
    List<Requisition> requisitions = requisitionRepository
        .findLatestRequisitionsByFacilityIdAndAndroidTemplateId(facilityId,
            androidTemplateConfigProperties.getAndroidTemplateIds());
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

  private void backupRequisitionRequest(RequisitionCreateRequest request, Exception e) {
    UserDto user = authHelper.getCurrentUser();
    String syncUpHash = request.getSyncUpHash(user);
    RequisitionRequestBackup existedBackup = requisitionRequestBackupRepository.findOneByHash(syncUpHash);
    if (existedBackup != null) {
      log.info("skip backup requisition request as syncUpHash: {} existed", syncUpHash);
      return;
    }
    String errorMessage = "";
    if (e instanceof javax.validation.ConstraintViolationException) {
      StringBuilder messageString = new StringBuilder();
      Set<ConstraintViolation<?>> constraintViolations = (((ConstraintViolationException) e).getConstraintViolations());
      constraintViolations.forEach(violation -> messageString.append(violation.getMessage()).append("\n"));
      errorMessage = messageString.toString();
    } else {
      errorMessage = e.getMessage() + e.getCause();
    }
    RequisitionRequestBackup backup = RequisitionRequestBackup.builder()
        .hash(syncUpHash)
        .facilityId(user.getHomeFacilityId())
        .userId(user.getId())
        .actualStartDate(request.getActualStartDate())
        .actualEndDate(request.getActualEndDate())
        .emergency(request.getEmergency())
        .programCode(request.getProgramCode())
        .clientSubmittedTime(request.getClientSubmittedTime())
        .errorMessage(errorMessage)
        .requestBody(request)
        .build();
    log.info("backup requisition request, syncUpHash: {}", syncUpHash);
    requisitionRequestBackupRepository.save(backup);
  }

  private void backupStockCardRequest(List<StockCardCreateRequest> stockCardCreateRequests, String errorMessage) {
    UserDto user = authHelper.getCurrentUser();
    StringBuilder hashStringBuilder = new StringBuilder(user.getId().toString() + user.getHomeFacilityId().toString());
    stockCardCreateRequests.forEach(r -> hashStringBuilder.append(r.getSyncUpProperties()));
    String syncUpHash = HashEncoder.hash(hashStringBuilder.toString());
    StockCardRequestBackup existedBackup = stockCardRequestBackupRepository.findOneByHash(syncUpHash);
    if (existedBackup != null) {
      log.info("skip backup stock card request as syncUpHash: {} existed", syncUpHash);
      return;
    }
    StockCardRequestBackup backup = StockCardRequestBackup.builder()
        .hash(syncUpHash)
        .facilityId(user.getHomeFacilityId())
        .userId(user.getId())
        .errorMessage(errorMessage)
        .requestBody(stockCardCreateRequests)
        .build();
    log.info("backup stock card request, syncUpHash: {}", syncUpHash);
    stockCardRequestBackupRepository.save(backup);
  }

  public void backupPodRequest(PodRequest podRequest, String errorMessage, UserDto user) {
    String syncUpHash = HashEncoder.hash(podRequest.getOrderCode() + user.getHomeFacilityId() + user.getId());
    PodRequestBackup existedBackup = podBackupRepository.findOneByHash(syncUpHash);
    if (existedBackup != null) {
      log.info("skip backup pod request as syncUpHash: {} existed", syncUpHash);
      return;
    }
    PodRequestBackup backup = PodRequestBackup.builder()
        .hash(syncUpHash)
        .facilityId(user.getHomeFacilityId())
        .userId(user.getId())
        .programCode(podRequest.getProgramCode())
        .orderCode(podRequest.getOrderCode())
        .errorMessage(errorMessage)
        .requestBody(podRequest)
        .build();
    log.info("backup proofOfDelivery request, syncUpHash: {}", syncUpHash);
    podBackupRepository.save(backup);
  }
}
