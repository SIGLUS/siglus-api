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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;

public class MoneyDeserializerTest {

  private ObjectMapper mapper;
  private MoneyDeserializer moneyDeserializer;

  @Before
  public void setup() {
    mapper = new ObjectMapper();
    moneyDeserializer = new MoneyDeserializer();
  }

  @Test(expected = NumberFormatException.class)
  public void shouldNotDeserializeMoneyWhenValueEmpty() throws IOException {
    String json = String.format("{\"value\":%s}", "\"\"");
    deserializeMoney(json);
  }

  @Test
  public void shouldDeserializeMoney() throws IOException {
    String json = String.format("{\"value\":%s}", "\"11.33\"");
    Money money = deserializeMoney(json);

    assertEquals(new BigDecimal("11.33"), money.getAmount());
    assertEquals(CurrencyUnit.USD, money.getCurrencyUnit());
  }

  private Money deserializeMoney(String json) throws IOException {
    InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    JsonParser parser = mapper.getFactory().createParser(stream);
    parser.nextValue();
    parser.nextValue();
    DeserializationContext ctxt = mapper.getDeserializationContext();

    return moneyDeserializer.deserialize(parser, ctxt);
  }
}