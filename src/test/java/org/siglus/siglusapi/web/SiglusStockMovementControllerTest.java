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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.service.SiglusStockCardService;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockMovementControllerTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @Mock
  private SiglusStockCardService siglusStockCardService;

  @InjectMocks
  private SiglusStockMovementController controller;

  private UUID facilityId;

  private UUID orderableId;

  @Before
  public void prepare() {
    facilityId = UUID.randomUUID();
    orderableId = UUID.randomUUID();
  }

  @Test
  public void shouldCallGetProductMovementByServiceGivenFacilityIdAndOrderableId() {
    controller.getMovementByProduct(orderableId, facilityId);
    verify(siglusStockCardService).getMovementByProduct(facilityId, orderableId);
  }
}
