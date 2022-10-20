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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.apache.kafka.connect.util.SafeObjectInputStream;

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
    log.info("Starting OffsetBackingStore");
    load();
  }

  @Override
  public synchronized void stop() {
    save();
    super.stop();
    // Nothing to do since this doesn't maintain any outstanding connections/data
    log.info("Stopped OffsetBackingStore");
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
    try (SafeObjectInputStream is = new SafeObjectInputStream(
        new ByteArrayInputStream(offsetBacking.getOffsetData()))) {
      Object obj = is.readObject();
      if (!(obj instanceof HashMap)) {
        throw new ConnectException("Expected HashMap but found " + obj.getClass());
      }
      Map<byte[], byte[]> raw = (Map<byte[], byte[]>) obj;
      data = new HashMap<>();
      for (Map.Entry<byte[], byte[]> mapEntry : raw.entrySet()) {
        ByteBuffer key = (mapEntry.getKey() != null) ? ByteBuffer.wrap(mapEntry.getKey()) : null;
        ByteBuffer value = (mapEntry.getValue() != null) ? ByteBuffer.wrap(mapEntry.getValue()) : null;
        data.put(key, value);
      }
    } catch (EOFException e) {
      // NoSuchFileException: Ignore, may be new.
      // EOFException: Ignore, this means the file was missing or corrupt
    } catch (IOException | ClassNotFoundException e) {
      throw new ConnectException(e);
    }
  }

  @SneakyThrows
  @Override
  protected void save() {
    log.info("save offset");
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (ObjectOutputStream os = new ObjectOutputStream(byteArrayOutputStream)) {
      Map<byte[], byte[]> raw = new HashMap<>();
      for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : data.entrySet()) {
        byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
        byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
        raw.put(key, value);
      }
      os.writeObject(raw);

      List<CdcOffsetBacking> offsetBackings = OffsetBackingStoreWrapper.getRepository().findAll();
      CdcOffsetBacking offsetBacking =
          CollectionUtils.isEmpty(offsetBackings) ? initCdcOffsetBacking() : offsetBackings.get(0);
      offsetBacking.setOffsetData(byteArrayOutputStream.toByteArray());
      OffsetBackingStoreWrapper.getRepository().save(offsetBacking);
    } catch (IOException e) {
      throw new ConnectException(e);
    }
  }

  private CdcOffsetBacking initCdcOffsetBacking() {
    return CdcOffsetBacking.builder().id(UUID.randomUUID()).build();
  }
}
