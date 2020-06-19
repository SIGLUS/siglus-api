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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.web.shipment.ShipmentLineItemDto;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.openlmis.referencedata.util.Pagination;
import org.siglus.siglusapi.domain.ShipmentDraftLineItemExtension;
import org.siglus.siglusapi.repository.ShipmentDraftLineItemExtensionRepository;
import org.siglus.siglusapi.service.client.SiglusShipmentDraftFulfillmentService;
import org.springframework.data.domain.Pageable;

@RunWith(MockitoJUnitRunner.class)
public class SiglusShipmentDraftServiceTest {

  @Captor
  private ArgumentCaptor<List<ShipmentDraftLineItemExtension>> lineItemExtensionsArgumentCaptor;

  @InjectMocks
  private SiglusShipmentDraftService siglusShipmentDraftService;

  @Mock
  private SiglusShipmentDraftFulfillmentService siglusShipmentDraftFulfillmentService;

  @Mock
  private ShipmentDraftLineItemExtensionRepository lineItemExtensionRepository;

  @Mock
  private Pageable pageable;

  private UUID draftId = UUID.randomUUID();

  private UUID orderId = UUID.randomUUID();

  private UUID lineItemId = UUID.randomUUID();

  @Test
  public void shouldUpdateLineItemExtensionWhenUpdateShipmentDraft() {
    // given
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(true);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    ShipmentLineItemDto updatedLineItemDto = new ShipmentLineItemDto();
    updatedLineItemDto.setId(lineItemId);
    updatedLineItemDto.setSkipped(false);
    ShipmentDraftDto updatedDraftDto = new ShipmentDraftDto();
    updatedDraftDto.setLineItems(newArrayList(updatedLineItemDto));
    when(siglusShipmentDraftFulfillmentService.updateShipmentDraft(draftId, draftDto))
        .thenReturn(updatedDraftDto);
    ShipmentDraftLineItemExtension extension = ShipmentDraftLineItemExtension.builder()
        .shipmentDraftLineItemId(lineItemId)
        .skipped(false)
        .build();
    when(lineItemExtensionRepository.findByShipmentDraftLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(extension));

    // when
    updatedDraftDto = siglusShipmentDraftService.updateShipmentDraft(draftId, draftDto);

    // then
    verify(lineItemExtensionRepository).save(lineItemExtensionsArgumentCaptor.capture());
    List<ShipmentDraftLineItemExtension> lineItemExtensions = lineItemExtensionsArgumentCaptor
        .getValue();
    lineItemExtensions.forEach(lineItemExtension -> assertTrue(lineItemExtension.isSkipped()));
    updatedDraftDto.lineItems().forEach(lineItem -> assertTrue(lineItem.isSkipped()));
  }

  @Test
  public void shouldCreateLineItemExtensionWhenUpdateShipmentDraft() {
    // given
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(true);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    ShipmentLineItemDto updatedLineItemDto = new ShipmentLineItemDto();
    updatedLineItemDto.setId(lineItemId);
    updatedLineItemDto.setSkipped(false);
    ShipmentDraftDto updatedDraftDto = new ShipmentDraftDto();
    updatedDraftDto.setLineItems(newArrayList(updatedLineItemDto));
    when(siglusShipmentDraftFulfillmentService.updateShipmentDraft(draftId, draftDto))
        .thenReturn(updatedDraftDto);
    when(lineItemExtensionRepository.findByShipmentDraftLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList());

    // when
    updatedDraftDto = siglusShipmentDraftService.updateShipmentDraft(draftId, draftDto);

    // then
    verify(lineItemExtensionRepository).save(lineItemExtensionsArgumentCaptor.capture());
    List<ShipmentDraftLineItemExtension> lineItemExtensions = lineItemExtensionsArgumentCaptor
        .getValue();
    lineItemExtensions.forEach(lineItemExtension -> assertTrue(lineItemExtension.isSkipped()));
    updatedDraftDto.lineItems().forEach(lineItem -> assertTrue(lineItem.isSkipped()));
  }

  @Test
  public void shouldSetLineItemExtensionWhenSearchShipmentDrafts() {
    // given
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(false);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    when(siglusShipmentDraftFulfillmentService.searchShipmentDrafts(orderId, pageable))
        .thenReturn(Pagination.getPage(newArrayList(draftDto)));
    ShipmentDraftLineItemExtension extension = ShipmentDraftLineItemExtension.builder()
        .shipmentDraftLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByShipmentDraftLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(extension));

    // when
    siglusShipmentDraftService.searchShipmentDrafts(orderId, pageable);

    // then
    assertTrue(lineItemDto.isSkipped());
  }

  @Test
  public void shouldSetLineItemExtensionWhenSearchShipmentDraft() {
    // given
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(false);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    final Set<String> expand = newHashSet();
    when(siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId, expand))
        .thenReturn(draftDto);
    ShipmentDraftLineItemExtension extension = ShipmentDraftLineItemExtension.builder()
        .shipmentDraftLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByShipmentDraftLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(extension));

    // when
    siglusShipmentDraftService.searchShipmentDraft(draftId, expand);

    // then
    assertTrue(lineItemDto.isSkipped());
  }

  @Test
  public void shouldDeleteLineItemExtensionWhenDeleteShipmentDraft() {
    // given
    ShipmentLineItemDto lineItemDto = new ShipmentLineItemDto();
    lineItemDto.setId(lineItemId);
    lineItemDto.setSkipped(false);
    ShipmentDraftDto draftDto = new ShipmentDraftDto();
    draftDto.setLineItems(newArrayList(lineItemDto));
    when(siglusShipmentDraftFulfillmentService.searchShipmentDraft(draftId, null))
        .thenReturn(draftDto);
    ShipmentDraftLineItemExtension extension = ShipmentDraftLineItemExtension.builder()
        .shipmentDraftLineItemId(lineItemId)
        .skipped(true)
        .build();
    when(lineItemExtensionRepository.findByShipmentDraftLineItemIdIn(newHashSet(lineItemId)))
        .thenReturn(newArrayList(extension));

    // when
    siglusShipmentDraftService.deleteShipmentDraft(draftId);

    // then
    verify(lineItemExtensionRepository).delete(lineItemExtensionsArgumentCaptor.capture());
    List<ShipmentDraftLineItemExtension> lineItemExtensions = lineItemExtensionsArgumentCaptor
        .getValue();
    assertEquals(1, lineItemExtensions.size());
  }

}
