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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.siglus.common.constant.ExtraDataConstants.ACTUAL_END_DATE;
import static org.siglus.siglusapi.util.ComparorUtil.distinctByKey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNullableByDefault;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.openlmis.fulfillment.domain.BaseEntity;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.OrderStatus;
import org.openlmis.fulfillment.domain.ProofOfDelivery;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.web.util.OrderDto;
import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.referencedata.dto.BaseDto;
import org.openlmis.referencedata.dto.MetadataDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicOrderableDto;
import org.openlmis.requisition.service.RequisitionService;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.domain.reason.StockCardLineItemReason;
import org.openlmis.stockmanagement.dto.ValidReasonAssignmentDto;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.constant.PodConstants;
import org.siglus.siglusapi.domain.AppInfo;
import org.siglus.siglusapi.domain.HfCmm;
import org.siglus.siglusapi.domain.PodExtension;
import org.siglus.siglusapi.domain.PodRequestBackup;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.domain.RequisitionRequestBackup;
import org.siglus.siglusapi.domain.ResyncInfo;
import org.siglus.siglusapi.domain.SiglusReportType;
import org.siglus.siglusapi.domain.StockCardRequestBackup;
import org.siglus.siglusapi.dto.FacilityDto;
import org.siglus.siglusapi.dto.GeographicProvinceDistrictDto;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.siglus.siglusapi.dto.SiglusOrderDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.EventTime;
import org.siglus.siglusapi.dto.android.RequisitionStatusDto;
import org.siglus.siglusapi.dto.android.ValidatedStockCards;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;
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
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.dto.android.response.ProductResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.dto.android.response.ProgramResponse;
import org.siglus.siglusapi.dto.android.response.ReportTypeResponse;
import org.siglus.siglusapi.dto.android.response.RequisitionResponse;
import org.siglus.siglusapi.exception.NoPermissionException;
import org.siglus.siglusapi.exception.OrderNotFoundException;
import org.siglus.siglusapi.localmachine.event.proofofdelivery.andriod.AndroidProofOfDeliverySyncedEmitter;
import org.siglus.siglusapi.repository.AppInfoRepository;
import org.siglus.siglusapi.repository.FacilityCmmsRepository;
import org.siglus.siglusapi.repository.LotNativeRepository;
import org.siglus.siglusapi.repository.PodExtensionRepository;
import org.siglus.siglusapi.repository.PodRequestBackupRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesRepository;
import org.siglus.siglusapi.repository.RequisitionExtensionRepository;
import org.siglus.siglusapi.repository.RequisitionRequestBackupRepository;
import org.siglus.siglusapi.repository.ResyncInfoRepository;
import org.siglus.siglusapi.repository.SiglusGeographicInfoRepository;
import org.siglus.siglusapi.repository.SiglusOrdersRepository;
import org.siglus.siglusapi.repository.SiglusProofOfDeliveryRepository;
import org.siglus.siglusapi.repository.SiglusReportTypeRepository;
import org.siglus.siglusapi.repository.SiglusRequisitionRepository;
import org.siglus.siglusapi.repository.StockCardRequestBackupRepository;
import org.siglus.siglusapi.service.LotConflictService;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.SiglusOrderService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.SiglusValidReasonAssignmentService;
import org.siglus.siglusapi.service.android.context.ContextHolder;
import org.siglus.siglusapi.service.android.context.CurrentUserContext;
import org.siglus.siglusapi.service.android.context.LotContext;
import org.siglus.siglusapi.service.android.context.ProductContext;
import org.siglus.siglusapi.service.android.context.StockCardCreateContextHolder;
import org.siglus.siglusapi.service.android.mapper.PodLotLineMapper;
import org.siglus.siglusapi.service.android.mapper.PodMapper;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.client.SiglusFacilityReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusLotReferenceDataService;
import org.siglus.siglusapi.util.AndroidHelper;
import org.siglus.siglusapi.util.HashEncoder;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.util.SiglusDateHelper;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.siglus.siglusapi.validator.android.StockCardCreateRequestValidator;
import org.slf4j.profiler.Profiler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public class MeService {

  static final String KEY_PROGRAM_CODE = "programCode";

  private final SiglusFacilityReferenceDataService facilityReferenceDataService;
  private final SiglusArchiveProductService siglusArchiveProductService;
  private final SiglusOrderableService orderableService;
  private final RequisitionService requisitionService;
  private final SiglusAuthenticationHelper authHelper;
  private final SupportedProgramsHelper programsHelper;
  private final ProgramAdditionalOrderableRepository additionalProductRepo;
  private final ProgramReferenceDataService programDataService;
  private final AppInfoRepository appInfoRepository;
  private final ProductMapper mapper;
  private final FacilityCmmsRepository facilityCmmsRepository;
  private final LotNativeRepository lotNativeRepository;
  private final LotConflictService lotConflictService;
  private final SiglusLotReferenceDataService siglusLotReferenceDataService;
  private final SiglusDateHelper dateHelper;
  private final SiglusValidReasonAssignmentService validReasonAssignmentService;
  private final AndroidHelper androidHelper;
  private final SiglusReportTypeRepository reportTypeRepository;
  private final SiglusRequisitionRepository requisitionRepository;
  private final RequisitionSearchService requisitionSearchService;
  private final SiglusProofOfDeliveryRepository podRepo;
  private final SiglusOrderService orderService;
  private final PodMapper podMapper;
  private final PodLotLineMapper podLotLineMapper;
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
  private final ResyncInfoRepository resyncInfoRepository;
  private final PodExtensionRepository podExtensionRepository;
  private final MeCreateRequisitionService meCreateRequisitionService;
  private final AndroidProofOfDeliverySyncedEmitter proofOfDeliverySyncedEmitter;
  private final ProgramOrderablesRepository programOrderablesRepository;
  private final ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  private final SiglusGeographicInfoRepository siglusGeographicInfoRepository;
  private final SiglusOrdersRepository siglusOrdersRepository;
  private final RequisitionExtensionRepository requisitionExtensionRepository;

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
    GeographicProvinceDistrictDto geographicInfo = siglusGeographicInfoRepository
        .getGeographicProvinceDistrictInfo(facilityDto.getCode());
    return FacilityResponse.builder()
        .code(facilityDto.getCode())
        .name(facilityDto.getName())
        .supportedPrograms(programResponses)
        .isAndroid(androidHelper.isAndroid())
        .supportedReportTypes(findSupportReportTypes(facilityDto.getId(), programs))
        .provinceCode(geographicInfo.getProvinceCode())
        .provinceName(geographicInfo.getProvinceName())
        .districtCode(geographicInfo.getDistrictCode())
        .districtName(geographicInfo.getDistrictName())
        .build();
  }

  @Transactional
  public void processAppInfo(AppInfo appInfo) {
    AppInfo existAppInfo = appInfoRepository.findByFacilityCode(appInfo.getFacilityCode());
    if (existAppInfo != null && !existAppInfo.getUniqueId().equals(appInfo.getUniqueId())) {
      return;
    }
    UUID appInfoId = existAppInfo != null ? existAppInfo.getId() : UUID.randomUUID();
    ZonedDateTime upgradeTime = existAppInfo != null && existAppInfo.getVersionCode().equals(appInfo.getVersionCode())
        ? existAppInfo.getUpgradeTime() : ZonedDateTime.now();
    appInfo.setId(appInfoId);
    appInfo.setUpgradeTime(upgradeTime);
    log.info("process app-info , id: {}", appInfoId);
    appInfoRepository.save(appInfo);
  }

  @Transactional
  public void processHfCmms(List<HfCmmDto> hfCmmDtos) {
    hfCmmDtos.stream().map(this::buildCmm).forEach(this::saveAndUpdateCmm);
  }

  @Transactional
  public void processResyncInfo(AndroidHeader androidHeader) {
    UserDto user = authHelper.getCurrentUser();
    FacilityDto facilityDto = getFacilityInfo(user.getHomeFacilityId());
    ResyncInfo resyncInfo = ResyncInfo.builder()
        .facilityId(user.getHomeFacilityId())
        .facilityName(facilityDto.getName())
        .uniqueId(androidHeader.getUniqueId())
        .deviceInfo(androidHeader.getDeviceInfo())
        .versionCode(androidHeader.getVersionCode())
        .androidSdkVersion(androidHeader.getAndroidSdkVersion())
        .userId(user.getId())
        .username(androidHeader.getUsername())
        .build();
    log.info("process resync-info , id: {}", resyncInfo);
    resyncInfoRepository.save(resyncInfo);
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
    Map<UUID, SupportedProgramDto> programMap = programsHelper.findHomeFacilitySupportedPrograms()
        .stream().collect(toMap(SupportedProgramDto::getId, Function.identity()));

    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<UUID, OrderableDto> allProducts = getAllProducts(homeFacilityId).stream()
        .collect(toMap(OrderableDto::getId, Function.identity()));
    List<OrderableDto> needSyncProducts = programMap.values().stream()
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .map(orderable -> {
          OrderableDto dto = allProducts.get(orderable.getId());
          dto.getExtraData().put(KEY_PROGRAM_CODE, orderable.getExtraData().get(KEY_PROGRAM_CODE));
          return dto;
        })
        .filter(p -> filterByLastUpdated(p, lastSyncTime))
        .collect(toList());

    Map<UUID, String> productIdToAdditionalProgramCode = additionalProductRepo.findAll().stream()
        .filter(p -> programMap.get(p.getProgramId()) != null)
        .collect(toMap(ProgramAdditionalOrderable::getAdditionalOrderableId,
            p -> programMap.get(p.getProgramId()).getCode()));
    Map<UUID, ProgramOrderablesExtension> orderableIdToProgramOrderablesExtension
        = buildOrderableIdToExtensionMap(needSyncProducts);

    Map<UUID, ProgramOrderable> programOrderableMap = programOrderablesRepository.findByProgramIdIn(programMap.keySet())
        .stream()
        .filter(distinctByKey(programOrderable -> programOrderable.getProduct().getId()))
        .collect(toMap(programOrderable -> programOrderable.getProduct().getId(), Function.identity()));

    List<ProductResponse> filteredProducts = needSyncProducts.stream()
        .map(orderableDto -> {
          ProductResponse productResponse =
              mapper.toResponse(orderableDto, allProducts, productIdToAdditionalProgramCode);
          ProgramOrderablesExtension extension = orderableIdToProgramOrderablesExtension.get(orderableDto.getId());
          if (extension != null) {
            productResponse.setShowInReport(extension.getShowInReport() != null && extension.getShowInReport());
            productResponse.setUnit(extension.getUnit());
          } else {
            productResponse.setShowInReport(false);
          }
          ProgramOrderable programOrderable = programOrderableMap.get(orderableDto.getId());
          if (programOrderable != null) {
            productResponse.setProgramCode(programOrderable.getProgram().getCode().toString());
            productResponse.setActive(programOrderable.isActive());
          }
          return productResponse;
        })
        .filter(ProductResponse::getActive)
        .collect(toList());
    syncResponse.setProducts(filteredProducts);
    filteredProducts.stream()
        .map(ProductResponse::getLastUpdated)
        .mapToLong(Instant::toEpochMilli)
        .max()
        .ifPresent(syncResponse::setLastSyncTime);
    return syncResponse;
  }

  private Map<UUID, ProgramOrderablesExtension> buildOrderableIdToExtensionMap(
      List<OrderableDto> orderableDtos) {
    Set<UUID> orderableIds = orderableDtos.stream()
        .map(BaseDto::getId)
        .collect(Collectors.toSet());
    List<ProgramOrderablesExtension> allExtensions = programOrderablesExtensionRepository
        .findAllByOrderableIdIn(orderableIds);
    return allExtensions.stream()
        .filter(distinctByKey(ProgramOrderablesExtension::getOrderableId))
        .collect(toMap(ProgramOrderablesExtension::getOrderableId, Function.identity()));
  }

  public CreateStockCardResponse createStockCards(List<StockCardCreateRequest> requests) {
    Profiler profiler = new Profiler("create stock cards");
    profiler.setLogger(log);
    profiler.start("prepare data");
    ValidatedStockCards validatedStockCards = ValidatedStockCards.builder()
        .validStockCardRequests(requests)
        .invalidProducts(Collections.emptyList())
        .build();
    CreateStockCardResponse createStockCardResponse;
    FacilityDto facilityDto = getCurrentFacilityInfo();
    LocalDate earliest = requests.stream().map(StockCardCreateRequest::getEventTime).map(EventTime::getOccurredDate)
        .min(Comparator.naturalOrder()).orElseThrow(IllegalStateException::new);
    profiler.start("init context");
    stockCardCreateContextHolder.initContext(facilityDto, earliest);
    profiler.start("validate data");
    try {
      validatedStockCards = stockCardCreateRequestValidator.validateStockCardCreateRequest(requests);
      profiler.start("backup request");
      createStockCardResponse = CreateStockCardResponse.from(validatedStockCards);
      if (!CollectionUtils.isEmpty(validatedStockCards.getInvalidProducts())) {
        backupStockCardRequest(requests, createStockCardResponse.getDetails());
      }
      Profiler createCardsProfiler = profiler.startNested("create cards");
      createCardsProfiler.start("invoking createStockCards");
      if (!CollectionUtils.isEmpty(validatedStockCards.getValidStockCardRequests())) {
        stockCardCreateService.createStockCards(validatedStockCards.getValidStockCardRequests(), createCardsProfiler);
      }
      profiler.start("clear context");
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
      profiler.stop().log();
    }
  }

  public void deleteStockCardByProduct(List<StockCardDeleteRequest> stockCardDeleteRequests) {
    stockCardDeleteService.deleteStockCardByProduct(stockCardDeleteRequests);
  }

  @ParametersAreNullableByDefault
  public FacilityProductMovementsResponse getProductMovements(
      LocalDate since, LocalDate tillExclusive) {
    FacilityProductMovementsResponse resp = stockCardSearchService.getProductMovementsByTime(since, tillExclusive);
    return filterOutNotApprovedProducts(resp);
  }

  public RequisitionResponse getRequisitionResponse(String startDate) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<UUID, String> orderableIdToCode = getOrderableIdToCode(getAllApprovedProducts());
    return requisitionSearchService
        .getRequisitionResponseByFacilityIdAndDate(facilityId, startDate, orderableIdToCode);
  }

  public List<RequisitionStatusDto> getRegularRequisitionStatus(List<RequisitionStatusDto> requisitions) {
    UUID facilityId = authHelper.getCurrentUser().getHomeFacilityId();
    Map<String, SupportedProgramDto> programDtoMap = programsHelper.findHomeFacilitySupportedPrograms()
        .stream().collect(toMap(SupportedProgramDto::getCode, dto -> dto));
    requisitions.forEach(requisition -> {
      if (ObjectUtils.isEmpty(programDtoMap.get(requisition.getProgramCode()))) {
        throw new IllegalArgumentException("un-support program code: " + requisition.getProgramCode());
      }
    });
    return requisitionSearchService.getRegularRequisitionsStatus(facilityId, requisitions, programDtoMap);
  }

  @SuppressWarnings("PMD.PreserveStackTrace")
  public UUID createRequisition(RequisitionCreateRequest request) {
    try {
      return meCreateRequisitionService.createRequisition(request);
    } catch (Exception e) {
      try {
        backupRequisitionRequest(request, e);
      } catch (NullPointerException backupError) {
        log.warn("backup requisition request error", backupError);
      }
      if (e instanceof org.openlmis.requisition.web.PermissionMessageException) {
        log.warn("forbidden!", e);
        throw NoPermissionException.requisition();
      }
      throw e;
    }
  }

  public List<PodResponse> getProofsOfDeliveryWithFilter(@Nullable LocalDate since, boolean shippedOnly) {
    try {
      CurrentUserContext currentUserContext = CurrentUserContext.init(authHelper, facilityReferenceDataService);
      ContextHolder.attachContext(currentUserContext);
      ContextHolder.attachContext(ProductContext.init(orderableService));
      ContextHolder.attachContext(
          LotContext.init(currentUserContext.getHomeFacility().getId(), lotNativeRepository, lotConflictService,
              siglusLotReferenceDataService, dateHelper));
      List<PodResponse> podResponses = getProofsOfDelivery(since, shippedOnly, null);
      return podResponses.stream().map(this::filterNoLotPod).collect(Collectors.toList());
    } finally {
      ContextHolder.clearContext();
    }
  }

  private List<PodResponse> getProofsOfDelivery(@Nullable LocalDate since, boolean shippedOnly, String orderCode) {
    if (since == null) {
      since = YearMonth.now().minusMonths(13L).atDay(1);
    }
    List<ProofOfDelivery> pods;
    FacilityDto homeFacility = ContextHolder.getContext(CurrentUserContext.class).getHomeFacility();
    UUID homeFacilityId = homeFacility.getId();
    if (shippedOnly) {
      if (orderCode != null && isCreateForClient(orderCode)) {
        pods = podRepo.findAllByFacilitySince(homeFacilityId, since, orderCode, OrderStatus.RECEIVED);
      } else {
        pods = podRepo.findAllByFacilitySince(homeFacilityId, since, orderCode, OrderStatus.SHIPPED);
      }
    } else {
      pods = podRepo
          .findAllByFacilitySince(homeFacilityId, since, orderCode, OrderStatus.SHIPPED, OrderStatus.RECEIVED);
    }
    Map<UUID, OrderDto> orderIdToOrder = pods.stream()
        .map(ProofOfDelivery::getShipment)
        .map(Shipment::getOrder)
        .map(BaseEntity::getId)
        .map(orderService::searchOrderByIdWithoutProducts)
        .collect(toMap(o -> o.getOrder().getId(), SiglusOrderDto::getOrder));
    Map<UUID, String> reasonIdToName =
        validReasonAssignmentService.getAllReasons(homeFacility.getType().getId()).stream()
            .map(ValidReasonAssignmentDto::getReason)
            .distinct()
            .collect(toMap(org.openlmis.stockmanagement.domain.BaseEntity::getId, StockCardLineItemReason::getName));
    Map<UUID, Requisition> orderIdToRequisition = orderIdToOrder.entrySet().stream()
        .collect(toMap(Entry::getKey, e -> orderService.getRequisitionByOrder(e.getValue())));
    return pods.stream()
        .map(pod -> toPodResponse(pod, orderIdToOrder, reasonIdToName, orderIdToRequisition))
        .collect(toList());
  }

  private boolean isCreateForClient(String orderCode) {
    Order order = Optional.ofNullable(siglusOrdersRepository.findByOrderCode(orderCode))
        .orElseThrow(() -> new EntityNotFoundException("order not found"));
    UUID externalId = order.getExternalId();
    UUID requisitionId = orderService.getRequisitionId(externalId);
    RequisitionExtension requisitionExtension = requisitionExtensionRepository.findByRequisitionId(requisitionId);
    return requisitionExtension.createdBySupplier();
  }

  @Transactional
  public PodResponse confirmPod(PodRequest podRequest, boolean isReplay) {
    Profiler profiler = new Profiler("confirmPod");
    profiler.setLogger(log);
    try {
      CurrentUserContext currentUserContext = CurrentUserContext.init(authHelper, facilityReferenceDataService);
      ContextHolder.attachContext(currentUserContext);
      ContextHolder.attachContext(ProductContext.init(orderableService));
      UserDto user = currentUserContext.getCurrentUser();
      UUID homeFacilityId = user.getHomeFacilityId();
      ContextHolder.attachContext(LotContext.init(homeFacilityId, lotNativeRepository, lotConflictService,
          siglusLotReferenceDataService, dateHelper));
      profiler.start("get current user");
      profiler.start("get initiated pod");
      ProofOfDelivery toUpdate = podRepository.findInitiatedPodByOrderCode(podRequest.getOrderCode());
      if (toUpdate == null) {
        log.warn("Pod orderCode: {} not found:", podRequest.getOrderCode());
        backupPodRequest(podRequest, PodConstants.NOT_EXIST_MESSAGE, user);
        throw new OrderNotFoundException(podRequest.getOrderCode());
      }
      profiler.start("back up");
      if (!StringUtils.isEmpty(podRequest.getOriginNumber())) {
        log.info("Pod orderCode: {} has originNumber {},backup request", podRequest.getOrderCode(),
            podRequest.getOriginNumber());
        backupPodRequest(podRequest, "replace the old order number " + podRequest.getOriginNumber()
            + " successfully.", user);
      }
      profiler.start("get pod");
      PodResponse podResponse = getProofsOfDelivery(null, true, podRequest.getOrderCode()).stream()
          .filter(p -> p.getOrder().getCode().equals(podRequest.getOrderCode())).findFirst().orElse(null);
      profiler.start("confirm");
      try {
        podConfirmService.confirmPod(podRequest, toUpdate, podResponse);
        log.info("Pod orderCode: {} has originNumber {},backup request", podRequest.getOrderCode(),
            podRequest.getOriginNumber());
        // only emit in online web
        if (!isReplay) {
          proofOfDeliverySyncedEmitter.emit(podRequest, toUpdate.getShipment().getOrder().getExternalId(),
              toUpdate.getSupplyingFacilityId());
        }
        log.info("andrioid Pod confirmed synced successfully, orderCode : {}", podRequest.getOrderCode());
        profiler.start("get response");
        return getPodByOrderCode(podRequest.getOrderCode());
      } catch (Exception e) {
        backupPodRequest(podRequest, PodConstants.ERROR_MESSAGE + e.getMessage(), authHelper.getCurrentUser());
        throw e;
      }
    } finally {
      profiler.stop().log();
      ContextHolder.clearContext();
    }
  }

  private PodResponse getPodByOrderCode(String orderCode) {
    entityManager.unwrap(Session.class).clear();
    PodResponse podResponse = getProofsOfDelivery(null, false, orderCode).stream().findFirst()
        .orElse(null);
    if (podResponse != null) {
      List<PodProductLineResponse> products = podResponse.getProducts().stream()
          .filter(p -> !CollectionUtils.isEmpty(p.getLots())).collect(Collectors.toList());
      podResponse.setProducts(products);
    }
    return podResponse;
  }

  private PodResponse toPodResponse(ProofOfDelivery pod, Map<UUID, OrderDto> orderIdToOrder,
      Map<UUID, String> reasonIdToName, Map<UUID, Requisition> orderIdToRequisition) {
    PodResponse podResponse = podMapper.toResponse(pod, orderIdToOrder, orderIdToRequisition);
    podResponse.getProducts().forEach(productLine -> {
      ProductContext productContext = ContextHolder.getContext(ProductContext.class);
      Map<UUID, ShipmentLineItem> shipmentLineMap = pod.getShipment().getLineItems().stream()
          .filter(podLine ->
              productContext.getProduct(podLine.getOrderable().getId()).getProductCode().equals(productLine.getCode()))
          .collect(toMap(ShipmentLineItem::getLotId, Function.identity()));
      List<PodLotLineResponse> lotLines = pod.getLineItems().stream()
          .filter(podLine ->
              productContext.getProduct(podLine.getOrderable().getId()).getProductCode().equals(productLine.getCode()))
          .map(l -> podLotLineMapper.toLotResponse(l, shipmentLineMap.get(l.getLotId()), reasonIdToName))
          .collect(toList());
      productLine.setLots(lotLines);
    });
    PodExtension podExtension = podExtensionRepository.findByPodId(pod.getId());
    if (podExtension != null) {
      podResponse.setConferredBy(podExtension.getConferredBy());
      podResponse.setPreparedBy(podExtension.getPreparedBy());
    }
    return podResponse;
  }

  private Map<UUID, String> getOrderableIdToCode(List<org.openlmis.requisition.dto.OrderableDto> orderableDtos) {
    return orderableDtos.stream()
        .collect(toMap(org.openlmis.requisition.dto.OrderableDto::getId,
            org.openlmis.requisition.dto.OrderableDto::getProductCode));
  }

  private List<org.openlmis.requisition.dto.OrderableDto> getAllApprovedProducts() {
    UUID homeFacilityId = authHelper.getCurrentUser().getHomeFacilityId();
    return programsHelper.findHomeFacilitySupportedPrograms().stream()
        .map(program -> getProgramProducts(homeFacilityId, program))
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private List<org.openlmis.requisition.dto.OrderableDto> getProgramProducts(UUID homeFacilityId,
      SupportedProgramDto program) {
    return requisitionService.getApprovedProductsWithoutAdditional(homeFacilityId, program.getId()).stream()
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
    Pageable pageable = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER, PaginationConstants.NO_PAGINATION);
    return orderableService.searchOrderables(params, pageable, homeFacilityId).getContent();
  }

  private HfCmm buildCmm(HfCmmDto hfCmmDto) {
    return HfCmm.builder()
        .facilityCode(getCurrentFacilityInfo().getCode())
        .cmm(hfCmmDto.getCmm())
        .periodEnd(hfCmmDto.getPeriodEnd())
        .periodBegin(hfCmmDto.getPeriodBegin())
        .productCode(hfCmmDto.getProductCode())
        .build();
  }

  private void saveAndUpdateCmm(HfCmm toBeUpdatedHfCmm) {
    if (!toBeUpdatedHfCmm.getPeriodBegin().isBefore(LocalDate.now())) {
      log.warn("period begin is future date, do not save or update cmm, toBeUpdatedHfCmm:{}", toBeUpdatedHfCmm);
      return;
    }
    HfCmm hfCmm = facilityCmmsRepository.findByFacilityCodeAndProductCodeAndPeriodBeginAndPeriodEnd(
        toBeUpdatedHfCmm.getFacilityCode(), toBeUpdatedHfCmm.getProductCode(),
        toBeUpdatedHfCmm.getPeriodBegin(), toBeUpdatedHfCmm.getPeriodEnd());
    UUID cmmId = hfCmm == null ? UUID.randomUUID() : hfCmm.getId();
    toBeUpdatedHfCmm.setId(cmmId);
    log.info("save hf_cmm info , id: {}", cmmId);
    facilityCmmsRepository.save(toBeUpdatedHfCmm);
  }

  private FacilityDto getCurrentFacilityInfo() {
    return getFacilityInfo(authHelper.getCurrentUser().getHomeFacilityId());
  }

  private FacilityDto getFacilityInfo(UUID facilityId) {
    return facilityReferenceDataService.findOne(facilityId);
  }

  private List<ReportTypeResponse> findSupportReportTypes(UUID facilityId, List<SupportedProgramDto> programs) {
    List<Requisition> requisitions = requisitionRepository.findLatestRequisitionsByFacilityId(facilityId);
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

  private LocalDate findLastReportDate(SiglusReportType reportType, Map<String, Requisition> programCodeToRequisition) {
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
    String errorMessage;
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
        .clientSubmittedTime(request.getClientSubmittedTime().atZone(ZoneId.systemDefault()))
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

  private void backupPodRequest(PodRequest podRequest, String errorMessage, UserDto user) {
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

  private PodResponse filterNoLotPod(PodResponse podResponse) {
    if (podResponse != null) {
      List<PodProductLineResponse> products = podResponse.getProducts().stream()
          .filter(p -> !CollectionUtils.isEmpty(p.getLots())).collect(Collectors.toList());
      podResponse.setProducts(products);
    }
    return podResponse;
  }

  private FacilityProductMovementsResponse filterOutNotApprovedProducts(
      FacilityProductMovementsResponse resp) {
    Set<String> approvedProductCodes = getAllApprovedProducts().stream()
        .map(BasicOrderableDto::getProductCode)
        .collect(toSet());
    List<ProductMovementResponse> movementsOfApprovedProduct =
        resp.getProductMovements().stream()
            .filter(it -> approvedProductCodes.contains(it.getProductCode()))
            .collect(toList());
    resp.setProductMovements(movementsOfApprovedProduct);
    return resp;
  }
}
