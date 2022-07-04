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

package org.siglus.siglusapi.service.fc;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS_BY_ORDERABLES;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_ORDERABLES;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.FieldConstants.IS_TRACER;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.common.domain.referencedata.Dispensable;
import org.siglus.common.dto.referencedata.OrderableChildDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.ProgramOrderableDto;
import org.siglus.siglusapi.constant.ProgramConstants;
import org.siglus.siglusapi.domain.BasicProductCode;
import org.siglus.siglusapi.domain.ProgramOrderablesExtension;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.ApprovedProductDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.OrderableDisplayCategoryDto;
import org.siglus.siglusapi.dto.TradeItemDto;
import org.siglus.siglusapi.dto.fc.AreaDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.BasicProductCodeRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.client.OrderableDisplayCategoryReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.TradeItemReferenceDataService;
import org.siglus.siglusapi.util.FcUtil;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
public class FcProductService implements ProcessDataService {

  private final SiglusOrderableService orderableService;
  private final SiglusOrderableReferenceDataService orderableReferenceDataService;
  private final TradeItemReferenceDataService tradeItemReferenceDataService;
  private final SiglusFacilityTypeReferenceDataService facilityTypeReferenceDataService;
  private final SiglusFacilityTypeApprovedProductReferenceDataService ftapReferenceDataService;
  private final ProgramReferenceDataService programReferenceDataService;
  private final OrderableDisplayCategoryReferenceDataService categoryRefDataService;
  private final ProgramRealProgramRepository programRealProgramRepository;
  private final ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;
  private final BasicProductCodeRepository basicProductCodeRepository;
  private final CacheManager cacheManager;

  private static final String SYSTEM_DEFAULT_MANUFACTURER = "Mozambique";

  private Map<String, ProgramRealProgram> realProgramCodeToEntityMap;

  private Map<String, UUID> programCodeToIdMap;

  private Map<String, OrderableDisplayCategoryDto> categoryCodeToEntityMap;

  private Set<String> basicProductCodes;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> products, String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC product] synced count: {}", products.size());
    if (products.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    AtomicInteger createCounter = new AtomicInteger();
    AtomicInteger updateCounter = new AtomicInteger();
    AtomicInteger sameCounter = new AtomicInteger();
    try {
      fetchAndCacheMasterData();
      Map<String, ProductInfoDto> productCodeToProduct = newHashMap();
      products.forEach(item -> {
        ProductInfoDto product = (ProductInfoDto) item;
        trimProductCode(product);
        boolean isAreaEmpty = product.getAreas() == null || product.getAreas().isEmpty();
        if ((FcUtil.isActive(product.getStatus()) && isAreaEmpty)
            || (!isAreaEmpty && !verifyExistRealProgram(product.getAreas()))) {
          log.warn("[FC product] areas is empty or real program is null: {}", product);
          return;
        }
        addProductCodeToProduct(productCodeToProduct, product);
      });
      productCodeToProduct.values().forEach(current -> {
        OrderableDto existed = orderableService.getOrderableByCode(current.getFnm());
        if (existed == null) {
          log.info("[FC product] create: {}", current);
          OrderableDto orderableDto = createOrderable(current);
          createFtap(orderableDto);
          createProgramOrderablesExtension(current, orderableDto.getId());
          createCounter.getAndIncrement();
        } else if (isDifferent(existed, current)) {
          log.info("[FC product] update, existed: {}, current: {}", existed, current);
          OrderableDto orderableDto = updateOrderable(existed, current);
          createProgramOrderablesExtension(current, orderableDto.getId());
          updateCounter.getAndIncrement();
        } else {
          sameCounter.getAndIncrement();
        }
      });
    } catch (Exception e) {
      log.error("[FC product] process data error", e);
      finalSuccess = false;
    }
    clearProductCaches();
    log.info("[FC product] process data create: {}, update: {}, same: {}",
        createCounter.get(), updateCounter.get(), sameCounter.get());
    return buildResult(PRODUCT_API, products, startDate, previousLastUpdatedAt, finalSuccess, createCounter.get(),
        updateCounter.get());
  }

  private void addProductCodeToProduct(Map<String, ProductInfoDto> productCodeToProduct, ProductInfoDto product) {
    ProductInfoDto productInMap = productCodeToProduct.get(product.getFnm());
    if (null == productInMap || FcUtil.isActive(product.getStatus())) {
      productCodeToProduct.put(product.getFnm(), product);
    }
  }

  private void trimProductCode(ProductInfoDto product) {
    if (product.getFnm().contains(" ")) {
      log.warn("[FC product] product code has space: {}", product.getFnm());
      product.setFnm(product.getFnm().trim());
    }
  }

  private void clearProductCaches() {
    cacheManager.getCacheNames().stream()
        .filter(name -> SIGLUS_ORDERABLES.equals(name)
            || SIGLUS_APPROVED_PRODUCTS.equals(name)
            || SIGLUS_APPROVED_PRODUCTS_BY_ORDERABLES.equals(name))
        .map(cacheManager::getCache)
        .forEach(Cache::clear);
  }

  private void createProgramOrderablesExtension(ProductInfoDto product, UUID orderableId) {
    programOrderablesExtensionRepository.deleteByOrderableId(orderableId);
    Set<ProgramOrderablesExtension> extensions = newHashSet();
    product.getAreas().forEach(areaDto -> {
      ProgramRealProgram programRealProgram = realProgramCodeToEntityMap.get(areaDto.getAreaCode());
      ProgramOrderablesExtension extension = ProgramOrderablesExtension.builder()
          .orderableId(orderableId)
          .realProgramCode(areaDto.getAreaCode())
          .realProgramName(areaDto.getAreaDescription())
          .programCode(programRealProgram.getProgramCode())
          .programName(programRealProgram.getProgramName())
          .build();
      extensions.add(extension);
    });
    programOrderablesExtensionRepository.save(extensions);
  }

  private void fetchAndCacheMasterData() {
    realProgramCodeToEntityMap = programRealProgramRepository.findAll().stream().collect(
        Collectors.toMap(ProgramRealProgram::getRealProgramCode, Function.identity()));
    programCodeToIdMap = programReferenceDataService.findAll().stream()
        .collect(Collectors.toMap(BasicProgramDto::getCode, BaseDto::getId));
    categoryCodeToEntityMap = categoryRefDataService.findAll().stream()
        .collect(Collectors.toMap(OrderableDisplayCategoryDto::getCode, Function.identity()));
    basicProductCodes = basicProductCodeRepository.findAll().stream()
        .map(BasicProductCode::getProductCode).collect(toSet());
  }

  private OrderableDto createOrderable(ProductInfoDto product) {
    OrderableDto newOrderable = new OrderableDto();
    newOrderable.setProductCode(product.getFnm());
    newOrderable.setDescription(product.getDescription());
    newOrderable.setFullProductName(product.getFullDescription());
    newOrderable.setPackRoundingThreshold(1L);
    newOrderable.setNetContent(1L);
    newOrderable.setRoundToZero(false);
    newOrderable.setDispensable(Dispensable.createNew("each"));
    Map<String, Object> extraData = newHashMap();
    if (!FcUtil.isActive(product.getStatus())) {
      extraData.put(ACTIVE, false);
    }
    if (basicProductCodes.contains(newOrderable.getProductCode())) {
      extraData.put(IS_BASIC, true);
    }
    if (product.isSentinel()) {
      extraData.put(IS_TRACER, true);
    }
    newOrderable.setExtraData(extraData);
    newOrderable.setTradeItemIdentifier(createTradeItem());
    if (FcUtil.isActive(product.getStatus())) {
      newOrderable.setPrograms(buildProgramOrderableDtos(product));
    } else {
      newOrderable.setPrograms(newHashSet());
    }
    if (product.isKit()) {
      Set<OrderableChildDto> children = buildOrderableChildDtos(product);
      newOrderable.setChildren(children);
    }
    return orderableReferenceDataService.create(newOrderable);
  }

  private OrderableDto updateOrderable(OrderableDto existed, ProductInfoDto current) {
    existed.setDescription(current.getDescription());
    existed.setFullProductName(current.getFullDescription());
    Map<String, Object> extraData = existed.getExtraData();
    if (FcUtil.isActive(current.getStatus())) {
      extraData.remove(ACTIVE);
    } else {
      extraData.put(ACTIVE, false);
    }
    extraData.put(IS_TRACER, current.isSentinel());
    existed.setExtraData(extraData);
    if (FcUtil.isActive(current.getStatus())) {
      existed.setPrograms(buildProgramOrderableDtos(current));
    } else {
      existed.setPrograms(newHashSet());
    }
    if (current.isKit()) {
      Set<OrderableChildDto> children = buildOrderableChildDtos(current);
      existed.setChildren(children);
    } else {
      existed.setChildren(newHashSet());
    }
    return orderableReferenceDataService.update(existed);
  }

  private Set<OrderableChildDto> buildOrderableChildDtos(ProductInfoDto product) {
    Set<OrderableChildDto> children = newHashSet();
    product.getProductsKits().forEach(productKitDto -> {
      OrderableChildDto childDto = new OrderableChildDto();
      OrderableDto childOrderable = orderableService.getOrderableByCode(productKitDto.getFnm());
      childDto.setOrderableFromDto(childOrderable);
      childDto.setQuantity(productKitDto.getQuantity());
      children.add(childDto);
    });
    return children;
  }

  private Set<ProgramOrderableDto> buildProgramOrderableDtos(ProductInfoDto product) {
    Set<String> programCodes = product.getAreas()
        .stream()
        .filter(areaDto -> FcUtil.isActive(areaDto.getStatus()))
        .map(areaDto -> realProgramCodeToEntityMap.get(areaDto.getAreaCode()).getProgramCode())
        .collect(Collectors.toSet());
    String programCode;
    if (programCodes.contains(ProgramConstants.RAPIDTEST_PROGRAM_CODE)) {
      programCode = ProgramConstants.RAPIDTEST_PROGRAM_CODE;
    } else if (programCodes.contains(ProgramConstants.TARV_PROGRAM_CODE)) {
      programCode = ProgramConstants.TARV_PROGRAM_CODE;
    } else {
      programCode = ProgramConstants.VIA_PROGRAM_CODE;
    }
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setProgramId(programCodeToIdMap.get(programCode));
    programOrderableDto.setActive(true);
    programOrderableDto.setFullSupply(true);
    OrderableDisplayCategoryDto categoryDto = categoryCodeToEntityMap.get(product.getCategoryCode());
    if (null == categoryDto) {
      categoryDto = categoryCodeToEntityMap.get("DEFAULT");
    }
    programOrderableDto.setOrderableDisplayCategoryId(categoryDto.getId());
    programOrderableDto.setOrderableCategoryDisplayName(categoryDto.getDisplayName());
    programOrderableDto.setOrderableCategoryDisplayOrder(categoryDto.getDisplayOrder());
    return Collections.singleton(programOrderableDto);
  }

  private boolean isDifferent(OrderableDto existed, ProductInfoDto current) {
    if (!StringUtils.equals(existed.getDescription(), current.getDescription())) {
      log.info("[FC product] description different, existed: {}, current: {}", existed.getDescription(),
          current.getDescription());
      return true;
    }
    if (!StringUtils.equals(existed.getFullProductName(), current.getFullDescription())) {
      log.info("[FC product] name different, existed: {}, current: {}", existed.getFullProductName(),
          current.getFullDescription());
      return true;
    }
    if (isDifferentProductStatus(existed, current)) {
      log.info("[FC product] status different, existed: {}, current: {}", existed.getExtraData().get(ACTIVE),
          current.getStatus());
      return true;
    }
    if (isDifferentProductTracer(existed, current)) {
      log.info("[FC product] isTracer different, existed: {}, current: {}", existed.getExtraData().get(IS_TRACER),
          current.isSentinel());
      return true;
    }
    if (FcUtil.isActive(current.getStatus()) && isDifferentProgramOrderable(existed, current)) {
      log.info("[FC product] program different, existed: {}, current: {}", existed.getPrograms(), current.getAreas());
      return true;
    }
    return false;
  }

  private boolean isDifferentProductStatus(OrderableDto existed, ProductInfoDto current) {
    boolean productActiveStatus = FcUtil.isActive(current.getStatus());
    Map<String, Object> extraData = existed.getExtraData();
    Object existingOrderableStatus = extraData.get(ACTIVE);
    if (null == existingOrderableStatus && !productActiveStatus) {
      return true;
    }
    return null != existingOrderableStatus && (boolean) existingOrderableStatus != productActiveStatus;
  }

  private boolean isDifferentProductTracer(OrderableDto existed, ProductInfoDto current) {
    Map<String, Object> existedExtraData = existed.getExtraData();
    Object tracerData = existedExtraData.get(IS_TRACER);
    return tracerData == null || (boolean) tracerData != current.isSentinel();
  }

  private boolean isDifferentProgramOrderable(OrderableDto existed, ProductInfoDto current) {
    Set<ProgramOrderableDto> productPrograms = buildProgramOrderableDtos(current);
    Set<ProgramOrderableDto> existingOrderablePrograms = existed.getPrograms();
    if (productPrograms.size() != existingOrderablePrograms.size()) {
      return true;
    }
    Set<UUID> productProgramIds = productPrograms.stream().map(ProgramOrderableDto::getProgramId).collect(toSet());
    Set<UUID> existingOrderableProgramIds = existingOrderablePrograms.stream()
        .map(ProgramOrderableDto::getProgramId)
        .collect(toSet());
    if (productProgramIds.size() != existingOrderableProgramIds.size()) {
      return true;
    }
    return !productProgramIds.containsAll(existingOrderableProgramIds);
  }

  private boolean verifyExistRealProgram(List<AreaDto> areas) {
    for (AreaDto areaDto : areas) {
      if (realProgramCodeToEntityMap.get(areaDto.getAreaCode()) == null) {
        return false;
      }
    }
    return true;
  }

  private UUID createTradeItem() {
    TradeItemDto tradeItemDto = new TradeItemDto();
    tradeItemDto.setManufacturerOfTradeItem(SYSTEM_DEFAULT_MANUFACTURER);
    return tradeItemReferenceDataService.create(tradeItemDto).getId();
  }

  private void createFtap(OrderableDto orderableDto) {
    RequestParameters parameters = RequestParameters.init().set(ACTIVE, true);
    List<FacilityTypeDto> facilityTypes = facilityTypeReferenceDataService.getPage(parameters).getContent();
    facilityTypes.forEach(facilityTypeDto -> {
      ApprovedProductDto approvedProductDto = new ApprovedProductDto();
      approvedProductDto.setActive(true);
      approvedProductDto.setMaxPeriodsOfStock(Double.valueOf("3"));
      approvedProductDto.setOrderable(orderableDto);
      approvedProductDto.setFacilityType(facilityTypeDto);
      orderableDto.getPrograms().forEach(programOrderableDto -> {
        ProgramDto programDto = new ProgramDto();
        programDto.setId(programOrderableDto.getProgramId());
        approvedProductDto.setProgram(programDto);
        ftapReferenceDataService.create(approvedProductDto);
      });
    });
  }

}
