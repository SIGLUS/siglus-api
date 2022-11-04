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

package org.siglus.siglusapi.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.javers.common.collections.Sets.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_ALREADY_ARCHIVED;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.StockEvent;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.CalculatedStockOnHandService;
import org.siglus.common.domain.ArchivedProduct;
import org.siglus.common.repository.ArchivedProductRepository;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.testutils.RequisitionLineItemDataBuilder;
import org.siglus.siglusapi.testutils.StockCardDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusArchiveProductServiceTest {

  public static final String CODE_1 = "CODE_1";
  public static final String CODE_2 = "CODE_2";
  @Captor
  private ArgumentCaptor<ArchivedProduct> archivedProductArgumentCaptor;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private ArchivedProductRepository archivedProductRepository;

  @Mock
  private SiglusUnpackService unpackService;

  @Mock
  private CalculatedStockOnHandService calculatedStockOnHandService;

  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private SiglusOrderableService siglusOrderableService;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private RequisitionRepository requisitionRepository;

  @InjectMocks
  private SiglusArchiveProductService archiveProductService;

  private final UUID facilityId = UUID.randomUUID();

  private final UUID orderableId = UUID.randomUUID();

  private final UUID stockCardId = UUID.randomUUID();

  private final UUID archivedProductId = UUID.randomUUID();

  private final ArchivedProduct archivedProduct = ArchivedProduct.builder()
      .facilityId(facilityId)
      .orderableId(orderableId).build();

  @Test
  public void shouldNotDeleteArchivedProductWhenActivateProductIfAlreadyActivate() {
    // given
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(null);

    // when
    archiveProductService.activateProduct(facilityId, orderableId);

    // then
    verify(archivedProductRepository, times(0)).delete(archivedProductArgumentCaptor.capture());
  }

  @Test
  public void shouldThrowExceptionIfArchiveProductOrderableInKit() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT));

    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        Lists.newArrayList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(orderableId));

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldThrowExceptionIfArchiveProductStockOnHandIsNotZero() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO));

    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .withStockOnHand(1).build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldThrowExceptionIfArchiveProductAlreadyArchived() {
    // then
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_ALREADY_ARCHIVED));

    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(archivedProduct);

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldArchiveProductIfNotArchived() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(null);
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    when(stockManagementDraftRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);
    calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard);

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);

    // then
    verify(archivedProductRepository).save(archivedProductArgumentCaptor.capture());
    assertNotNull(archivedProductArgumentCaptor.getValue());
  }

  @Test
  public void shouldSkipArchiveProductIfStockCardNotExisted() {
    // given
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        emptyList());
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);

    // then
    verify(archivedProductRepository, times(0))
        .findByFacilityIdAndOrderableId(facilityId, orderableId);
  }

  @Test
  public void shouldDoNothingIfAllArchiveProductsAlreadyExisted() {
    // given
    OrderableDto orderableDto1 = new OrderableDto();
    OrderableDto orderableDto2 = new OrderableDto();
    UUID orderableId1 = UUID.randomUUID();
    UUID orderableId2 = UUID.randomUUID();
    orderableDto1.setId(orderableId1);
    orderableDto2.setId(orderableId2);
    when(siglusOrderableService.getOrderableByCode(CODE_1)).thenReturn(orderableDto1);
    when(siglusOrderableService.getOrderableByCode(CODE_2)).thenReturn(orderableDto2);
    List<String> productCodes = Arrays.asList(CODE_1, CODE_2);
    Set<String> archivedProductIds = asSet(orderableId1.toString(), orderableId2.toString());
    when(archivedProductRepository.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(archivedProductIds);

    // when
    archiveProductService.archiveAllProducts(facilityId, productCodes);

    // then
    verify(archivedProductRepository, times(0)).deleteAllArchivedProductsByFacilityId(facilityId);
    verify(archivedProductRepository, times(0)).save(archivedProductArgumentCaptor.capture());
  }

  @Test
  public void shouldArchivedProductsIfAllArchiveProductsMoreThanTargetProducts() {
    // given
    OrderableDto orderableDto1 = new OrderableDto();
    OrderableDto orderableDto2 = new OrderableDto();
    UUID orderableId1 = UUID.randomUUID();
    UUID orderableId2 = UUID.randomUUID();
    orderableDto1.setId(orderableId1);
    orderableDto2.setId(orderableId2);
    when(siglusOrderableService.getOrderableByCode(CODE_1)).thenReturn(orderableDto1);
    when(siglusOrderableService.getOrderableByCode(CODE_2)).thenReturn(orderableDto2);
    StockCard stockCard1 = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId1)
        .build();
    StockCard stockCard2 = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId2)
        .build();
    calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard1);
    calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard2);
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId1)).thenReturn(
        singletonList(stockCard1));
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId2)).thenReturn(
        singletonList(stockCard2));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId1))
        .thenReturn(null);
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId2))
        .thenReturn(null);
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    when(stockManagementDraftRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);
    List<String> productCodes = Arrays.asList(CODE_1, CODE_2);
    Set<String> archivedProductIds = asSet(orderableId1.toString(), orderableId2.toString(),
        UUID.randomUUID().toString());
    when(archivedProductRepository.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(archivedProductIds);

    // when
    archiveProductService.archiveAllProducts(facilityId, productCodes);

    // then
    verify(archivedProductRepository).deleteAllArchivedProductsByFacilityId(facilityId);
    verify(archivedProductRepository, times(2)).save(archivedProductArgumentCaptor.capture());
  }

  @Test
  public void shouldArchive2TimesWhenArchived2ProductsIfOnly1ProductArchived() {
    // given
    OrderableDto orderableDto1 = new OrderableDto();
    OrderableDto orderableDto2 = new OrderableDto();
    UUID orderableId1 = UUID.randomUUID();
    UUID orderableId2 = UUID.randomUUID();
    orderableDto1.setId(orderableId1);
    orderableDto2.setId(orderableId2);
    when(siglusOrderableService.getOrderableByCode(CODE_1)).thenReturn(orderableDto1);
    when(siglusOrderableService.getOrderableByCode(CODE_2)).thenReturn(orderableDto2);
    StockCard stockCard1 = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId1)
        .build();
    StockCard stockCard2 = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId2)
        .build();
    calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard1);
    calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard2);
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId1)).thenReturn(
        singletonList(stockCard1));
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId2)).thenReturn(
        singletonList(stockCard2));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId1))
        .thenReturn(null);
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId2))
        .thenReturn(null);
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    when(stockManagementDraftRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);
    List<String> productCodes = Arrays.asList(CODE_1, CODE_2);
    Set<String> archivedProductIds = asSet(orderableId1.toString());
    when(archivedProductRepository.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(archivedProductIds);

    // when
    archiveProductService.archiveAllProducts(facilityId, productCodes);

    // then
    verify(archivedProductRepository).deleteAllArchivedProductsByFacilityId(facilityId);
    verify(archivedProductRepository, times(2)).save(archivedProductArgumentCaptor.capture());
  }

  @Test
  public void shouldDeleteArchivedItemInPhysicalInventoryDraftWhenArchiveProductIfExistsTheDraft() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(null);
    PhysicalInventoryDto physicalInventoryDraft = createInventoryDto();
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(physicalInventoryDraft);
    when(stockManagementDraftRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);

    // then
    verify(siglusPhysicalInventoryService).deletePhysicalInventoryDraftForAllPrograms(facilityId);
  }

  @Test
  public void shouldDeleteArchivedItemInStockManagementDraftWhenArchiveProductIfExistsTheDraft() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(null);
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    StockManagementDraft stockManagementDraft = createStockManagementDraft();
    when(stockManagementDraftRepository.findByFacilityId(facilityId))
        .thenReturn(singletonList(stockManagementDraft));
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);

    // then
    verify(stockManagementDraftRepository).save(stockManagementDraft);
  }

  @Test
  public void shouldDeleteArchivedItemInRequisitionDraftWhenArchiveProductIfExistsTheDraft() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(null);
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    when(stockManagementDraftRepository.findByFacilityId(facilityId))
        .thenReturn(null);
    Requisition requisition = createRequisition();
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(singletonList(requisition));

    // when
    archiveProductService.archiveProduct(facilityId, orderableId);

    // then
    verify(requisitionRepository).save(requisition);
  }

  @Test
  public void shouldActivateProduct() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(archivedProduct);

    // when
    archiveProductService.activateProduct(facilityId, orderableId);

    // then
    verify(archivedProductRepository).delete(archivedProductArgumentCaptor.capture());
    assertNotNull(archivedProductArgumentCaptor.getValue());
  }

  @Test
  public void shouldActivateProducts() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).build();
    stockCard.setId(stockCardId);
    when(stockCardRepository.findByOrderableIdInAndFacilityId(any(), any()))
        .thenReturn(singletonList(stockCard));
    archivedProduct.setId(archivedProductId);
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(archivedProduct);

    // when
    archiveProductService.activateProducts(facilityId, Sets.newHashSet(orderableId));

    // then
    verify(archivedProductRepository).delete(archivedProduct);
  }

  @Test
  public void shouldReturnFalseIfProductNotArchived() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent())
        .withOrderable(orderableId)
        .build();
    stockCard.setFacilityId(facilityId);
    when(stockCardRepository.findOne(stockCardId)).thenReturn(stockCard);
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(null);

    // when
    boolean archived = archiveProductService.isArchived(stockCardId);

    // then
    assertFalse(archived);
  }

  @Test
  public void shouldReturnTrueIfProductIsArchived() {
    // given
    StockCard stockCard = new StockCardDataBuilder(new StockEvent())
        .withOrderable(orderableId)
        .build();
    stockCard.setFacilityId(facilityId);
    when(stockCardRepository.findOne(stockCardId)).thenReturn(stockCard);
    when(archivedProductRepository.findByFacilityIdAndOrderableId(facilityId, orderableId))
        .thenReturn(archivedProduct);

    // when
    boolean archived = archiveProductService.isArchived(stockCardId);

    // then
    assertTrue(archived);
  }

  @Test
  public void shouldReturnArchivedProductsByFacilityId() {
    // given
    Set<String> archivedProducts = Sets.newHashSet(orderableId.toString());
    when(archivedProductRepository.findArchivedProductsByFacilityId(facilityId))
        .thenReturn(archivedProducts);

    // when
    Set<String> archivedProductsReturn = archiveProductService
        .searchArchivedProductsByFacilityId(facilityId);

    // then
    assertEquals(archivedProducts, archivedProductsReturn);
  }

  @Test
  public void shouldReturnArchivedProductsByFacilityIds() {
    // given
    Set<String> archivedProducts = Sets.newHashSet(orderableId.toString());
    when(archivedProductRepository.findArchivedProductsByFacilityIds(Sets.newHashSet(facilityId)))
        .thenReturn(archivedProducts);

    // when
    Set<String> archivedProductsReturn = archiveProductService
        .searchArchivedProductsByFacilityIds(Sets.newHashSet(facilityId));

    // then
    assertEquals(archivedProducts, archivedProductsReturn);
  }

  private PhysicalInventoryDto createInventoryDto() {
    PhysicalInventoryLineItemDto lineItemDto = PhysicalInventoryLineItemDto
        .builder()
        .orderableId(orderableId)
        .build();
    return PhysicalInventoryDto
        .builder()
        .lineItems(singletonList(lineItemDto))
        .build();
  }

  private StockManagementDraft createStockManagementDraft() {
    StockManagementDraftLineItem lineItem = StockManagementDraftLineItem
        .builder()
        .orderableId(orderableId)
        .build();
    return StockManagementDraft
        .builder()
        .lineItems(Lists.newArrayList(lineItem))
        .build();
  }

  private Requisition createRequisition() {
    RequisitionLineItem lineItem = new RequisitionLineItemDataBuilder()
        .withOrderable(orderableId, 1L)
        .build();
    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Lists.newArrayList(lineItem));
    return requisition;
  }
}
