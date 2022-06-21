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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openlmis.stockmanagement.i18n.MessageKeys.ERROR_EVENT_ORDERABLE_INVALID;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
import org.openlmis.stockmanagement.dto.StockCardDto;
import org.siglus.siglusapi.dto.android.response.ProductMovementResponse;
import org.siglus.siglusapi.service.SiglusStockCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RunWith(MockitoJUnitRunner.class)
public class SiglusStockCardControllerTest {

  private static final String PRODUCT_ID = "id";

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  @Mock
  private SiglusStockCardService service;

  @InjectMocks
  private SiglusStockCardController controller;

  private UUID productId;

  private UUID stockCardId;

  private String facilityId;

  private String orderableId;

  private MultiValueMap<String, String> parameters;


  @Before
  public void prepare() {
    MockitoAnnotations.initMocks(this);
    productId = UUID.randomUUID();
    stockCardId = UUID.randomUUID();
    facilityId = UUID.randomUUID().toString();
    orderableId = UUID.randomUUID().toString();
    parameters = new LinkedMultiValueMap<>();
    parameters.add(PRODUCT_ID, productId.toString());
  }


  @Test
  public void shouldCallSiglusServiceFindStockCardByOrderable() {
    controller.searchStockCard(parameters);
    verify(service).findStockCardByOrderable(productId);
  }

  @Test
  public void shouldThrowExceptionWhenSearchStockCardGiven() {
    parameters.remove(PRODUCT_ID);
    expectedEx.expect(ValidationMessageException.class);
    expectedEx.expectMessage(ERROR_EVENT_ORDERABLE_INVALID);
    controller.searchStockCard(parameters);
  }

  @Test
  public void shouldGetResponseNotFound() {
    when(service.findStockCardByOrderable(productId)).thenReturn(null);
    ResponseEntity<StockCardDto> responseEntity = controller.searchStockCard(parameters);
    assertEquals(NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  public void shouldGetReponseOk() {
    when(service.findStockCardByOrderable(productId)).thenReturn(new StockCardDto());
    ResponseEntity<StockCardDto> responseEntity = controller.searchStockCard(parameters);
    assertEquals(OK, responseEntity.getStatusCode());
  }

  @Test
  public void shouldGetReponseNotFoundById() {
    when(service.findStockCardById(stockCardId)).thenReturn(null);
    ResponseEntity<StockCardDto> responseEntity = controller.searchStockCardById(stockCardId);
    assertEquals(NOT_FOUND, responseEntity.getStatusCode());
  }

  @Test
  public void shouldGetReponseOkById() {
    when(service.findStockCardById(stockCardId)).thenReturn(new StockCardDto());
    ResponseEntity<StockCardDto> responseEntity = controller.searchStockCardById(stockCardId);
    assertEquals(OK, responseEntity.getStatusCode());
  }

  @Test
  public void shouldGetStockMovementByFacilityIdAndOrderableId() {
    when(service.getProductMovements(facilityId, orderableId, null, null))
            .thenReturn(new LinkedList<ProductMovementResponse>());
    ResponseEntity<List<ProductMovementResponse>> responseEntity =
            controller.getStockMovementByFacilityIdAndOrderableId(facilityId, orderableId, null, null);
    assertEquals(OK, responseEntity.getStatusCode());
  }
}
