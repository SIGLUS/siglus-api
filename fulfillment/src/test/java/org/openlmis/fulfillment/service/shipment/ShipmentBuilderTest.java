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

package org.openlmis.fulfillment.service.shipment;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.csv.CSVRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.fulfillment.FileColumnBuilder;
import org.openlmis.fulfillment.FileTemplateBuilder;
import org.openlmis.fulfillment.OrderDataBuilder;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.domain.TemplateType;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentRepository;
import org.openlmis.fulfillment.service.FulfillmentException;
import org.openlmis.fulfillment.service.PageDto;
import org.openlmis.fulfillment.testutils.ShipmentDataBuilder;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.util.FileColumnKeyPath;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CSVRecord.class)
public class ShipmentBuilderTest {

  private static final String ORDER_CODE = "O0001";
  private static final UUID SHIPPED_BY_ID = UUID.randomUUID();

  private static final String BATCH_NUMBER_FIELD_KEY = "batchNumber";

  @Value("${shipment.shippedById}")
  UUID shippedById;

  @Mock
  OrderRepository orderRepository;

  @Mock
  DateHelper dateHelper;

  @Mock
  ShipmentRepository shipmentRepository;

  @Mock
  ShipmentLineItemBuilder lineItemBuilder;

  @InjectMocks
  ShipmentBuilder builderService;

  private Order order;

  private CSVRecord csvRecord = PowerMockito.mock(CSVRecord.class);

  @Before
  public void setup() {
    order = new OrderDataBuilder().withOrderCode(ORDER_CODE).build();

    ReflectionTestUtils.setField(builderService, "shippedById",
        SHIPPED_BY_ID);
    ImportedShipmentLineItemData result = new ImportedShipmentLineItemData();
    when(lineItemBuilder.build(any(), any())).thenReturn(result);
    when(csvRecord.get(0)).thenReturn(ORDER_CODE);
    when(shipmentRepository.findByOrder(any(), any())).thenReturn(new PageDto<>());
  }

  @Test(expected = FulfillmentException.class)
  public void shouldThrowFulfillmentExceptionWhenParsedDataIsEmpty() {
    FileTemplate template = mockTemplate(false);
    List<CSVRecord> parsedData = new ArrayList<>();

    builderService.build(template, parsedData);
  }


  @Test(expected = FulfillmentException.class)
  public void shouldThrowFulfillmentExceptionWhenOrderIsNotFound() {
    FileTemplate template = mockTemplate(false);
    when(orderRepository.findByOrderCode(ORDER_CODE)).thenReturn(null);

    builderService.build(template, asList(csvRecord));
  }

  @Test
  public void shouldCreateShipmentWithRequiredFieldsProperties() {
    FileTemplate template = mockTemplate(false);
    when(orderRepository.findByOrderCode(ORDER_CODE)).thenReturn(order);

    Shipment shipment = builderService.build(template, asList(csvRecord));

    assertThat(shipment.getOrder(), is(equalTo(order)));
  }

  @Test(expected = FulfillmentException.class)
  public void shouldThrowFulfillmentExceptionWhenShipmentAlreadyExists() {
    FileTemplate template = mockTemplate(false);
    when(orderRepository.findByOrderCode(ORDER_CODE)).thenReturn(order);
    Shipment persistedShipment = new ShipmentDataBuilder().build();
    when(shipmentRepository.findByOrder(any(), any()))
        .thenReturn(new PageImpl<>(asList(persistedShipment)));

    builderService.build(template, asList(csvRecord));
  }


  private FileTemplate mockTemplate(Boolean includeExtraData) {
    FileTemplateBuilder templateBuilder = new FileTemplateBuilder();
    FileColumnBuilder columnBuilder = new FileColumnBuilder();

    FileColumn orderCode = columnBuilder
        .withPosition(0).withNested("order")
        .withKeyPath(FileColumnKeyPath.ORDER_CODE.toString()).build();
    FileColumn orderableId = columnBuilder
        .withPosition(1).withNested("lineItem")
        .withKeyPath(FileColumnKeyPath.ORDERABLE_ID.toString()).build();
    FileColumn quantityShipped = columnBuilder
        .withPosition(2).withNested("lineItem")
        .withKeyPath(FileColumnKeyPath.QUANTITY_SHIPPED.toString()).build();
    FileColumn batchNumber = columnBuilder
        .withPosition(3).withNested("lineItem")
        .withKeyPath(BATCH_NUMBER_FIELD_KEY).build();

    return templateBuilder
        .withTemplateType(TemplateType.SHIPMENT)
        .withFileColumns(
            (includeExtraData)
                ? asList(orderCode, orderableId, quantityShipped, batchNumber) :
                asList(orderCode, orderableId, quantityShipped))
        .build();
  }

}