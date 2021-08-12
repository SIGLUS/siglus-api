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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.ACTIVE;
import static org.siglus.siglusapi.constant.FieldConstants.IS_BASIC;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.siglus.common.dto.referencedata.FacilityTypeDto;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.ProgramOrderableDto;
import org.siglus.common.dto.referencedata.TradeItemDto;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.domain.BasicProductCode;
import org.siglus.siglusapi.domain.ProgramOrderablesExtension;
import org.siglus.siglusapi.domain.ProgramRealProgram;
import org.siglus.siglusapi.dto.ApprovedProductDto;
import org.siglus.siglusapi.dto.OrderableDisplayCategoryDto;
import org.siglus.siglusapi.dto.fc.AreaDto;
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
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class FcProductServiceTest {

  @Captor
  private ArgumentCaptor<OrderableDto> orderableCaptor;

  @Captor
  private ArgumentCaptor<ApprovedProductDto> approvedProductCaptor;

  @Captor
  private ArgumentCaptor<Set<ProgramOrderablesExtension>> extensionCaptor;

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
  private BasicProductCodeRepository basicProductCodeRepository;

  @Mock
  private CacheManager cacheManager;

  private final Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);

  private final UUID orderableId = UUID.randomUUID();

  private final UUID programId = UUID.randomUUID();

  private final UUID categoryId = UUID.randomUUID();

  private final UUID tradeItemId = UUID.randomUUID();

  private final UUID facilityTypeId = UUID.randomUUID();

  private final String fnm = "fnm";

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

    BasicProductCode basicProductCode = new BasicProductCode();
    basicProductCode.setProductCode(fnm);
    when(basicProductCodeRepository.findAll()).thenReturn(newArrayList(basicProductCode));

    when(cacheManager.getCacheNames())
        .thenReturn(Arrays.asList("token", "siglus-orderables", "siglus-approved-products"));
    when(cacheManager.getCache("token")).thenReturn(cache1);
    when(cacheManager.getCache("siglus-orderables")).thenReturn(cache2);
    when(cacheManager.getCache("siglus-approved-products")).thenReturn(cache3);
  }

  @Test
  public void shouldCreateOrderable() {
    // given
    TradeItemDto tradeItemDto = new TradeItemDto();
    tradeItemDto.setId(tradeItemId);
    when(tradeItemReferenceDataService.create(any())).thenReturn(tradeItemDto);

    FacilityTypeDto facilityTypeDto = new FacilityTypeDto();
    facilityTypeDto.setId(facilityTypeId);
    when(facilityTypeReferenceDataService.getPage(any())).thenReturn(
        Pagination.getPage(newArrayList(facilityTypeDto), pageable, 1));

    OrderableDto orderableDto = new OrderableDto();
    orderableDto.setId(orderableId);
    ProgramOrderableDto programOrderable = new ProgramOrderableDto();
    programOrderable.setProgramId(programId);
    orderableDto.setPrograms(newHashSet(programOrderable));
    when(orderableReferenceDataService.create(any())).thenReturn(orderableDto);

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
    fcProductService.processProductData(newArrayList(product));

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
    ProgramOrderablesExtension extension = extensionCaptor.getValue().stream().findFirst()
        .orElse(new ProgramOrderablesExtension());
    assertEquals(orderableId, extension.getOrderableId());
    assertEquals(programCode, extension.getProgramCode());
    assertEquals(programName, extension.getProgramName());
    assertEquals(realProgramCode, extension.getRealProgramCode());
    assertEquals(realProgramName, extension.getRealProgramName());
  }

  @Test
  public void shouldUpdateOrderable() {
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
    fcProductService.processProductData(newArrayList(product));

    // then
    verify(orderableReferenceDataService).update(orderableCaptor.capture());
    OrderableDto orderableToUpdate = orderableCaptor.getValue();
    assertEquals(description, orderableToUpdate.getDescription());
    assertEquals(fullDescription, orderableToUpdate.getFullProductName());
    assertNull(orderableToUpdate.getExtraData().get(ACTIVE));
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
    ProgramOrderablesExtension extension = extensionCaptor.getValue().stream().findFirst()
        .orElse(new ProgramOrderablesExtension());
    assertEquals(orderableId, extension.getOrderableId());
    assertEquals(programCode, extension.getProgramCode());
    assertEquals(programName, extension.getProgramName());
    assertEquals(realProgramCode, extension.getRealProgramCode());
    assertEquals(realProgramName, extension.getRealProgramName());
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
    fcProductService.processProductData(newArrayList(product));

    // then
    verify(cache1, times(0)).clear();
    verify(cache2).clear();
    verify(cache3).clear();
  }
}
