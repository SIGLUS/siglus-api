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

import static java.lang.Long.parseLong;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNumeric;
import static org.openlmis.fulfillment.util.FileColumnKeyPath.ALL_REQUIRED_COLUMN_PATHS;
import static org.openlmis.fulfillment.util.FileColumnKeyPath.ORDERABLE_COLUMN_PATHS;
import static org.openlmis.fulfillment.util.FileColumnKeyPath.ORDER_COLUMN_PATHS;
import static org.openlmis.fulfillment.util.FileColumnKeyPath.PRODUCT_CODE;
import static org.openlmis.fulfillment.util.FileColumnKeyPath.QUANTITY_SHIPPED_PATHS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;
import org.openlmis.fulfillment.domain.FileColumn;
import org.openlmis.fulfillment.domain.FileTemplate;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.openlmis.fulfillment.domain.VersionEntityReference;
import org.openlmis.fulfillment.service.FulfillmentException;
import org.openlmis.fulfillment.service.referencedata.OrderableDto;
import org.openlmis.fulfillment.service.referencedata.OrderableReferenceDataService;
import org.openlmis.fulfillment.web.util.VersionIdentityDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShipmentLineItemBuilder {

  private static final String LINE_ITEM = "lineItem";

  @Autowired
  private OrderableReferenceDataService orderableService;

  /**
   * Builds shipment line item objects from parsed CSV data.
   *
   * @param template file template used for parsing
   * @param lines data read from the csv.
   * @return List of ShipmentLineItems
   */
  public ImportedShipmentLineItemData build(FileTemplate template, List<CSVRecord> lines) {
    // find required columns.
    FileColumn orderableColumn = template.findColumn(ORDERABLE_COLUMN_PATHS).orElse(null);
    FileColumn orderColumn = template.findColumn(ORDER_COLUMN_PATHS).orElse(null);
    FileColumn quantityShippedColumn = template.findColumn(QUANTITY_SHIPPED_PATHS).orElse(null);

    if (orderColumn == null || orderableColumn == null || quantityShippedColumn == null) {
      throw new FulfillmentException(
          "Required shipment template columns not found.");
    }

    ImportedShipmentLineItemData result = new ImportedShipmentLineItemData();
    // Initialize and cache variables that would be used repeatedly for each row.
    List<OrderableDto> orderables = orderableService.findAll();
    Map<String, OrderableDto> orderableDtoMap = orderables
        .stream()
        .collect(toMap(orderable -> (PRODUCT_CODE.equals(orderableColumn.getFileColumnKeyPathEnum())
            ? orderable.getProductCode()
            : orderable.getId().toString()),
            orderable -> orderable));

    List<FileColumn> extraDataFields = template.getFileColumns()
        .stream()
        .filter(column -> !ALL_REQUIRED_COLUMN_PATHS
            .contains(column.getFileColumnKeyPathEnum())
            && LINE_ITEM.equals(column.getNested()))
        .collect(toList());

    // read the first order identifier to check subsequent order identifiers against it.
    String orderIdentifier = lines.get(0).get(orderColumn.getPosition());

    for (CSVRecord row : lines) {
      validateOrderIdentifier(orderColumn, orderIdentifier, row);

      VersionIdentityDto orderable =
          extractOrderableIdentity(orderableColumn, row, orderableDtoMap);

      if (orderable == null) {
        result.addUnresolvedRowData(row.toMap());
        continue;
      }
      String quantityShippedString = row.get(quantityShippedColumn.getPosition());
      validateOrderableAndQuantity(orderable, quantityShippedString);

      Long quantityShipped = parseLong(quantityShippedString);
      Map<String, String> extraData = extractExtraData(extraDataFields, row);

      ShipmentLineItem lineItem = new ShipmentLineItem(
          new VersionEntityReference(orderable.getId(), orderable.getVersionNumber()),
          quantityShipped, extraData);
      result.addLineItem(lineItem);
    }
    return result;
  }

  private void validateOrderIdentifier(FileColumn orderColumn, String orderIdentifier,
      CSVRecord row) {
    String orderIdForRow = row.get(orderColumn.getPosition());
    if (!orderIdentifier.equals(orderIdForRow)) {
      throw new FulfillmentException("Shipment file contains inconsistent order numbers.");
    }
  }

  private void validateOrderableAndQuantity(VersionIdentityDto orderable,
      String quantityShippedString) {
    if (orderable == null) {
      throw new FulfillmentException("Orderable not found for line Item.");
    }
    if (isEmpty(quantityShippedString) || !isNumeric(quantityShippedString)) {
      throw new FulfillmentException("Quantity Shipped value should be a valid number.");
    }
    long quantityShipped = parseLong(quantityShippedString);
    if (quantityShipped < 0) {
      throw new FulfillmentException(
          "Quantity Shipped should be number greater than or equal to 0.");
    }
  }

  private VersionIdentityDto extractOrderableIdentity(FileColumn orderableColumn, CSVRecord row,
      Map<String, OrderableDto> orderableDtoMap) {
    String orderableIdentifier = row.get(orderableColumn.getPosition());
    OrderableDto orderableDto = orderableDtoMap.get(orderableIdentifier);
    return (orderableDto != null) ? orderableDto.getIdentity() : null;
  }

  private Map<String, String> extractExtraData(List<FileColumn> extraDataFields, CSVRecord row) {
    Map<String, String> extraData = new HashMap<>();
    if (!extraDataFields.isEmpty()) {
      for (FileColumn column : extraDataFields) {
        extraData.put(column.getKeyPath(), row.get(column.getPosition()));
      }
    }
    return extraData;
  }

}
