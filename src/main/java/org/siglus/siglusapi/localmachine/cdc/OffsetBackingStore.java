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
