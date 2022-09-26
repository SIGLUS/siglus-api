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

package org.siglus.siglusapi.localmachine.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.localmachine.utils.MoneyDeserializer;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class MoneyDeserializerTest {

  @InjectMocks
  private MoneyDeserializer moneyDeserializer;

  @Test
  public void shouldSuccessWhenEmitWithJson() throws IOException {
    moneyDeserializer = new MoneyDeserializer();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Money.class, moneyDeserializer);
    objectMapper.registerModule(module);
    Money money = Money.of(CurrencyUnit.USD, 100);
    String json = objectMapper.writeValueAsString(money);
    // when
    objectMapper.readValue(json, Money.class);
  }

  @Test
  public void shouldSuccessWhenEmitWithEmptyJson() throws IOException {
    moneyDeserializer = new MoneyDeserializer();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Money.class, moneyDeserializer);
    objectMapper.registerModule(module);
    // when
    objectMapper.readValue("{}", Money.class);
  }


}
