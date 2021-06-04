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
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.referencedata.ProgramReferenceDataService;
import org.openlmis.stockmanagement.dto.ObjectReferenceDto;
import org.openlmis.stockmanagement.dto.referencedata.ApprovedProductDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableChildDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderablesAggregator;
import org.siglus.common.dto.referencedata.FacilityDto;
import org.siglus.common.dto.referencedata.SupportedProgramDto;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.service.client.SiglusFacilityReferenceDataService;
import org.siglus.common.util.SiglusAuthenticationHelper;
import org.siglus.common.util.SupportedProgramsHelper;
import org.siglus.siglusapi.dto.response.android.FacilityResponse;
import org.siglus.siglusapi.dto.response.android.ProductChildResponse;
import org.siglus.siglusapi.dto.response.android.ProductResponse;
import org.siglus.siglusapi.dto.response.android.ProductSyncResponse;
import org.siglus.siglusapi.service.SiglusArchiveProductService;
import org.siglus.siglusapi.service.client.SiglusApprovedProductReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
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
  private SiglusApprovedProductReferenceDataService approvedProductService;

  @Mock
  private SiglusAuthenticationHelper authHelper;

  @Mock
  private SupportedProgramsHelper programsHelper;

  @Mock
  private ProgramReferenceDataService programDataService;

  private final UUID facilityId = UUID.randomUUID();
  private final String facilityCode = "facilityCode";
  private final String facilityName = "facilityName";

  private final String oldTimeStr = "2020-12-31T09:18:34.001Z";
  private ZonedDateTime oldTime;
  // private final String syncTimeStr = "2021-04-12T12:32:26.003Z";
  // private ZonedDateTime syncTime;
  private final String latestTimeStr = "2021-05-31T09:08:35.004Z";
  private ZonedDateTime latestTime;

  private final UUID productId1 = UUID.randomUUID();

  @Before
  public void prepare() {
    UserDto user = mock(UserDto.class);
    when(user.getHomeFacilityId()).thenReturn(facilityId);
    when(authHelper.getCurrentUser()).thenReturn(user);

    oldTime = Instant.parse(oldTimeStr).atZone(ZoneId.systemDefault());
    // syncTime = Instant.parse(syncTimeStr).atZone(ZoneId.systemDefault());
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
    when(approvedProductService.getApprovedProducts(facilityId, programId1, emptyList()))
        .thenReturn(mockGetProductResponse(asList(mockOrderable1(), mockOrderable2())));
    when(approvedProductService.getApprovedProducts(facilityId, programId2, emptyList()))
        .thenReturn(mockGetProductResponse(singletonList(mockOrderable3())));
  }


  @Test
  public void shouldCallFacilityReferenceDataServiceWhenGetFacility() {
    // given
    programDtos.add(getSupportedProgramDto());
    FacilityDto facilityDto = new FacilityDto();
    facilityDto.setCode(facilityCode);
    facilityDto.setName(facilityName);
    facilityDto.setSupportedPrograms(programDtos);
    when(facilityReferenceDataService.getFacilityById(facilityId)).thenReturn(facilityDto);

    // when
    FacilityResponse response = service.getFacility();

    // then
    assertEquals(programDtos.get(0).getCode(),response.getSupportedPrograms().get(0).getCode());
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
    // given

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

  private void assertJustNow(Long time) {
    Duration duration = Duration.between(Instant.ofEpochMilli(time), Instant.now());
    assertTrue(duration.compareTo(Duration.ofSeconds(5)) < 0);
  }

  private void assertProduct1(ProductResponse product) {
    assertEquals("product 1", product.getProductCode());
    assertEquals("full name of product 1", product.getFullProductName());
    assertEquals("description of product 1", product.getDescription());
    assertEquals(1L, (long) product.getNetContent());
    assertEquals(3L, (long) product.getPackRoundingThreshold());
    assertTrue(product.getRoundToZero());
    assertEquals("code 1", product.getProgramCode());
    assertFalse(product.getIsKit());
    assertEquals(0, product.getChildren().size());
    assertFalse(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(oldTimeStr, product.getLastUpdated());
  }

  private void assertProduct2(ProductResponse product) {
    assertEquals("product 2", product.getProductCode());
    assertEquals("full name of product 2", product.getFullProductName());
    assertEquals("description of product 2", product.getDescription());
    assertEquals(2L, (long) product.getNetContent());
    assertEquals(5L, (long) product.getPackRoundingThreshold());
    assertTrue(product.getRoundToZero());
    assertEquals("code 1", product.getProgramCode());
    assertTrue(product.getIsKit());
    assertEquals(1, product.getChildren().size());
    ProductChildResponse child = product.getChildren().get(0);
    assertEquals("product 1", child.getProductCode());
    assertEquals(100L, (long) child.getQuantity());
    assertFalse(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(latestTimeStr, product.getLastUpdated());
  }

  private void assertProduct3(ProductResponse product) {
    assertEquals("product 3", product.getProductCode());
    assertEquals("full name of product 3", product.getFullProductName());
    assertEquals("description of product 3", product.getDescription());
    assertEquals(2L, (long) product.getNetContent());
    assertEquals(5L, (long) product.getPackRoundingThreshold());
    assertFalse(product.getRoundToZero());
    assertEquals("code 2", product.getProgramCode());
    assertFalse(product.getIsKit());
    assertEquals(0, product.getChildren().size());
    assertTrue(product.getIsBasic());
    assertFalse(product.getIsHiv());
    assertFalse(product.getIsNos());
    assertEquals(latestTimeStr, product.getLastUpdated());
  }

  private OrderablesAggregator mockGetProductResponse(List<ApprovedProductDto> products) {
    return new OrderablesAggregator(products);
  }

  private ApprovedProductDto mockOrderable1() {
    String productCode = "product 1";
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(productId1);
    orderable.setProductCode(productCode);
    orderable.setFullProductName("full name of " + productCode);
    orderable.setDescription("description of " + productCode);
    orderable.setNetContent(1L);
    orderable.setPackRoundingThreshold(3L);
    orderable.setRoundToZero(true);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getMeta().setLastUpdated(oldTime);
    return approvedProduct;
  }

  private ApprovedProductDto mockOrderable2() {
    String productCode = "product 2";
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(UUID.randomUUID());
    orderable.setProductCode(productCode);
    orderable.setFullProductName("full name of " + productCode);
    orderable.setDescription("description of " + productCode);
    orderable.setNetContent(2L);
    orderable.setPackRoundingThreshold(5L);
    orderable.setRoundToZero(true);
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

  private ApprovedProductDto mockOrderable3() {
    String productCode = "product 3";
    ApprovedProductDto approvedProduct = new ApprovedProductDto();
    OrderableDto orderable = new OrderableDto();
    approvedProduct.setOrderable(orderable);
    orderable.setId(UUID.randomUUID());
    orderable.setProductCode(productCode);
    orderable.setFullProductName("full name of " + productCode);
    orderable.setDescription("description of " + productCode);
    orderable.setNetContent(2L);
    orderable.setPackRoundingThreshold(5L);
    orderable.setRoundToZero(false);
    orderable.setChildren(emptySet());
    orderable.setExtraData(new HashMap<>());
    orderable.getExtraData().put("isBasic", "true");
    orderable.getMeta().setLastUpdated(latestTime);
    return approvedProduct;
  }

}
