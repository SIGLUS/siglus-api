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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.joda.money.Money;

@Slf4j
public class EventPayloadCheckUtils {

  public static int checkEventSerializeChanges(Object event, Class clazz) {
    // TODO: use same objectMapper in PayloadSerializer
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Money.class, new MoneyDeserializer());
    objectMapper.registerModule(module);

    String json = null;
    try {
      json = objectMapper.writeValueAsString(event);

      Object eventRead = objectMapper.readValue(json, clazz);

      Javers javers = JaversBuilder.javers()
          .registerValue(BigDecimal.class, (a, b) -> a.compareTo(b) == 0)
          .build();

      Diff compare = javers.compare(event, eventRead);

      log.info("hasChanges = " + compare.hasChanges());
      log.info(compare.toString());
      return compare.getChanges().size();
    } catch (IOException e) {
      log.error("fail to parse the event", e);
      throw (IllegalStateException) (new IllegalStateException().initCause(e));
    }
  }

}
