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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.ShipmentLineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor
public class ImportedShipmentLineItemData {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImportedShipmentLineItemData.class);

  @Getter
  private List<ShipmentLineItem> lineItems = new ArrayList<>();

  @Getter
  private List<Map> rowsWithUnresolvedOrderable = new ArrayList<>();

  /**
   * returns a json representation of the list of rows with unknown products.
   *
   * @return String
   */
  public String getRowsWithUnresolvedOrderableAsString() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(this.rowsWithUnresolvedOrderable);
    } catch (JsonProcessingException exp) {
      LOGGER.warn(exp.getMessage(), exp);
      return "";
    }
  }

  /**
   * Adds a line item.
   */
  public void addLineItem(ShipmentLineItem lineItem) {
    lineItems.add(lineItem);
  }

  /**
   * Adds a map to the unresolved orderable map.
   *
   * @param row map
   */
  public void addUnresolvedRowData(Map<String, String> row) {
    rowsWithUnresolvedOrderable.add(row);
  }
}
