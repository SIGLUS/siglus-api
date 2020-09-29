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
import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.IN_ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.FieldConstants.STATUS;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.domain.referencedata.Dispensable;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.common.dto.referencedata.OrderableChildDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.ProgramOrderableDto;
import org.siglus.common.dto.referencedata.TradeItemDto;
import org.siglus.common.util.RequestParameters;
import org.siglus.siglusapi.domain.BasicProductCode;
import org.siglus.siglusapi.domain.ProgramOrderablesExtension;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.ApprovedProductDto;
import org.siglus.siglusapi.dto.OrderableDisplayCategoryDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcProductService {

  @Autowired
  private SiglusOrderableService orderableService;

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private TradeItemReferenceDataService tradeItemReferenceDataService;

  @Autowired
  private SiglusFacilityTypeReferenceDataService facilityTypeReferenceDataService;

  @Autowired
  private SiglusFacilityTypeApprovedProductReferenceDataService ftapReferenceDataService;

  @Autowired
  private ProgramRealProgramRepository programRealProgramRepository;

  @Autowired
  private ProgramReferenceDataService programReferenceDataService;

  @Autowired
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Autowired
  private OrderableDisplayCategoryReferenceDataService categoryRefDataService;

  @Autowired
  private BasicProductCodeRepository basicProductCodeRepository;

  private static final String SYSTEM_DEFAULT_MANUFACTURER = "Mozambique";

  private Map<String, ProgramRealProgram> realProgramCodeToEntityMap;

  private Map<String, UUID> programCodeToIdMap;

  private Map<String, OrderableDisplayCategoryDto> categoryCodeToEntityMap;

  private Set<String> basicProductCodes;

  public boolean processProductData(List<ProductInfoDto> products) {
    AtomicInteger createNum = new AtomicInteger();
    AtomicInteger updateNum = new AtomicInteger();
    AtomicInteger sameNum = new AtomicInteger();
    try {
      fetchAndCacheMasterData();
      Map<String, ProductInfoDto> productCodeToEntityMap = newHashMap();
      products.forEach(product -> {
        ProductInfoDto productInMap = productCodeToEntityMap.get(product.getFnm().trim());
        if (null == productInMap || FcUtil.isActive(product.getStatus())) {
          productCodeToEntityMap.put(product.getFnm().trim(), product);
        }
      });
      log.info("process product data size: {}", productCodeToEntityMap.values().size());
      productCodeToEntityMap.values().forEach(product -> {
        OrderableDto existingOrderable = orderableService.getOrderableByCode(product.getFnm());
        if (null == existingOrderable) {
          OrderableDto orderableDto = createOrderable(product);
          createFtap(orderableDto);
          createProgramOrderablesExtension(product, orderableDto.getId());
          createNum.getAndIncrement();
        } else if (isDifferentOrderable(existingOrderable, product)) {
          OrderableDto orderableDto = updateOrderable(existingOrderable, product);
          createProgramOrderablesExtension(product, orderableDto.getId());
          updateNum.getAndIncrement();
        } else {
          sameNum.getAndIncrement();
        }
      });
      log.info("process product data createNum: {}, updateNum: {}, sameNum: {}", createNum.get(),
          updateNum.get(), sameNum.get());
      return true;
    } catch (Exception e) {
      log.info("process product data createNum: {}, updateNum: {}, sameNum: {}", createNum.get(),
          updateNum.get(), sameNum.get());
      log.error("process product data error", e);
      return false;
    }
  }

  private void createProgramOrderablesExtension(ProductInfoDto product, UUID orderableId) {
    List<ProgramOrderablesExtension> existingExtensions = programOrderablesExtensionRepository
        .findAllByOrderableId(orderableId);
    programOrderablesExtensionRepository.delete(existingExtensions);
    Set<ProgramOrderablesExtension> extensions = newHashSet();
    product.getAreas().forEach(areaDto -> {
      ProgramRealProgram programRealProgram = realProgramCodeToEntityMap
          .get(areaDto.getAreaCode());
      ProgramOrderablesExtension extension = ProgramOrderablesExtension.builder()
          .orderableId(orderableId)
          .realProgramCode(areaDto.getAreaCode())
          .realProgramName(areaDto.getAreaDescription())
          .programCode(programRealProgram.getProgramCode())
          .programName(programRealProgram.getProgramName())
          .build();
      extensions.add(extension);
    });
    log.info("save program orderables extension: {}", extensions);
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
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setProductCode(product.getFnm());
    orderableDto.setDescription(product.getDescription());
    orderableDto.setFullProductName(product.getFullDescription());
    orderableDto.setPackRoundingThreshold(16L);
    orderableDto.setNetContent(16L);
    orderableDto.setRoundToZero(false);
    orderableDto.setDispensable(Dispensable.createNew("each"));
    Map<String, Object> extraData = newHashMap();
    extraData.put(STATUS, FcUtil.isActive(product.getStatus()) ? ACTIVE : IN_ACTIVE);
    extraData.put(IS_BASIC, basicProductCodes.contains(orderableDto.getProductCode()));
    orderableDto.setExtraData(extraData);
    orderableDto.setTradeItemIdentifier(createTradeItem());
    if (FcUtil.isActive(product.getStatus())) {
      Set<ProgramOrderableDto> programOrderableDtos = buildProgramOrderableDtos(product);
      orderableDto.setPrograms(programOrderableDtos);
    } else {
      orderableDto.setPrograms(newHashSet());
    }
    if (product.isKit()) {
      Set<OrderableChildDto> children = buildOrderableChildDtos(product);
      orderableDto.setChildren(children);
    }
    log.info("create orderable: {}", orderableDto);
    return orderableReferenceDataService.create(orderableDto);
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
      OrderableDisplayCategoryDto categoryDto = categoryCodeToEntityMap
          .get(product.getCategoryCode());
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
      ProgramOrderableDto programOrderableInMap = programIdToEntityMap
          .get(programOrderable.getProgramId());
      if (null == programOrderableInMap || programOrderable.isActive()) {
        programIdToEntityMap.put(programOrderable.getProgramId(), programOrderable);
      }
    });
    return newHashSet(programIdToEntityMap.values());
  }

  private boolean isDifferentOrderable(OrderableDto existingOrderable, ProductInfoDto product) {
    if (!StringUtils.equals(existingOrderable.getDescription(), product.getDescription())) {
      return true;
    }
    if (!StringUtils.equals(existingOrderable.getFullProductName(), product.getFullDescription())) {
      return true;
    }
    if (isDifferentProductStatus(existingOrderable, product)) {
      return true;
    }
    if (FcUtil.isActive(product.getStatus())) {
      return isDifferentProgramOrderable(existingOrderable, product);
    }
    return false;
  }

  private boolean isDifferentProductStatus(OrderableDto existingOrderable, ProductInfoDto product) {
    String productStatus = FcUtil.isActive(product.getStatus()) ? ACTIVE : IN_ACTIVE;
    Map<String, Object> extraData = existingOrderable.getExtraData();
    Object existingOrderableStatus = extraData.get(STATUS);
    if (null == existingOrderableStatus && IN_ACTIVE.equals(productStatus)) {
      return true;
    }
    return null != existingOrderableStatus && !existingOrderableStatus.equals(productStatus);
  }

  private boolean isDifferentProgramOrderable(OrderableDto existingOrderable,
      ProductInfoDto product) {
    Set<ProgramOrderableDto> productPrograms = buildProgramOrderableDtos(product);
    Set<ProgramOrderableDto> existingOrderablePrograms = existingOrderable.getPrograms();
    if (productPrograms.size() != existingOrderablePrograms.size()) {
      return true;
    }
    Set<UUID> productProgramIds = productPrograms.stream().map(ProgramOrderableDto::getProgramId)
        .collect(toSet());
    Set<UUID> existingOrderableProgramIds = existingOrderablePrograms.stream()
        .map(ProgramOrderableDto::getProgramId)
        .collect(toSet());
    if (productProgramIds.size() != existingOrderableProgramIds.size()) {
      return true;
    }
    return !productProgramIds.containsAll(existingOrderableProgramIds);
  }

  private OrderableDto updateOrderable(OrderableDto existingOrderable, ProductInfoDto product) {
    existingOrderable.setDescription(product.getDescription());
    existingOrderable.setFullProductName(product.getFullDescription());
    Map<String, Object> extraData = existingOrderable.getExtraData();
    extraData.put(STATUS, FcUtil.isActive(product.getStatus()) ? ACTIVE : IN_ACTIVE);
    existingOrderable.setExtraData(extraData);
    if (FcUtil.isActive(product.getStatus())) {
      Set<ProgramOrderableDto> programOrderableDtos = buildProgramOrderableDtos(product);
      existingOrderable.setPrograms(programOrderableDtos);
    } else {
      existingOrderable.setPrograms(newHashSet());
    }
    if (product.isKit()) {
      Set<OrderableChildDto> children = buildOrderableChildDtos(product);
      existingOrderable.setChildren(children);
    } else {
      existingOrderable.setChildren(newHashSet());
    }
    log.info("update orderable: {}", existingOrderable);
    return orderableReferenceDataService.update(existingOrderable);
  }

  private UUID createTradeItem() {
    TradeItemDto tradeItemDto = new TradeItemDto();
    tradeItemDto.setManufacturerOfTradeItem(SYSTEM_DEFAULT_MANUFACTURER);
    return tradeItemReferenceDataService.create(tradeItemDto).getId();
  }

  private void createFtap(OrderableDto orderableDto) {
    RequestParameters parameters = RequestParameters.init().set(ACTIVE, true);
    List<FacilityTypeDto> facilityTypes = facilityTypeReferenceDataService.getPage(parameters)
        .getContent();
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
        log.info("create ftap: {}", approvedProductDto);
        ftapReferenceDataService.create(approvedProductDto);
      });
    });
  }

}
