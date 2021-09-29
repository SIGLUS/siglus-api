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
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS_BY_ORDERABLES;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_ORDERABLES;
import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;

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
import org.siglus.siglusapi.domain.BasicProductCode;
import org.siglus.siglusapi.domain.ProgramOrderablesExtension;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.ApprovedProductDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.OrderableDisplayCategoryDto;
import org.siglus.siglusapi.dto.TradeItemDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;
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
public class FcProductService {

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

  public boolean processProductData(List<ProductInfoDto> products) {
    if (isEmpty(products)) {
      return false;
    }
    AtomicInteger createCounter = new AtomicInteger();
    AtomicInteger updateCounter = new AtomicInteger();
    AtomicInteger sameCounter = new AtomicInteger();
    try {
      fetchAndCacheMasterData();
      Map<String, ProductInfoDto> productCodeToProduct = newHashMap();
      products.forEach(product -> {
        trimProductCode(product);
        ProductInfoDto productInMap = productCodeToProduct.get(product.getFnm());
        if (null == productInMap || FcUtil.isActive(product.getStatus())) {
          productCodeToProduct.put(product.getFnm(), product);
        }
      });
      log.info("[FC product] sync product count: {}", productCodeToProduct.values().size());
      productCodeToProduct.values().forEach(current -> {
        OrderableDto existed = orderableService.getOrderableByCode(current.getFnm());
        if (existed == null) {
          log.info("[FC product] create product: {}", current);
          OrderableDto orderableDto = createOrderable(current);
          createFtap(orderableDto);
          createProgramOrderablesExtension(current, orderableDto.getId());
          createCounter.getAndIncrement();
        } else if (isDifferentOrderable(existed, current)) {
          log.info("[FC product] update product, existed: {}, current: {}", existed, current);
          OrderableDto orderableDto = updateOrderable(existed, current);
          createProgramOrderablesExtension(current, orderableDto.getId());
          updateCounter.getAndIncrement();
        } else {
          sameCounter.getAndIncrement();
        }
      });
      clearProductCaches();
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      log.info("[FC product] process product data create: {}, update: {}, same: {}",
          createCounter.get(), updateCounter.get(), sameCounter.get());
    }
  }

  private void trimProductCode(ProductInfoDto product) {
    if (product.getFnm().contains(" ")) {
      log.warn("product code has space: {}", product.getFnm());
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
    newOrderable.setExtraData(extraData);
    newOrderable.setTradeItemIdentifier(createTradeItem());
    if (FcUtil.isActive(product.getStatus())) {
      Set<ProgramOrderableDto> programOrderableDtos = buildProgramOrderableDtos(product);
      newOrderable.setPrograms(programOrderableDtos);
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
    existed.setExtraData(extraData);
    if (FcUtil.isActive(current.getStatus())) {
      Set<ProgramOrderableDto> programOrderableDtos = buildProgramOrderableDtos(current);
      existed.setPrograms(programOrderableDtos);
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
    Set<ProgramOrderableDto> programOrderableDtos = newHashSet();
    product.getAreas().forEach(areaDto -> {
      ProgramOrderableDto programDto = new ProgramOrderableDto();
      String programCode = realProgramCodeToEntityMap.get(areaDto.getAreaCode()).getProgramCode();
      programDto.setProgramId(programCodeToIdMap.get(programCode));
      programDto.setActive(FcUtil.isActive(areaDto.getStatus()));
      programDto.setFullSupply(true);
      OrderableDisplayCategoryDto categoryDto = categoryCodeToEntityMap.get(product.getCategoryCode());
      if (null == categoryDto) {
        categoryDto = categoryCodeToEntityMap.get("DEFAULT");
      }
      programDto.setOrderableDisplayCategoryId(categoryDto.getId());
      programDto.setOrderableCategoryDisplayName(categoryDto.getDisplayName());
      programDto.setOrderableCategoryDisplayOrder(categoryDto.getDisplayOrder());
      programOrderableDtos.add(programDto);
    });
    Map<UUID, ProgramOrderableDto> programIdToEntityMap = newHashMap();
    programOrderableDtos.forEach(programOrderable -> {
      ProgramOrderableDto programOrderableInMap = programIdToEntityMap.get(programOrderable.getProgramId());
      if (null == programOrderableInMap || programOrderable.isActive()) {
        programIdToEntityMap.put(programOrderable.getProgramId(), programOrderable);
      }
    });
    return newHashSet(programIdToEntityMap.values());
  }

  private boolean isDifferentOrderable(OrderableDto existed, ProductInfoDto current) {
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
