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

package org.openlmis.referencedata.domain;

import static org.openlmis.referencedata.util.messagekeys.TradeItemMessageKeys.ERROR_GTIN_INVALID_LENGTH;
import static org.openlmis.referencedata.util.messagekeys.TradeItemMessageKeys.ERROR_GTIN_NUMERIC;

import javax.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.util.Message;

/**
 * Global Trade Item Number, associated with TradeItem.
 */
@Embeddable
@EqualsAndHashCode
public class Gtin {

  @Getter
  private String gtin;

  private Gtin() { }

  /**
   * Creates a new Gtin value.
   * @param gtin the gtin
   */
  public Gtin(String gtin) {
    if (!StringUtils.isNumeric(gtin)) {
      throw new ValidationMessageException(
          new Message(ERROR_GTIN_NUMERIC));
    }
    if (gtin.length() < 8 || gtin.length() > 14) {
      throw new ValidationMessageException(
          new Message(ERROR_GTIN_INVALID_LENGTH));
    }
    this.gtin = gtin;
  }
}
