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

import static org.openlmis.fulfillment.domain.Shipment.ROWS_WITH_UNRESOLVED_ORDERABLE;
import static org.openlmis.fulfillment.i18n.MessageKeys.ERROR_MISSING_REQUIRED_COLUMN;
import static org.openlmis.fulfillment.i18n.MessageKeys.ORDER_NOT_FOUND;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_LINE_ITEMS_REQUIRED;
import static org.openlmis.fulfillment.i18n.MessageKeys.SHIPMENT_ORDER_DUPLICATE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.openlmis.fulfillment.domain.CreationDetails;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.Order;
import org.openlmis.fulfillment.domain.Shipment;
import org.openlmis.fulfillment.repository.OrderRepository;
import org.openlmis.fulfillment.repository.ShipmentRepository;
import org.openlmis.fulfillment.service.FulfillmentException;
import org.openlmis.fulfillment.util.DateHelper;
import org.openlmis.fulfillment.util.FileColumnKeyPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@NoArgsConstructor
public class ShipmentBuilder {

  @Value("${shipment.shippedById}")
  private UUID shippedById;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ShipmentRepository shipmentRepository;

  @Autowired
  private DateHelper dateHelper;

  @Autowired
  private ShipmentLineItemBuilder lineItemBuilder;

  /**
   * Creates a shipment domain object from parsed shipment csv data.
   */
  public Shipment build(FileTemplate template, List<CSVRecord> lines) {
    if (lines.isEmpty()) {
      throw new FulfillmentException(SHIPMENT_LINE_ITEMS_REQUIRED);
    }

    FileColumn orderColumn = getOrderIdentifierColumn(template);

    if (orderColumn == null) {
      throw new FulfillmentException(ERROR_MISSING_REQUIRED_COLUMN, "Order Code/Order ID");
    }
    //find the order number
    CSVRecord firstRow = lines.get(0);
    String orderIdentifier = firstRow.get(orderColumn.getPosition());
    Order order = (FileColumnKeyPath.ORDER_CODE.equals(orderColumn.getFileColumnKeyPathEnum()))
        ? orderRepository.findByOrderCode(orderIdentifier) :
        orderRepository.findOne(UUID.fromString(orderIdentifier));

    if (order == null) {
      throw new FulfillmentException(ORDER_NOT_FOUND, orderIdentifier);
    }

    Page<Shipment> shipment = shipmentRepository.findByOrder(order, new PageRequest(0, 100));

    if (shipment.hasContent()) {
      throw new FulfillmentException(SHIPMENT_ORDER_DUPLICATE, orderIdentifier);
    }

    ImportedShipmentLineItemData result = lineItemBuilder.build(template, lines);

    Map<String, String> extraData = new HashMap<>();
    extraData.put(ROWS_WITH_UNRESOLVED_ORDERABLE, result.getRowsWithUnresolvedOrderableAsString());
    return new Shipment(order,
        new CreationDetails(shippedById,
            dateHelper.getCurrentDateTimeWithSystemZone()), null, result.getLineItems(), extraData);
  }

  private FileColumn getOrderIdentifierColumn(FileTemplate template) {
    return template
        .getFileColumns().stream()
        .filter(i -> FileColumnKeyPath.ORDER_COLUMN_PATHS
            .contains(i.getFileColumnKeyPathEnum())).findFirst()
        .orElse(null);

  }

}
