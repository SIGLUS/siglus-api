package org.siglus.siglusapi.localmachine.cdc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;

@Slf4j
public class OffsetBackingStore extends MemoryOffsetBackingStore {

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

  private void load() {
    List<CdcOffsetBacking> offsetBackings = OffsetBackingStoreWrapper.getRepository().findAll();
    if (CollectionUtils.isEmpty(offsetBackings)) {
      log.info("offset backing is empty");
      return;
    }
    offsetBackings.forEach(offsetBacking -> {
      data = new HashMap<>();
      ByteBuffer key = (offsetBacking.getOffsetKey() != null) ? ByteBuffer.wrap(offsetBacking.getOffsetKey()) : null;
      ByteBuffer value =
          (offsetBacking.getOffsetValue() != null) ? ByteBuffer.wrap(offsetBacking.getOffsetValue()) : null;
      data.put(key, value);
    });
  }

  @Override
  protected void save() {
    // TODO: 2022/9/23 存整体 entryset
    List<CdcOffsetBacking> offsetBackings = Lists.newArrayList();
    for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : data.entrySet()) {
      byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
      byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
      offsetBackings.add(CdcOffsetBacking.builder()
          .id(UUID.randomUUID())
          .offsetKey(key)
          .offsetValue(value)
          .build());
    }
    OffsetBackingStoreWrapper.getRepository().deleteAll();
    OffsetBackingStoreWrapper.getRepository().save(offsetBackings);
  }
}
