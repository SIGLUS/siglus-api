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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.dto.referencedata.ApprovedProductDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableChildDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderablesAggregator;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.ProgramOrderableDto;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.domain.android.AppInfoDomain;
import org.siglus.siglusapi.dto.android.response.FacilityResponse;
import org.siglus.siglusapi.dto.android.response.ProductChildResponse;
import org.siglus.siglusapi.dto.android.response.ProductResponse;
import org.siglus.siglusapi.dto.android.response.ProductSyncResponse;
import org.siglus.siglusapi.repository.android.AppInfoRepository;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.SiglusOrderableService;
import org.siglus.siglusapi.service.android.mapper.ProductChildMapperImpl;
import org.siglus.siglusapi.service.android.mapper.ProductMapper;
import org.siglus.siglusapi.service.android.mapper.ProductMapperImpl;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ProductMapperImpl.class, ProductChildMapperImpl.class})
@SuppressWarnings("PMD.TooManyMethods")
public class SiglusMeServiceTest {

  @InjectMocks
  private SiglusMeService service;

  @Mock
  private SiglusFacilityReferenceDataService facilityReferenceDataService;

  @Mock
  private SiglusArchiveProductService siglusArchiveProductService;

  private final List<SupportedProgramDto> programDtos = new ArrayList<>();

  @Mock
  private SiglusOrderableService orderableDataService;

  @Mock
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private SupportedProgramsHelper programsHelper;

  @Mock
  private ProgramReferenceDataService programDataService;

  @Mock
  private ArchivedProductRepository archivedProductRepo;

  @Mock
  private AppInfoRepository appInfoRepository;

  @Autowired
  private ProductMapper mapper;

  private final UUID appInfoId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();

  private ZonedDateTime oldTime;
  private Instant syncTime;
  private ZonedDateTime latestTime;

  private final UUID productId1 = UUID.randomUUID();
  private final UUID productId2 = UUID.randomUUID();
  private final UUID productId3 = UUID.randomUUID();

  private final String productCode1 = "product 1";
  private final String productCode2 = "product 2";
  private final String productCode3 = "product 3";

  @Before
  public void prepare() {
    ReflectionTestUtils.setField(service, "mapper", mapper);
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);

    String oldTimeStr = "2020-12-31T09:18:34.001Z";
    oldTime = Instant.parse(oldTimeStr).atZone(ZoneId.systemDefault());
    String syncTimeStr = "2021-04-12T12:32:26.003Z";
    syncTime = Instant.parse(syncTimeStr);
    String latestTimeStr = "2021-05-31T09:08:35.004Z";
    latestTime = Instant.parse(latestTimeStr).atZone(ZoneId.systemDefault());

    UUID programId1 = UUID.randomUUID();
    ProgramDto program1 = mock(ProgramDto.class);
    when(program1.getId()).thenReturn(programId1);
    when(program1.getCode()).thenReturn("code 1");
    when(programDataService.findOne(programId1)).thenReturn(program1);
    UUID programId2 = UUID.randomUUID();
    ProgramDto program2 = mock(ProgramDto.class);
    when(program2.getId()).thenReturn(programId2);
    when(program2.getCode()).thenReturn("code 2");
    when(programDataService.findOne(programId2)).thenReturn(program2);
    when(programsHelper.findUserSupportedPrograms())
        .thenReturn(ImmutableSet.of(programId1, programId2));
    when(orderableDataService.searchOrderables(any(), any(), any()))
        .thenReturn(new PageImpl<>(asList(mockOrderable1(), mockOrderable2(), mockOrderable3())));
    when(approvedProductService.getApprovedProducts(facilityId, programId1, emptyList()))
        .thenReturn(mockGetProductResponse(asList(mockApprovedProduct1(), mockApprovedProduct2())));
    when(approvedProductService.getApprovedProducts(facilityId, programId2, emptyList()))
        .thenReturn(mockGetProductResponse(singletonList(mockApprovedProduct3())));
    when(archivedProductRepo.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(singleton(productId1.toString()));
  }


  @Test
  public void shouldCallFacilityReferenceDataServiceWhenGetFacility() {
    // given
    programDtos.add(getSupportedProgramDto());
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setCode("facilityCode");
    facilityDto.setName("facilityName");
    facilityDto.setSupportedPrograms(programDtos);
    when(facilityReferenceDataService.getFacilityById(facilityId)).thenReturn(facilityDto);

    // when
    FacilityResponse response = service.getCurrentFacility();

    // then
    assertEquals(programDtos.get(0).getCode(), response.getSupportedPrograms().get(0).getCode());
  }

  @Test
  public void shouldUpdateAppInfoWhenAppInfoIsExist() {
    // given
    AppInfoDomain appInfoCurrent = mockCurrentAppInfo();
    AppInfoDomain appInfoUpdate = mockUpdateAppInfo();
    when(appInfoRepository.findAppInfoByFacilityCodeAndUniqueId(appInfoUpdate.getFacilityCode(),
            appInfoUpdate.getUniqueId())).thenReturn(appInfoCurrent);
    when(appInfoRepository.save(appInfoUpdate)).thenReturn(appInfoCurrent);

    // when
    service.processAppInfo(appInfoUpdate);

    // then
    assertEquals(appInfoUpdate.getId(), appInfoCurrent.getId());
    assertEquals(appInfoUpdate.getFacilityCode(), appInfoCurrent.getFacilityCode());
    assertEquals(appInfoUpdate.getUniqueId(), appInfoCurrent.getUniqueId());
  }

  @Test
  public void shouldCallaArchiveProductServiceWhenDoArchive() {
    // given
    List<String> productCodes = Arrays.asList("product1", "product2", "product3");

    // when
    service.archiveAllProducts(productCodes);

    // then
    verify(siglusArchiveProductService).archiveAllProducts(facilityId, productCodes);
  }

  private SupportedProgramDto getSupportedProgramDto() {
    SupportedProgramDto supportedProgram = new SupportedProgramDto();
    supportedProgram.setId(UUID.randomUUID());
    supportedProgram.setCode("ARV");
    supportedProgram.setName("ARV");
    supportedProgram.setDescription("description");
    supportedProgram.setProgramActive(true);
    supportedProgram.setSupportActive(true);
    supportedProgram.setSupportLocallyFulfilled(true);
    supportedProgram.setSupportStartDate(LocalDate.now());
    return supportedProgram;
  }

  @Test
  public void shouldReturnAllProductsWhenGetFacilityProducts() {
    // when
    ProductSyncResponse syncResponse = service.getFacilityProducts(null);

    // then
    assertNotNull(syncResponse);
    assertJustNow(syncResponse.getLastSyncTime());
    assertNotNull(syncResponse.getProducts());
    assertEquals(3, syncResponse.getProducts().size());
    ProductResponse product1 = syncResponse.getProducts().get(0);
    assertProduct1(product1);

    ProductResponse product2 = syncResponse.getProducts().get(1);
    assertProduct2(product2);

    ProductResponse product3 = syncResponse.getProducts().get(2);
    assertProduct3(product3);
  }

  @Test
  public void shouldReturnAllProductsWhenGetFacilityProductsGivenSyncTime() {
    // when
    ProductSyncResponse syncResponse = service.getFacilityProducts(syncTime);

    // then
    assertNotNull(syncResponse);
    assertJustNow(syncResponse.getLastSyncTime());
    assertNotNull(syncResponse.getProducts());
    assertEquals(2, syncResponse.getProducts().size());
    ProductResponse product1 = syncResponse.getProducts().get(0);
    assertProduct2(product1);

    ProductResponse product2 = syncResponse.getProducts().get(1);
    assertProduct3(product2);
  }

  private void assertJustNow(Long time) {
    Duration duration = Duration.between(Instant.ofEpochMilli(time), Instant.now());
    assertTrue(duration.compareTo(Duration.ofSeconds(5)) < 0);
  }

  private void assertProduct1(ProductResponse product) {
    assertEquals(productCode1, product.getProductCode());
    assertEquals("full name of product 1", product.getFullProductName());
    assertEquals("description of product 1", product.getDescription());
    assertTrue(product.getActive());
    assertTrue(product.getArchived());
    assertEquals(1L, (long) product.getNetContent());
    assertEquals(3L, (long) product.getPackRoundingThreshold());
    assertTrue(product.getRoundToZero());
    assertEquals("code 1", product.getProgramCode());
    assertEquals("Default", product.getCategory());
    assertFalse(product.getIsKit());
    assertEquals(0, product.getChildren().size());
    assertFalse(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(oldTime.toInstant(), product.getLastUpdated());
  }

  private void assertProduct2(ProductResponse product) {
    assertEquals(productCode2, product.getProductCode());
    assertEquals("full name of product 2", product.getFullProductName());
    assertEquals("description of product 2", product.getDescription());
    assertTrue(product.getActive());
    assertFalse(product.getArchived());
    assertEquals(2L, (long) product.getNetContent());
    assertEquals(5L, (long) product.getPackRoundingThreshold());
    assertTrue(product.getRoundToZero());
    assertEquals("code 1", product.getProgramCode());
    assertEquals("12", product.getCategory());
    assertTrue(product.getIsKit());
    assertEquals(1, product.getChildren().size());
    ProductChildResponse child = product.getChildren().get(0);
    assertEquals(productCode1, child.getProductCode());
    assertEquals(100L, (long) child.getQuantity());
    assertFalse(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(latestTime.toInstant(), product.getLastUpdated());
  }

  private void assertProduct3(ProductResponse product) {
    assertEquals(productCode3, product.getProductCode());
    assertEquals("full name of product 3", product.getFullProductName());
    assertEquals("description of product 3", product.getDescription());
    assertTrue(product.getActive());
    assertFalse(product.getArchived());
    assertEquals(2L, (long) product.getNetContent());
    assertEquals(5L, (long) product.getPackRoundingThreshold());
    assertFalse(product.getRoundToZero());
    assertEquals("code 2", product.getProgramCode());
    assertEquals("13", product.getCategory());
    assertFalse(product.getIsKit());
    assertEquals(0, product.getChildren().size());
    assertTrue(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(latestTime.toInstant(), product.getLastUpdated());
  }

  private OrderablesAggregator mockGetProductResponse(List<ApprovedProductDto> products) {
    return new OrderablesAggregator(products);
  }

  private org.siglus.common.dto.referencedata.OrderableDto mockOrderable1() {
    String productCode = productCode1;
    org.siglus.common.dto.referencedata.OrderableDto orderable =
        new org.siglus.common.dto.referencedata.OrderableDto();
    orderable.setId(productId1);
    orderable.setArchived(true);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setDescription(genDescription(productCode));
    orderable.setNetContent(1L);
    orderable.setPackRoundingThreshold(3L);
    orderable.setRoundToZero(true);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(oldTime);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("Default");
    orderable.setPrograms(singleton(programOrderableDto));
    return orderable;
  }

  private ApprovedProductDto mockApprovedProduct1() {
    String productCode = productCode1;
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(productId1);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setNetContent(1L);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(oldTime);
    return approvedProduct;
  }

  private org.siglus.common.dto.referencedata.OrderableDto mockOrderable2() {
    String productCode = productCode2;
    org.siglus.common.dto.referencedata.OrderableDto orderable =
        new org.siglus.common.dto.referencedata.OrderableDto();
    orderable.setId(productId2);
    orderable.setArchived(false);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setDescription(genDescription(productCode));
    orderable.setNetContent(2L);
    orderable.setPackRoundingThreshold(5L);
    orderable.setRoundToZero(true);
    org.siglus.common.dto.referencedata.ObjectReferenceDto childRef =
        new org.siglus.common.dto.referencedata.ObjectReferenceDto();
    childRef.setId(productId1);
    org.siglus.common.dto.referencedata.OrderableChildDto child =
        new org.siglus.common.dto.referencedata.OrderableChildDto(childRef, 100L);
    orderable.setChildren(new HashSet<>());
    orderable.getChildren().add(child);
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(latestTime);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("12");
    orderable.setPrograms(singleton(programOrderableDto));
    return orderable;
  }

  private ApprovedProductDto mockApprovedProduct2() {
    String productCode = productCode2;
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(productId2);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setNetContent(2L);
    orderable.setChildren(new HashSet<>());
    OrderableChildDto child = new OrderableChildDto();
    ObjectReferenceDto childRef = new ObjectReferenceDto();
    childRef.setId(productId1);
    child.setOrderable(childRef);
    child.setQuantity(100);
    orderable.getChildren().add(child);
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(latestTime);
    return approvedProduct;
  }

  private org.siglus.common.dto.referencedata.OrderableDto mockOrderable3() {
    String productCode = productCode3;
    org.siglus.common.dto.referencedata.OrderableDto orderable =
        new org.siglus.common.dto.referencedata.OrderableDto();
    orderable.setId(productId3);
    orderable.setArchived(false);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setDescription(genDescription(productCode));
    orderable.setNetContent(2L);
    orderable.setPackRoundingThreshold(5L);
    orderable.setRoundToZero(false);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getExtraData().put("isBasic", "true");
    orderable.getMeta().setLastUpdated(latestTime);
    ProgramOrderableDto programOrderableDto = new ProgramOrderableDto();
    programOrderableDto.setOrderableCategoryDisplayName("13");
    orderable.setPrograms(singleton(programOrderableDto));
    return orderable;
  }

  private ApprovedProductDto mockApprovedProduct3() {
    String productCode = productCode3;
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(productId3);
    orderable.setProductCode(productCode);
    orderable.setFullProductName(genFullName(productCode));
    orderable.setNetContent(2L);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getExtraData().put("isBasic", "true");
    orderable.getMeta().setLastUpdated(latestTime);
    return approvedProduct;
  }

  private String genFullName(String productCode) {
    return "full name of " + productCode;
  }

  private String genDescription(String productCode) {
    return "description of " + productCode;
  }

  private AppInfoDomain mockCurrentAppInfo() {
    AppInfoDomain appInfoDomain = AppInfoDomain.builder()
            .deviceInfo("deviceinfo1")
            .facilityName("Centro de Saude de Chiunze")
            .androidsdkVersion(28)
            .versionCode(88)
            .facilityCode("01080904")
            .userName("CS_Moine_Role1")
            .uniqueId("ac36c07a09f2fdcd")
            .build();
    appInfoDomain.setId(appInfoId);
    return appInfoDomain;
  }

  private AppInfoDomain mockUpdateAppInfo() {
    AppInfoDomain appInfoDomain = AppInfoDomain.builder()
            .deviceInfo("deviceinfo2")
            .facilityName("Centro de Saude de Chiunze")
            .androidsdkVersion(28)
            .versionCode(88)
            .facilityCode("01080904")
            .userName("CS_Moine_Role1")
            .uniqueId("ac36c07a09f2fdcd")
            .build();
    appInfoDomain.setId(appInfoId);
    return appInfoDomain;
  }
}
