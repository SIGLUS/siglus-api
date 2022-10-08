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

package org.siglus.siglusapi.localmachine.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.joda.money.Money;

public class MoneyDeserializer extends JsonDeserializer<Money> {

  public static final String AMOUNT = "amount";
  public static final String CURRENCY_UNIT = "currencyUnit";
  public static final String CURRENCY_CODE = "currencyCode";

  @Override
  public Money deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    String currencyCode = null;
    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    ObjectNode root = mapper.readTree(jp);
    DoubleNode amountNode = (DoubleNode) root.findValue(AMOUNT);
    String amount = null;
    if (null != amountNode) {
      amount = amountNode.asText();
    }
    JsonNode currencyUnitNode = root.get(CURRENCY_UNIT);
    if (currencyUnitNode != null) {
      JsonNode currencyCodeNode = currencyUnitNode.get(CURRENCY_CODE);
      if (currencyCodeNode != null) {
        currencyCode = currencyCodeNode.textValue();
      }
    }
    if (StringUtils.isBlank(amount) || StringUtils.isBlank(currencyCode)) {
      return null;
    }
    return Money.parse(currencyCode + " " + amount);
  }
}
