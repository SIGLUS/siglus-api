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

package org.openlmis.fulfillment.domain;

import java.math.BigInteger;
import org.openlmis.fulfillment.extension.point.OrderNumberGenerator;
import org.springframework.stereotype.Component;

/**
 * A default implementation of OrderNumberGenerator extension that encodes Order's id as Base36.
 * @see OrderNumberGenerator
 */
@Component("Base36EncodedOrderNumberGenerator")
public class Base36EncodedOrderNumberGenerator implements OrderNumberGenerator {

  /**
   * Generates unique number for given order.
   */
  public String generate(Order order) {
    String id = order.getExternalId().toString();
    String base36Id = new BigInteger(id.replace("-", ""), 16)
        .toString(36).toUpperCase();
    return base36Id.substring(0, 8);
  }
}
