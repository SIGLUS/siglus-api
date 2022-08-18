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

package org.siglus.siglusapi.localmachine.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class PayloadSerializer {
  // TODO: 2022/8/17 scan siglus package to build the class name mapping below
  private static Map<String, Class<?>> payloadNameToClass = new HashMap<>();
  private static Map<Class<?>, String> payloadClassToName = new HashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @SneakyThrows
  public byte[] dump(Object payload) {
    byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
    PayloadWrapper payloadWrapper = new PayloadWrapper(getName(payload), payloadBytes);
    return objectMapper.writeValueAsBytes(payloadWrapper);
  }

  @SneakyThrows
  public Object load(byte[] payload) {
    PayloadWrapper payloadWrapper = objectMapper.readValue(payload, PayloadWrapper.class);
    return objectMapper.readValue(payloadWrapper.getPayload(), getPayloadClass(payloadWrapper));
  }

  private Class<?> getPayloadClass(PayloadWrapper payloadWrapper) throws ClassNotFoundException {
    String name = payloadWrapper.getName();
    return payloadNameToClass.getOrDefault(name, Class.forName(name));
  }

  private String getName(Object payload) {
    return payloadClassToName.getOrDefault(payload.getClass(), payload.getClass().getName());
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  static class PayloadWrapper {
    private String name;
    private byte[] payload;
  }
}
