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

package org.openlmis.requisition.utils;

import static org.openlmis.requisition.CurrencyConfig.currencyCode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

/**
 * MoneyDeserializer class represents the deserializer for Joda-Money.
 */
public class MoneyDeserializer extends JsonDeserializer<Money> {

  @Override
  public Money deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException {

    String currencyString = currencyCode;
    return Money.parse(
        CurrencyUnit.of(currencyString).getCode() + " " + jsonParser.getText());
  }
}
