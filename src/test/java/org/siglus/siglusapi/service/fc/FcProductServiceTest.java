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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_ORDERABLES;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;
import static org.siglus.siglusapi.constant.FieldConstants.IS_TRACER;
import static org.siglus.siglusapi.service.fc.FcVariables.LAST_UPDATED_AT;
import static org.siglus.siglusapi.service.fc.FcVariables.START_DATE;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.OrderableDisplayCategory;
import org.openlmis.referencedata.dto.OrderableChildDto;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.referencedata.dto.ProgramOrderableDto;
import org.openlmis.referencedata.web.OrderableController;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.web.Pagination;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.domain.BasicProductCode;
import org.siglus.siglusapi.domain.CustomProductsRegimens;
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
import org.siglus.siglusapi.repository.CustomProductsRegimensRepository;
import org.siglus.siglusapi.repository.ProgramRealProgramRepository;
import org.siglus.siglusapi.repository.SiglusOrderableDisplayCategoriesRepository;
import org.siglus.siglusapi.repository.SiglusProgramOrderableRepository;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.client.OrderableDisplayCategoryReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeApprovedProductReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusFacilityTypeReferenceDataService;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.siglus.siglusapi.service.client.TradeItemReferenceDataService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class FcProductServiceTest {

  @Captor
  private ArgumentCaptor<OrderableDto> orderableCaptor;

  @Captor
  private ArgumentCaptor<ApprovedProductDto> approvedProductCaptor;

  @Captor
  private ArgumentCaptor<ProgramOrderablesExtension> extensionCaptor;

  @InjectMocks
  private FcProductService fcProductService;

  @Mock
  private SiglusOrderableService orderableService;

  @Mock
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Mock
  private TradeItemReferenceDataService tradeItemReferenceDataService;

  @Mock
  private SiglusFacilityTypeReferenceDataService facilityTypeReferenceDataService;

  @Mock
  private SiglusFacilityTypeApprovedProductReferenceDataService ftapReferenceDataService;

  @Mock
  private ProgramRealProgramRepository programRealProgramRepository;

  @Mock
  private ProgramReferenceDataService programReferenceDataService;

  @Mock
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Mock
  private OrderableDisplayCategoryReferenceDataService categoryRefDataService;

  @Mock
  private SiglusOrderableDisplayCategoriesRepository orderableDisplayCategoriesRepository;

  @Mock
  private BasicProductCodeRepository basicProductCodeRepository;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private CustomProductsRegimensRepository customProductsRegimensRepository;

  @Mock
  private SiglusProgramOrderableRepository siglusProgramOrderableRepository;

  @Mock
  private OrderableController orderableController;

  private final Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

  private final UUID orderableId = UUID.randomUUID();

  private final UUID childrenOrderableId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID categoryId = UUID.randomUUID();

  private final UUID tradeItemId = UUID.randomUUID();

  private final UUID facilityTypeId = UUID.randomUUID();

  private final String fnm = "fnm";

  private final String kitFnm = "kitFnm";

  private final boolean isTracer = true;

  private final String description = "description";

  private final String fullDescription = "fullDescription";

  private final String realProgramCode = "PT";

  private final String realProgramName = "PTV";

  private final String programCode = "T";

  private final String programName = "TARV";

  private final String categoryCode = "11";

  private final String displayName = "Other";

  private final int displayOrder = 1;

  @Mock
  private final ConcurrentMapCache cache1 = new ConcurrentMapCache("cache1");

  @Mock
  private final ConcurrentMapCache cache2 = new ConcurrentMapCache("cache2");

  @Mock
  private final ConcurrentMapCache cache3 = new ConcurrentMapCache("cache3");

  @Before
  public void prepare() {
    ProgramRealProgram programRealProgram = ProgramRealProgram.builder()
        .realProgramCode(realProgramCode)
        .realProgramName(realProgramName)
        .programCode(programCode)
        .programName(programName)
        .active(true)
        .build();
    when(programRealProgramRepository.findAll()).thenReturn(newArrayList(programRealProgram));

    ProgramDto programDto = new ProgramDto();
    programDto.setCode(programCode);
    programDto.setId(programId);
    when(programReferenceDataService.findAll()).thenReturn(newArrayList(programDto));

    OrderableDisplayCategoryDto categoryDto = new OrderableDisplayCategoryDto();
    categoryDto.setCode(categoryCode);
    categoryDto.setId(categoryId);
    categoryDto.setDisplayName(displayName);
    categoryDto.setDisplayOrder(displayOrder);
    when(categoryRefDataService.findAll()).thenReturn(newArrayList(categoryDto));

    OrderableDisplayCategory orderableDisplayCategory = OrderableDisplayCategory.createNew(Code.code("category"));
    when(orderableDisplayCategoriesRepository.findAll()).thenReturn(newArrayList(orderableDisplayCategory));

    BasicProductCode basicProductCode = new BasicProductCode();
    basicProductCode.setProductCode(fnm);
    when(basicProductCodeRepository.findAll()).thenReturn(newArrayList(basicProductCode));

    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(facilityTypeId);
    when(facilityTypeReferenceDataService.getPage(any())).thenReturn(
        Pagination.getPage(newArrayList(facilityTypeDto), pageable, 1));

    when(cacheManager.getCacheNames())
        .thenReturn(Arrays.asList("token", SIGLUS_ORDERABLES, SIGLUS_APPROVED_PRODUCTS));
    when(cacheManager.getCache("token")).thenReturn(cache1);
    when(cacheManager.getCache(SIGLUS_ORDERABLES)).thenReturn(cache2);
    when(cacheManager.getCache(SIGLUS_APPROVED_PRODUCTS)).thenReturn(cache3);
  }

  @Test
  @SuppressWarnings("java:S5961")
  public void shouldCreateOrderable() {
    // given
    TradeItemDto tradeItemDto = new TradeItemDto();
    tradeItemDto.setId(tradeItemId);
    when(tradeItemReferenceDataService.create(any())).thenReturn(tradeItemDto);

    givenChildrenOrderableDto();

    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    ProgramOrderableDto programOrderable = new ProgramOrderableDto();
    programOrderable.setProgramId(programId);
    orderableDto.setPrograms(newHashSet(programOrderable));
    when(orderableReferenceDataService.create(any())).thenReturn(orderableDto);

    ProductInfoDto product = buildProductInfoDto();
    product.setFnm(" fnm");

    // when
    fcProductService.processData(newArrayList(product), START_DATE, LAST_UPDATED_AT);

    // then
    verify(orderableReferenceDataService).create(orderableCaptor.capture());
    OrderableDto orderableToCreate = orderableCaptor.getValue();
    assertEquals(fnm, orderableToCreate.getProductCode());
    assertEquals(description, orderableToCreate.getDescription());
    assertEquals(fullDescription, orderableToCreate.getFullProductName());
    assertEquals(1L, orderableToCreate.getPackRoundingThreshold().longValue());
    assertEquals(1L, orderableToCreate.getNetContent().longValue());
    assertFalse(orderableToCreate.getRoundToZero());
    assertEquals("each", orderableToCreate.getDispensable().getDispensingUnit());
    Map<String, Object> extraData = newHashMap();
    extraData.put(IS_BASIC, true);
    extraData.put(IS_TRACER, isTracer);
    assertEquals(extraData, orderableToCreate.getExtraData());
    assertEquals(tradeItemId.toString(), orderableToCreate.getTradeItemIdentifier());
    assertEquals(1, orderableToCreate.getPrograms().size());
    orderableToCreate.getPrograms().forEach(programOrderableDto -> {
      assertEquals(programId, programOrderableDto.getProgramId());
      assertTrue(programOrderableDto.isActive());
      assertTrue(programOrderableDto.isFullSupply());
      assertEquals(categoryId, programOrderableDto.getOrderableDisplayCategoryId());
      assertEquals(displayName, programOrderableDto.getOrderableCategoryDisplayName());
      assertEquals(displayOrder, programOrderableDto.getOrderableCategoryDisplayOrder().intValue());
    });

    verify(ftapReferenceDataService).create(approvedProductCaptor.capture());
    ApprovedProductDto approvedProductDto = approvedProductCaptor.getValue();
    assertTrue(approvedProductDto.getActive());
    assertEquals(orderableId, approvedProductDto.getOrderable().getId());
    assertEquals(facilityTypeId, approvedProductDto.getFacilityType().getId());
    assertEquals(programId, approvedProductDto.getProgram().getId());

    verify(programOrderablesExtensionRepository).save(extensionCaptor.capture());
    ProgramOrderablesExtension extension = extensionCaptor.getValue();
    assertEquals(orderableId, extension.getOrderableId());
    assertEquals(programCode, extension.getProgramCode());
    assertEquals(programName, extension.getProgramName());
    assertEquals(realProgramCode, extension.getRealProgramCode());
    assertEquals(realProgramName, extension.getRealProgramName());
  }

  @Ignore("the change will be save manually through script")
  @Test
  public void shouldUpdateOrderable() {
    // given
    //    UUID uuid = UUID.fromString("59aea383-1ff1-4d65-901d-5fbe37e8ecfd");
    //    Money originProductPrice = getOriginProductPrice(uuid);
    //    ProductInfoDto product2 = buildProductInfoDto();
    //    Money currentProductPrice = getCurrentProductPrice(product2.getProductPrices());

    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    Map<String, Object> orderableExtraData = newHashMap();
    orderableExtraData.put(IS_TRACER, false);
    orderableExtraData.put(ACTIVE, false);
    orderableDto.setExtraData(orderableExtraData);
    ProgramOrderableDto programDto = new ProgramOrderableDto();
    programDto.setProgramId(UUID.randomUUID());
    Set<ProgramOrderableDto> programs = newHashSet(programDto);
    orderableDto.setPrograms(programs);
    when(orderableService.getOrderableByCode(fnm)).thenReturn(orderableDto);
    when(orderableReferenceDataService.update(any())).thenReturn(orderableDto);
    ApprovedProductDto approvedProductDto = new ApprovedProductDto();
    approvedProductDto.setId(UUID.randomUUID());
    approvedProductDto.setOrderable(orderableDto);
    when(ftapReferenceDataService.getPage(any()))
        .thenReturn(Pagination.getPage(newArrayList(approvedProductDto), pageable, 1));
    givenChildrenOrderableDto();
    ProductInfoDto product = buildProductInfoDto();
    org.siglus.siglusapi.repository.dto.ProgramOrderableDto programOrderableDto1 =
        new org.siglus.siglusapi.repository.dto.ProgramOrderableDto(orderableId,
            programId, new BigDecimal(100), true);
    when(siglusProgramOrderableRepository.findAllMaxVersionProgramOrderableDtos()).thenReturn(
        newArrayList(programOrderableDto1));
    when(orderableController.update(orderableId, orderableDto,
            new BeanPropertyBindingResult(orderableDto, "OrderableDto")))
            .thenReturn(ResponseEntity.ok().body(orderableDto));
    // when
    fcProductService.processData(newArrayList(product), START_DATE, LAST_UPDATED_AT);

    // then
    verify(orderableController).update(eq(orderableId), orderableCaptor.capture(),
            eq(new BeanPropertyBindingResult(orderableDto, "OrderableDto")));
    OrderableDto orderableToUpdate = orderableCaptor.getValue();
    assertEquals(description, orderableToUpdate.getDescription());
    assertEquals(fullDescription, orderableToUpdate.getFullProductName());
    Map<String, Object> extraData = newHashMap();
    extraData.put(IS_TRACER, isTracer);
    assertNull(extraData.get(ACTIVE));
    assertEquals(extraData, orderableToUpdate.getExtraData());
    assertEquals(1, orderableToUpdate.getPrograms().size());
    orderableToUpdate.getPrograms().forEach(programOrderableDto -> {
      assertEquals(programId, programOrderableDto.getProgramId());
      assertTrue(programOrderableDto.isActive());
      assertTrue(programOrderableDto.isFullSupply());
      assertEquals(categoryId, programOrderableDto.getOrderableDisplayCategoryId());
      assertEquals(displayName, programOrderableDto.getOrderableCategoryDisplayName());
      assertEquals(displayOrder, programOrderableDto.getOrderableCategoryDisplayOrder().intValue());
    });

    verify(programOrderablesExtensionRepository).save(extensionCaptor.capture());
    ProgramOrderablesExtension extension = extensionCaptor.getValue();
    assertEquals(orderableId, extension.getOrderableId());
    assertEquals(programCode, extension.getProgramCode());
    assertEquals(programName, extension.getProgramName());
    assertEquals(realProgramCode, extension.getRealProgramCode());
    assertEquals(realProgramName, extension.getRealProgramName());

    // given

    // when

    // then
    //    assertNull(getCurrentProductPrice(product.getProductPrices()));
    //    assertNotEquals(currentProductPrice, originProductPrice);
  }

  @Test
  public void shouldClearProductCache() {
    // given
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    orderableDto.setExtraData(newHashMap());
    when(orderableService.getOrderableByCode(fnm)).thenReturn(orderableDto);
    when(orderableReferenceDataService.update(any())).thenReturn(orderableDto);
    AreaDto area = AreaDto.builder()
        .areaCode(realProgramCode)
        .areaDescription(realProgramName)
        .status(STATUS_ACTIVE)
        .build();
    ProductInfoDto product = ProductInfoDto.builder()
        .fnm(fnm)
        .description(description)
        .fullDescription(fullDescription)
        .status(STATUS_ACTIVE)
        .areas(newArrayList(area))
        .categoryCode(categoryCode)
        .build();

    // when
    fcProductService.processData(newArrayList(product), START_DATE, LAST_UPDATED_AT);

    // then
    verify(cache1, times(0)).clear();
    verify(cache2).clear();
    verify(cache3).clear();
  }

  @Test
  public void shouldReturnNullWhenProcessDataGivenEmptyProducts() {
    // given
    List<ResponseBaseDto> products = emptyList();
    // when
    FcIntegrationResultDto fcIntegrationResultDto =
        fcProductService.processData(products, START_DATE, LAST_UPDATED_AT);

    // then
    assertThat(fcIntegrationResultDto).isNull();
  }

  @Test
  public void shouldIgnoreWhenGetProgramOrderablesExtensionGivenRealProgramNotFound() {
    AreaDto areaWithUnknownCode = AreaDto.builder().areaCode("unknown-code").build();
    ProductInfoDto productInfo =
        ProductInfoDto.builder().areas(singletonList(areaWithUnknownCode)).build();

    Set<ProgramOrderablesExtension> extensions =
        fcProductService.getProgramOrderablesExtensionsForOneProduct(
            productInfo, UUID.randomUUID(), new HashMap<>());

    assertThat(extensions).isEmpty();
  }

  @Test
  public void shouldSetActiveAsFalseWhenCreateExtraDataGivenProductIsNotActive() {
    ProductInfoDto productInfo = ProductInfoDto.builder().status("Inactivo").build();
    Map<String, Object> extraData =
        fcProductService.createOrderableExtraData(productInfo, "product-inactive", emptySet());
    assertThat(extraData).containsEntry(ACTIVE, false);
  }

  @Test
  public void shouldNotSetActiveFlagWhenCreateExtraDataGivenProductIsActive() {
    ProductInfoDto productInfo = ProductInfoDto.builder().status(STATUS_ACTIVE).build();
    Map<String, Object> extraData =
        fcProductService.createOrderableExtraData(productInfo, "product-active", emptySet());
    assertThat(extraData.get(ACTIVE)).isNull();
  }

  @Test
  public void shouldNotBasicFlagWhenCreateExtraDataGivenProductIsNotBasic() {
    ProductInfoDto productInfo = ProductInfoDto.builder().status(STATUS_ACTIVE).build();
    Map<String, Object> extraData =
        fcProductService.createOrderableExtraData(productInfo, "product-not-basic", emptySet());
    assertThat(extraData.get(IS_BASIC)).isNull();
  }

  @Test
  public void shouldRemoveActiveFlagWhenUpdateExtraDataGivenProductIsActiveNow() {
    // given
    ProductInfoDto current = ProductInfoDto.builder().status(STATUS_ACTIVE).build();
    // when
    Map<String, Object> updatedExtraData = fcProductService.createOrderableExtraData(current, fnm,
        newHashSet(fnm));
    // then
    assertThat(updatedExtraData.get(ACTIVE)).isNull();
  }

  @Test
  public void shouldSetActiveAsFalseWhenUpdateExtraDataGivenProductIsInactiveNow() {
    // given
    ProductInfoDto current = ProductInfoDto.builder().status("inactivo").build();
    // when
    Map<String, Object> updatedExtraData = fcProductService.createOrderableExtraData(current, fnm,
        newHashSet(fnm));
    // then
    assertThat(updatedExtraData).containsEntry(ACTIVE, false);
  }

  @Test
  @Ignore("ignore this test")
  public void shouldSetChildrenEmptyWhenUpdateOrderableGivenProductIsNotKit() {
    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setExtraData(new HashMap<>());
    orderableDto.setChildren(Collections.singleton(mock(OrderableChildDto.class)));
    ProductInfoDto current = ProductInfoDto.builder().status("status").isKit(false).build();

    fcProductService.updateOrderable(orderableDto, current);

    assertThat(orderableDto.getChildren()).isNotNull();
    assertThat(orderableDto.getChildren()).isEmpty();
  }

  private void givenChildrenOrderableDto() {
    OrderableDto childrenOrderableDto = new OrderableDto();
    childrenOrderableDto.setId(childrenOrderableId);
    when(orderableService.getOrderableByCode(kitFnm)).thenReturn(childrenOrderableDto);
  }

  private ProductInfoDto buildProductInfoDto() {
    AreaDto area = AreaDto.builder()
        .areaCode(realProgramCode)
        .areaDescription(realProgramName)
        .status(STATUS_ACTIVE)
        .build();

    return ProductInfoDto.builder()
        .fnm(fnm)
        .description(description)
        .fullDescription(fullDescription)
        .status(STATUS_ACTIVE)
        .areas(newArrayList(area))
        .categoryCode(categoryCode)
        .isSentinel(isTracer)
        .isKit(true)
        .build();
  }

  @Test
  public void shouldSelectCustomProductsRegimens() {
    CustomProductsRegimens customProductsRegimens = customProductsRegimensRepository
        .findCustomProductsRegimensByCode("08S01");
    System.out.println(customProductsRegimens);
  }
}
