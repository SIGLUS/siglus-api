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

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_ALREADY_ACTIVATED;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_ALREADY_ARCHIVED;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO;
import static org.siglus.siglusapi.i18n.ArchiveMessageKeys.ERROR_ARCHIVE_STOCK_CARD_NOT_FOUND;

import com.google.common.collect.Sets;
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
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.domain.requisition.RequisitionLineItemDataBuilder;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.stockmanagement.domain.card.StockCard;
import org.openlmis.stockmanagement.domain.event.StockEvent;
import org.openlmis.stockmanagement.dto.PhysicalInventoryDto;
import org.openlmis.stockmanagement.dto.PhysicalInventoryLineItemDto;
import org.openlmis.stockmanagement.exception.ResourceNotFoundException;
import org.openlmis.stockmanagement.exception.ValidationMessageException;
import org.openlmis.stockmanagement.repository.StockCardRepository;
import org.openlmis.stockmanagement.service.CalculatedStockOnHandService;
import org.openlmis.stockmanagement.testutils.StockCardDataBuilder;
import org.siglus.common.domain.StockCardExtension;
import org.siglus.common.repository.StockCardExtensionRepository;
import org.siglus.siglusapi.domain.StockManagementDraft;
import org.siglus.siglusapi.domain.StockManagementDraftLineItem;
import org.siglus.siglusapi.repository.StockManagementDraftRepository;
import org.siglus.siglusapi.testutils.StockCardExtensionDataBuilder;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusArchiveProductServiceTest {

  @Captor
  private ArgumentCaptor<StockCardExtension> stockCardExtensionArgumentCaptor;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock
  private StockCardRepository stockCardRepository;

  @Mock
  private StockCardExtensionRepository stockCardExtensionRepository;

  @Mock
  private SiglusUnpackService unpackService;

  @Mock
  private CalculatedStockOnHandService calculatedStockOnHandService;

  @Mock
  private SiglusPhysicalInventoryService siglusPhysicalInventoryService;

  @Mock
  private StockManagementDraftRepository stockManagementDraftRepository;

  @Mock
  private RequisitionRepository requisitionRepository;

  @InjectMocks
  private SiglusArchiveProductService archiveProductService;

  private UUID facilityId = UUID.randomUUID();

  private UUID orderableId = UUID.randomUUID();

  private UUID stockCardId = UUID.randomUUID();

  @Test
  public void shouldThrowExceptionIfArchiveProductStockCardNotFound() {
    exception.expect(ResourceNotFoundException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_STOCK_CARD_NOT_FOUND));

    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        Lists.newArrayList());

    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldThrowExceptionIfActivateProductStockCardNotFound() {
    exception.expect(ResourceNotFoundException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_STOCK_CARD_NOT_FOUND));

    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        Lists.newArrayList());

    archiveProductService.activateProduct(facilityId, orderableId);
  }

  @Test
  public void shouldThrowExceptionIfArchiveProductOrderableInKit() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_CANNOT_ARCHIVE_ORDERABLE_IN_KIT));

    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        Lists.newArrayList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(orderableId));

    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldThrowExceptionIfArchiveProductStockOnHandIsNotZero() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_SOH_SHOULD_BE_ZERO));

    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .withStockOnHand(1)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));

    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldThrowExceptionIfArchiveProductAlreadyArchived() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_ALREADY_ARCHIVED));

    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(new StockCardExtensionDataBuilder().withStockCardId(stockCard.getId())
            .withArchived(true).build());

    archiveProductService.archiveProduct(facilityId, orderableId);
  }

  @Test
  public void shouldThrowExceptionIfActivateProductAlreadyActivated() {
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(containsString(ERROR_ARCHIVE_ALREADY_ACTIVATED));

    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(new StockCardExtensionDataBuilder().withStockCardId(stockCard.getId()).build());

    archiveProductService.activateProduct(facilityId, orderableId);
  }

  @Test
  public void shouldArchiveProduct() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(new StockCardExtensionDataBuilder().withStockCardId(stockCard.getId()).build());
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    when(stockManagementDraftRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);
    calculatedStockOnHandService.fetchCurrentStockOnHand(stockCard);

    archiveProductService.archiveProduct(facilityId, orderableId);

    verify(stockCardExtensionRepository).save(stockCardExtensionArgumentCaptor.capture());
    StockCardExtension stockCardExtension = stockCardExtensionArgumentCaptor.getValue();
    assertTrue(stockCardExtension.isArchived());
  }

  @Test
  public void shouldDeleteArchivedItemInPhysicalInventoryDraftWhenArchiveProductIfExistsTheDraft() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(new StockCardExtensionDataBuilder().withStockCardId(stockCard.getId()).build());
    PhysicalInventoryDto physicalInventoryDraft = createInventoryDto();
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(physicalInventoryDraft);
    when(stockManagementDraftRepository.findByFacilityId(facilityId)).thenReturn(null);
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);

    archiveProductService.archiveProduct(facilityId, orderableId);

    verify(siglusPhysicalInventoryService).deletePhysicalInventoryForAllProducts(facilityId);
  }

  @Test
  public void shouldDeleteArchivedItemInStockManagementDraftWhenArchiveProductIfExistsTheDraft() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(new StockCardExtensionDataBuilder().withStockCardId(stockCard.getId()).build());
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    StockManagementDraft stockManagementDraft = createStockManagementDraft();
    when(stockManagementDraftRepository.findByFacilityId(facilityId))
        .thenReturn(singletonList(stockManagementDraft));
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(null);

    archiveProductService.archiveProduct(facilityId, orderableId);

    verify(stockManagementDraftRepository).save(stockManagementDraft);
  }

  @Test
  public void shouldDeleteArchivedItemInRequisitionDraftWhenArchiveProductIfExistsTheDraft() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(unpackService.orderablesInKit()).thenReturn(Sets.newHashSet(UUID.randomUUID()));
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(new StockCardExtensionDataBuilder().withStockCardId(stockCard.getId()).build());
    when(siglusPhysicalInventoryService.getPhysicalInventoryForAllProducts(facilityId))
        .thenReturn(null);
    when(stockManagementDraftRepository.findByFacilityId(facilityId))
        .thenReturn(null);
    Requisition requisition = createRequisition();
    when(requisitionRepository.findByFacilityIdAndStatus(facilityId, RequisitionStatus.INITIATED))
        .thenReturn(singletonList(requisition));

    archiveProductService.archiveProduct(facilityId, orderableId);

    verify(requisitionRepository).save(requisition);
  }

  @Test
  public void shouldActivateProduct() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).withOrderable(orderableId)
        .build();
    when(stockCardRepository.findByFacilityIdAndOrderableId(facilityId, orderableId)).thenReturn(
        singletonList(stockCard));
    when(stockCardExtensionRepository.findByStockCardId(stockCard.getId()))
        .thenReturn(new StockCardExtensionDataBuilder().withStockCardId(stockCard.getId())
            .withArchived(true).build());

    archiveProductService.activateProduct(facilityId, orderableId);

    verify(stockCardExtensionRepository).save(stockCardExtensionArgumentCaptor.capture());
    StockCardExtension stockCardExtension = stockCardExtensionArgumentCaptor.getValue();
    assertFalse(stockCardExtension.isArchived());
  }

  @Test
  public void shouldActivateArchivedProducts() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).build();
    stockCard.setId(stockCardId);
    when(stockCardRepository.findByOrderableIdInAndFacilityId(any(), any()))
        .thenReturn(singletonList(stockCard));
    StockCardExtension stockCardExtension = new StockCardExtensionDataBuilder()
        .withStockCardId(stockCardId).withArchived(true).build();
    when(stockCardExtensionRepository.findByStockCardIdIn(Sets.newHashSet(stockCardId)))
        .thenReturn(singletonList(stockCardExtension));

    archiveProductService.activateArchivedProducts(Sets.newHashSet(orderableId), facilityId);

    verify(stockCardExtensionRepository).save(singletonList(stockCardExtension));
    assertFalse(stockCardExtension.isArchived());
  }

  @Test
  public void shouldReturnTrueIfStockCardIsNotArchived() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).build();
    stockCard.setId(stockCardId);
    StockCardExtension stockCardExtension = new StockCardExtensionDataBuilder()
        .withStockCardId(stockCardId).withArchived(false).build();
    when(stockCardExtensionRepository.findByStockCardId(stockCardId))
        .thenReturn(stockCardExtension);

    assertTrue(archiveProductService.isNotArchived(stockCard));
  }

  @Test
  public void shouldReturnFalseIfStockCardIsArchived() {
    StockCard stockCard = new StockCardDataBuilder(new StockEvent()).build();
    stockCard.setId(stockCardId);
    StockCardExtension stockCardExtension = new StockCardExtensionDataBuilder()
        .withStockCardId(stockCardId).withArchived(true).build();
    when(stockCardExtensionRepository.findByStockCardId(stockCardId))
        .thenReturn(stockCardExtension);

    assertFalse(archiveProductService.isNotArchived(stockCard));
  }

  @Test
  public void shouldReturnArchivedProductsWhenSearch() {
    Set<String> archivedProducts = Sets.newHashSet(orderableId.toString());
    when(stockCardExtensionRepository.findArchivedProducts(facilityId))
        .thenReturn(archivedProducts);

    Set<String> archivedProductsReturn = archiveProductService.searchArchivedProducts(facilityId);

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
