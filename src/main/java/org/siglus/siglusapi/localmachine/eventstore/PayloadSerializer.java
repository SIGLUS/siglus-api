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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.money.Money;
import org.siglus.siglusapi.localmachine.EventPayload;
import org.siglus.siglusapi.localmachine.utils.MoneyDeserializer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PayloadSerializer {
  public static final ObjectMapper LOCALMACHINE_EVENT_OBJECT_MAPPER;
  // in case the event class may be moved to other packages causing class name not match the one of
  // event in db. here to maintain the class name mapping.
  private static final Map<String, Class<?>> payloadNameToClass = new HashMap<>();
  private static final Map<Class<?>, String> payloadClassToName = new HashMap<>();

  static {
    LOCALMACHINE_EVENT_OBJECT_MAPPER = new ObjectMapper();
    LOCALMACHINE_EVENT_OBJECT_MAPPER.registerModule(new JavaTimeModule());
    LOCALMACHINE_EVENT_OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    LOCALMACHINE_EVENT_OBJECT_MAPPER.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Money.class, new MoneyDeserializer());
    LOCALMACHINE_EVENT_OBJECT_MAPPER.registerModule(module);
  }

  public PayloadSerializer() {}

  @SneakyThrows
  public byte[] dump(Object payload) {
    byte[] payloadBytes = LOCALMACHINE_EVENT_OBJECT_MAPPER.writeValueAsBytes(payload);
    PayloadWrapper payloadWrapper = new PayloadWrapper(getName(payload), payloadBytes);
    return LOCALMACHINE_EVENT_OBJECT_MAPPER.writeValueAsBytes(payloadWrapper);
  }

  @SneakyThrows
  public Object load(byte[] payload) {
    PayloadWrapper payloadWrapper =
        LOCALMACHINE_EVENT_OBJECT_MAPPER.readValue(payload, PayloadWrapper.class);
    return LOCALMACHINE_EVENT_OBJECT_MAPPER.readValue(
        payloadWrapper.getPayload(), getPayloadClass(payloadWrapper));
  }

  @PostConstruct
  public void scanPayloadClasses() {
    ClassPathScanningCandidateComponentProvider provider =
        new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AnnotationTypeFilter(EventPayload.class));
    Set<BeanDefinition> payloadClasses = provider.findCandidateComponents("org.siglus.siglusapi");
    payloadClasses.forEach(
        it -> {
          try {
            Class<?> payloadClass = Class.forName(it.getBeanClassName());
            String payloadClassSimpleName = payloadClass.getSimpleName();
            payloadNameToClass.put(payloadClassSimpleName, payloadClass);
            payloadClassToName.put(payloadClass, payloadClassSimpleName);
          } catch (ClassNotFoundException e) {
            log.error("class {} not found", it.getBeanClassName());
            throw new IllegalStateException(e);
          }
        });
    log.info("find event payload classes:{}", payloadNameToClass);
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
