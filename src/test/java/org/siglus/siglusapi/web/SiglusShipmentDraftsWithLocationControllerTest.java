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

package org.siglus.siglusapi.web;

import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.fulfillment.web.shipmentdraft.ShipmentDraftDto;
import org.siglus.siglusapi.service.SiglusShipmentDraftService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusShipmentDraftsWithLocationControllerTest {
  @InjectMocks
  private SiglusShipmentDraftsWithLocationController siglusShipmentDraftsWithLocationController;

  @Mock
  private SiglusShipmentDraftService siglusShipmentDraftService;

  private final UUID draftId = UUID.randomUUID();

  @Test
  public void shouldGetShipmentDraftByLocation() {
    // when
    siglusShipmentDraftsWithLocationController.getShipmentDraftByLocation(draftId);

    // then
    verify(siglusShipmentDraftService).getShipmentDraftByLocation(draftId);
  }

  @Test
  public void shouldUpdateShipmentDraftByLocation() {
    // given
    ShipmentDraftDto draftDto = new ShipmentDraftDto();

    // when
    siglusShipmentDraftsWithLocationController.updateShipmentDraftByLocation(draftId, draftDto);

    // then
    verify(siglusShipmentDraftService).updateShipmentDraftByLocation(draftId, draftDto);
  }

  @Test
  public void shouldDeleteShipmentDraftByLocation() {
    // when
    siglusShipmentDraftsWithLocationController.deleteShipmentDraftByLocation(draftId);

    // then
    verify(siglusShipmentDraftService).deleteShipmentDraftByLocation(draftId);
  }
}
