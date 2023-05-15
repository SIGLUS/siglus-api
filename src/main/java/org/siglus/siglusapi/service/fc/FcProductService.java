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
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS_BY_ORDERABLES;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_ORDERABLES;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;
import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.FieldConstants.IS_TRACER;
import static org.siglus.siglusapi.constant.FieldConstants.ORDERABLE_ID;
import static org.siglus.siglusapi.dto.fc.FcIntegrationResultDto.buildResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.openlmis.referencedata.domain.Dispensable;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.openlmis.requisition.dto.BaseDto;
import org.openlmis.requisition.dto.BasicProgramDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.domain.BasicProductCode;
import org.siglus.siglusapi.domain.CustomProductsRegimens;
import org.siglus.siglusapi.domain.FcIntegrationChanges;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.ApprovedProductDto;
import org.siglus.siglusapi.dto.FacilityTypeDto;
import org.siglus.siglusapi.dto.OrderableDisplayCategoryDto;
import org.siglus.siglusapi.dto.TradeItemDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultBuildDto;
import org.siglus.siglusapi.dto.fc.FcIntegrationResultDto;
import org.siglus.siglusapi.dto.fc.ProductInfoDto;
import org.siglus.siglusapi.dto.fc.ProductPriceDto;
import org.siglus.siglusapi.dto.fc.ResponseBaseDto;
import org.siglus.siglusapi.repository.BasicProductCodeRepository;
import org.siglus.siglusapi.repository.CustomProductsRegimensRepository;
import org.siglus.siglusapi.repository.ProgramOrderablesRepository;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.SiglusOrderableDisplayCategoriesRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.client.OrderableDisplayCategoryReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.TradeItemReferenceDataService;
import org.siglus.siglusapi.service.fc.mapper.FcProductMapper;
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

  private final SiglusOrderableDisplayCategoriesRepository orderableDisplayCategoriesRepository;

  private final CustomProductsRegimensRepository customProductsRegimensRepository;

  private final CacheManager cacheManager;

  private static final String SYSTEM_DEFAULT_MANUFACTURER = "Mozambique";

  private static final String DEFAULT_UNIT = "each";

  private Map<String, ProgramRealProgram> realProgramCodeToEntityMap;

  private Map<String, UUID> programCodeToIdMap;

  private Map<UUID, String> programIdToCodeMap;

  private Map<String, OrderableDisplayCategoryDto> categoryCodeToEntityMap;

  private Map<String, String> categoryDisplayNameToCodeMap;

  private Set<String> basicProductCodes;

  private final Map<String, CustomProductsRegimens> codeToCustomProductsRegimens = newHashMap();

  private final ProgramOrderablesRepository programOrderablesRepository;
  private Map<UUID, UUID> orderableIdToProgramId;

  @Override
  public FcIntegrationResultDto processData(List<? extends ResponseBaseDto> products,
      String startDate,
      ZonedDateTime previousLastUpdatedAt) {
    log.info("[FC product] synced count: {}", products.size());
    if (products.isEmpty()) {
      return null;
    }
    boolean finalSuccess = true;
    AtomicInteger createCounter = new AtomicInteger();
    AtomicInteger updateCounter = new AtomicInteger();
    AtomicInteger sameCounter = new AtomicInteger();
    AtomicInteger errorCounter = new AtomicInteger();
    List<FcIntegrationChanges> fcIntegrationChangesList = new ArrayList<>();
    try {
      fetchAndCacheMasterData();
      Map<String, ProductInfoDto> productCodeToProduct = newHashMap();
      List<String> errorCodes = new ArrayList<>();
      products.forEach(item -> {
        ProductInfoDto product = (ProductInfoDto) item;
        trimProductCode(product);
        addProductCodeToProductMap(productCodeToProduct, product);
      });

      List<CustomProductsRegimens> customProductsRegimens = customProductsRegimensRepository.findAll();
      customProductsRegimens.forEach(item -> codeToCustomProductsRegimens.put(item.getCode(), item));

      productCodeToProduct.values().forEach(current -> {
        OrderableDto existed = orderableService.getOrderableByCode(current.getFnm());
        if (existed != null) {
          FcIntegrationChanges updateChanges = getUpdatedOrderable(existed, current);
          if (updateChanges != null) {
            log.info("[FC product] update, existed: {}, current: {}", existed, current);
            OrderableDto orderableDto = updateOrderable(existed, current);
            if (updateChanges.isUpdateProgram()) {
              updateFtap(orderableDto);
            }
            updateCounter.getAndIncrement();
            fcIntegrationChangesList.add(updateChanges);
          } else {
            sameCounter.getAndIncrement();
          }
          createProgramOrderablesExtension(current, existed.getId());
        } else if (FcUtil.isNotMatchedCode(current.getFnm())) {
          errorCodes.add(current.getFnm());
          errorCounter.getAndIncrement();
        } else {
          log.info("[FC product] create: {}", current);
          OrderableDto orderableDto = createOrderable(current);
          createFtap(orderableDto);
          orderableIdToProgramId.put(orderableDto.getId(),
              ((ProgramOrderableDto) orderableDto.getPrograms().toArray()[0]).getProgramId());
          createProgramOrderablesExtension(current, orderableDto.getId());
          createCounter.getAndIncrement();
          FcIntegrationChanges createChanges = FcUtil
              .buildCreateFcIntegrationChanges(PRODUCT_API, orderableDto.getProductCode(),
                  orderableDto.toString());
          fcIntegrationChangesList.add(createChanges);
        }
      });
      log.info("[FC product] process data error code: {}", errorCodes);
    } catch (Exception e) {
      log.error("[FC product] process data error", e);
      finalSuccess = false;
    }
    clearProductCaches();
    log.info("[FC product] process data create: {}, update: {}, error: {}, same: {}",
        createCounter.get(), updateCounter.get(), errorCounter.get(), sameCounter.get());
    return buildResult(
        new FcIntegrationResultBuildDto(PRODUCT_API, products, startDate, previousLastUpdatedAt,
            finalSuccess,
            createCounter.get(), updateCounter.get(), null, fcIntegrationChangesList));
  }

  private void addProductCodeToProductMap(Map<String, ProductInfoDto> productCodeToProduct,
      ProductInfoDto product) {
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
    Set<ProgramOrderablesExtension> extensions =
        getProgramOrderablesExtensionsForOneProduct(product, orderableId,
            realProgramCodeToEntityMap);
    for (ProgramOrderablesExtension extension : extensions) {
      if (orderableIdToProgramId.containsKey(orderableId)
          && orderableIdToProgramId.get(orderableId).equals(programCodeToIdMap.get(extension.getProgramCode()))) {
        programOrderablesExtensionRepository.save(extension);
        break;
      }
    }
  }

  Set<ProgramOrderablesExtension> getProgramOrderablesExtensionsForOneProduct(
      ProductInfoDto product,
      UUID orderableId,
      Map<String, ProgramRealProgram> realProgramCodeToEntityMap) {
    Set<ProgramOrderablesExtension> extensions = newHashSet();
    product
        .getAreas()
        .forEach(
            areaDto -> {
              ProgramRealProgram programRealProgram =
                  realProgramCodeToEntityMap.get(areaDto.getAreaCode());
              if (programRealProgram == null) {
                return;
              }
              ProgramOrderablesExtension extension =
                  ProgramOrderablesExtension.builder()
                      .orderableId(orderableId)
                      .realProgramCode(areaDto.getAreaCode())
                      .realProgramName(areaDto.getAreaDescription())
                      .programCode(programRealProgram.getProgramCode())
                      .programName(programRealProgram.getProgramName())
                      .build();
              extensions.add(extension);
            });
    return extensions;
  }

  private void fetchAndCacheMasterData() {
    realProgramCodeToEntityMap = programRealProgramRepository.findAll().stream().collect(
        Collectors.toMap(ProgramRealProgram::getRealProgramCode, Function.identity()));
    programCodeToIdMap = programReferenceDataService.findAll().stream()
        .collect(Collectors.toMap(BasicProgramDto::getCode, BaseDto::getId));
    programIdToCodeMap = programReferenceDataService.findAll().stream()
        .collect(Collectors.toMap(BaseDto::getId, BasicProgramDto::getCode));
    categoryCodeToEntityMap = categoryRefDataService.findAll().stream()
        .collect(Collectors.toMap(OrderableDisplayCategoryDto::getCode, Function.identity()));
    basicProductCodes = basicProductCodeRepository.findAll().stream()
        .map(BasicProductCode::getProductCode).collect(toSet());
    categoryDisplayNameToCodeMap = orderableDisplayCategoriesRepository.findAll().stream()
        .collect(toMap(odc -> odc.getOrderedDisplayValue().getDisplayName(), odc -> odc.getCode().toString()));
    orderableIdToProgramId = programOrderablesRepository.findAllMaxVersionProgramOrderableDtos().stream()
        .collect(toMap(org.siglus.siglusapi.repository.dto.ProgramOrderableDto::getOrderableId,
            org.siglus.siglusapi.repository.dto.ProgramOrderableDto::getProgramId));
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
    Map<String, Object> extraData = createOrderableExtraData(product, newOrderable.getProductCode(),
        basicProductCodes);
    newOrderable.setExtraData(extraData);
    newOrderable.setTradeItemIdentifier(createTradeItem());
    newOrderable.setPrograms(buildProgramOrderableDtos(product));
    return orderableReferenceDataService.create(newOrderable);
  }

  Map<String, Object> createOrderableExtraData(
      ProductInfoDto product, String productCode, Set<String> basicProductCodes) {
    Map<String, Object> extraData = newHashMap();
    if (!FcUtil.isActive(product.getStatus())) {
      extraData.put(ACTIVE, false);
    }
    if (basicProductCodes.contains(productCode)) {
      extraData.put(IS_BASIC, true);
    }
    if (Boolean.TRUE.equals(product.getIsSentinel())) {
      extraData.put(IS_TRACER, true);
    }
    return extraData;
  }

  OrderableDto updateOrderable(OrderableDto existed, ProductInfoDto current) {
    existed.setDescription(current.getDescription());
    existed.setFullProductName(current.getFullDescription());
    Map<String, Object> extraData = createOrderableExtraData(current, existed.getProductCode(),
        basicProductCodes);
    existed.setExtraData(extraData);
    existed.setPrograms(buildProgramOrderableDtos(current));
    return orderableReferenceDataService.update(existed);
  }

  private Set<ProgramOrderableDto> buildProgramOrderableDtos(ProductInfoDto product) {
    return new FcProductMapper(
        realProgramCodeToEntityMap, programCodeToIdMap, categoryCodeToEntityMap, categoryDisplayNameToCodeMap)
        .getProgramOrderablesFrom(product, codeToCustomProductsRegimens);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity")
  private FcIntegrationChanges getUpdatedOrderable(OrderableDto existed, ProductInfoDto current) {
    boolean isSame = true;
    StringBuilder updateContent = new StringBuilder();
    StringBuilder originContent = new StringBuilder();
    if (isBothInactive(existed, current)) {
      log.info("[FC product] existed and current product are both inactive, ignore");
      return null;
    }
    if (!StringUtils.equals(existed.getDescription(), current.getDescription())) {
      log.info("[FC product] description different, existed: {}, current: {}",
          existed.getDescription(),
          current.getDescription());
      updateContent.append("description=").append(current.getDescription()).append("; ");
      originContent.append("description=").append(existed.getDescription()).append("; ");
      isSame = false;
    }
    if (!StringUtils.equals(existed.getFullProductName(), current.getFullDescription())) {
      log.info("[FC product] name different, existed: {}, current: {}",
          existed.getFullProductName(),
          current.getFullDescription());
      updateContent.append("productName=").append(current.getFullDescription()).append("; ");
      originContent.append("productName=").append(existed.getFullProductName()).append("; ");
      isSame = false;
    }
    if (isDifferentProductStatus(existed, current)) {
      log.info("[FC product] status different, existed: {}, current: {}",
          existed.getExtraData().get(ACTIVE),
          current.getStatus());
      updateContent.append("status=").append(current.getStatus()).append("; ");
      originContent.append("status=").append(existed.getExtraData().get(ACTIVE)).append("; ");
      isSame = false;
    }
    if (isDifferentProductTracer(existed, current)) {
      Object existedIsTracer =
          existed.getExtraData() == null ? null : existed.getExtraData().get(IS_TRACER);
      log.info("[FC product] isTracer different, existed: {}, current: {}", existedIsTracer,
          current.getIsSentinel());
      updateContent.append("isTracer=").append(current.getIsSentinel()).append("; ");
      originContent.append("isTracer=").append(existedIsTracer).append("; ");
      isSame = false;
    }
    boolean isUpdateProgram = false;
    if (isDifferentProgramOrderable(existed, current)) {
      log.info("[FC product] program different, existed: {}, current: {}",
          programsToString(existed.getPrograms()), current.getAreas());
      updateContent.append("program=").append(current.getAreas()).append("; ");
      originContent.append("program=").append(programsToString(existed.getPrograms())).append("; ");
      isSame = false;
      isUpdateProgram = true;
    }

    if (isDifferentProductCategory(existed, current)) {
      String originProductCategory = getOriginProductCategory(existed);
      String currentProductCategory = getCurrentProductCategory(current);
      log.info("[FC product] productCategory different, existed: {}, current: {}",
          originProductCategory, currentProductCategory);
      updateContent.append("productCategory=").append(currentProductCategory).append("; ");
      originContent.append("productCategory=").append(originProductCategory).append("; ");
      isSame = false;
      isUpdateProgram = true;
    }

    if (isDifferentProductPrice(existed, current)) {
      log.info("[FC product] productPrice different, existed: {}, current: {}",
          getOriginProductPrice(existed.getPrograms()), getCurrentProductPrice(current.getPrice()));
      updateContent.append("productPrice=")
          .append(getCurrentProductPrice(current.getPrice())).append("; ");
      originContent.append("productPrice=").append(getOriginProductPrice(existed.getPrograms()))
          .append("; ");
      isSame = false;
    }

    if (isSame) {
      return null;
    }
    return FcUtil.buildUpdateFcIntegrationChanges(PRODUCT_API, existed.getProductCode(),
        updateContent.toString(),
        originContent.toString(), isUpdateProgram);
  }

  private boolean isBothInactive(OrderableDto existed, ProductInfoDto current) {
    boolean bothInactive = Boolean.FALSE.equals(existed.getExtraData().get(ACTIVE))
        && Boolean.FALSE.equals(FcUtil.isActive(current.getStatus()));
    boolean bothWithoutProgram = (existed.getPrograms() == null || existed.getPrograms().isEmpty())
        && (current.getAreas() == null || current.getAreas().isEmpty());
    return bothInactive || bothWithoutProgram;
  }

  private boolean isDifferentProductCategory(OrderableDto existed, ProductInfoDto current) {
    String originProductCategory = getOriginProductCategory(existed);
    String currentProductCategory = getCurrentProductCategory(current);
    return !Objects.equals(originProductCategory, currentProductCategory);
  }

  private String getCurrentProductCategory(ProductInfoDto current) {
    CustomProductsRegimens customProductsRegimensByCode = codeToCustomProductsRegimens
        .get(current.getFnm());
    if (customProductsRegimensByCode != null) {
      return customProductsRegimensByCode.getCategoryType();
    }
    return current.getCategoryCode() == null
        ? "Default" : categoryCodeToEntityMap.get(current.getCategoryCode()).getDisplayName();
  }

  private String getOriginProductCategory(OrderableDto existed) {
    if (existed.getPrograms() != null && CollectionUtils.isNotEmpty(existed.getPrograms())) {
      ProgramOrderableDto existedProgramOrderableDto = (ProgramOrderableDto) existed.getPrograms().toArray()[0];
      return existedProgramOrderableDto.getOrderableCategoryDisplayName();
    }
    return null;
  }

  private boolean isDifferentProductPrice(OrderableDto existed, ProductInfoDto current) {
    Money currentProductPrice = getCurrentProductPrice(current.getPrice());
    Money originProductPrice = getOriginProductPrice(existed.getPrograms());
    Optional<Money> currentMoney = Optional.ofNullable(currentProductPrice);
    Optional<Money> originMoney = Optional.ofNullable(originProductPrice);
    return !originMoney.orElse(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(Double.MAX_VALUE)))
        .isEqual(currentMoney.orElse(Money.of(CurrencyUnit.USD, BigDecimal.valueOf(Double.MAX_VALUE))));
  }

  private Money getOriginProductPrice(Set<ProgramOrderableDto> programs) {
    ProgramOrderableDto programOrderableDto = programs.stream().findFirst().orElse(null);
    return null == programOrderableDto ? null : programOrderableDto.getPricePerPack();
  }

  public static Money getCurrentProductPrice(List<ProductPriceDto> productPrices) {
    if (CollectionUtils.isEmpty(productPrices)) {
      return null;
    }
    Optional<ProductPriceDto> productPriceDto = productPrices.stream()
        .max(Comparator.comparing(ProductPriceDto::getPriceDate));
    return productPriceDto.map(priceDto -> Money.of(CurrencyUnit.USD, priceDto.getProductPrice(), RoundingMode.HALF_UP))
        .orElse(null);
  }

  private boolean isDifferentProductStatus(OrderableDto existed, ProductInfoDto current) {
    boolean productActiveStatus = FcUtil.isActive(current.getStatus());
    Map<String, Object> extraData = existed.getExtraData();
    Object existingOrderableStatus = extraData.get(ACTIVE);
    if (null == existingOrderableStatus && !productActiveStatus) {
      return true;
    }
    return null != existingOrderableStatus
        && (boolean) existingOrderableStatus != productActiveStatus;
  }

  private boolean isDifferentProductTracer(OrderableDto existed, ProductInfoDto current) {
    Map<String, Object> existedExtraData = existed.getExtraData();
    boolean tracerData =
        existedExtraData.get(IS_TRACER) != null && (boolean) existedExtraData.get(IS_TRACER);
    return tracerData != current.getIsSentinel();
  }

  private boolean isDifferentProgramOrderable(OrderableDto existed, ProductInfoDto current) {
    Set<ProgramOrderableDto> productPrograms = buildProgramOrderableDtos(current);
    Set<ProgramOrderableDto> existingOrderablePrograms =
        existed.getPrograms() == null ? Collections.emptySet() : existed.getPrograms();
    if (existingOrderablePrograms.isEmpty()) {
      return !productPrograms.isEmpty();
    }
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
        ftapReferenceDataService.create(approvedProductDto);
      });
    });
  }

  private void updateFtap(OrderableDto orderableDto) {
    RequestParameters parameters = RequestParameters.init().set(ORDERABLE_ID, orderableDto.getId());
    List<ApprovedProductDto> approvedProductDtos = ftapReferenceDataService.getPage(parameters)
        .getContent();
    approvedProductDtos
        .forEach(approvedProductDto -> {
          RequestParameters parametersDelete = RequestParameters.init()
              .set("versionNumber", approvedProductDto.getMeta().getVersionNumber());
          ftapReferenceDataService.delete(approvedProductDto.getId(), parametersDelete);
        });
    createFtap(orderableDto);
  }


  private String programsToString(Set<ProgramOrderableDto> programs) {
    StringBuilder originPrograms = new StringBuilder("[");
    programs.forEach(program ->
        originPrograms.append("ProgramOrderableDto(")
            .append("programId:").append(program.getProgramId())
            .append(",programCode:").append(programIdToCodeMap.get(program.getProgramId()))
            .append(",status:").append(program.isActive()).append(")"));
    return originPrograms.append("]").toString();
  }

}
