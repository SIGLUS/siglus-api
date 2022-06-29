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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.siglus.siglusapi.dto.StockMovementResDto;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class SiglusStockMovementControllerTest {

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();
  @Mock
  private SiglusStockCardService service;

  @InjectMocks
  private SiglusStockMovementController controller;
  private UUID facilityId;

  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    facilityId = UUID.randomUUID();
  }

  @Test
  public void shouldGetStockMovementByFacilityId() {
    when(service.getProductMovements(facilityId, null, null, null))
        .thenReturn(new LinkedList());
    ResponseEntity<List<StockMovementResDto>> responseEntity =
        controller.getStockMovement(facilityId, null, null, null);
    assertEquals(OK, responseEntity.getStatusCode());
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenOrderableIdAndFacilityIdAreNull() throws ValidationMessageException {
    controller.getStockMovement(null, null, null, null);
  }
}
