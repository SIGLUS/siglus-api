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

package org.siglus.siglusapi.localmachine.cdc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;

@Slf4j
public class OffsetBackingStore extends MemoryOffsetBackingStore {

  public static final ObjectMapper OBJECT_MAPPER;

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
    OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    OBJECT_MAPPER.configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public synchronized void start() {
    super.start();
    log.info("Starting FileOffsetBackingStore");
    load();
  }

  @Override
  public synchronized void stop() {
    super.stop();
    // Nothing to do since this doesn't maintain any outstanding connections/data
    log.info("Stopped FileOffsetBackingStore");
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private void load() {
    List<CdcOffsetBacking> offsetBackings = OffsetBackingStoreWrapper.getRepository().findAll();
    if (CollectionUtils.isEmpty(offsetBackings)) {
      log.info("offset backing is empty");
      return;
    }
    CdcOffsetBacking offsetBacking = offsetBackings.get(0);
    Map<ByteBuffer, ByteBuffer> offsetBackingMap = OBJECT_MAPPER.readValue(offsetBacking.getOffsetData(), Map.class);
    data.putAll(offsetBackingMap);
  }

  @SneakyThrows
  @Override
  protected void save() {
    List<CdcOffsetBacking> offsetBackings = OffsetBackingStoreWrapper.getRepository().findAll();
    CdcOffsetBacking offsetBacking =
        CollectionUtils.isEmpty(offsetBackings) ? initCdcOffsetBacking() : offsetBackings.get(0);
    offsetBacking.setOffsetData(OBJECT_MAPPER.writeValueAsString(data));
    OffsetBackingStoreWrapper.getRepository().save(offsetBacking);
  }

  private CdcOffsetBacking initCdcOffsetBacking() {
    return CdcOffsetBacking.builder().id(UUID.randomUUID()).build();
  }
}
