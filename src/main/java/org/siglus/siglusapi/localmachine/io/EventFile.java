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

package org.siglus.siglusapi.localmachine.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;

public class EventFile implements AutoCloseable {

  private final ExternalEventDtoMapper mapper;
  private final int capacityBytes;
  private File file;
  private EventWriter eventWriter;
  private DataOutputStream out;
  private int count = 0;

  public EventFile(int capacityBytes, String fileName, ExternalEventDtoMapper mapper) {
    this.capacityBytes = capacityBytes;
    this.file = new File(fileName);
    this.mapper = mapper;
  }

  public int writeEventAndGetRemainingCapacity(Event event) throws IOException {
    prepare();
    boolean isFull = out.size() >= capacityBytes;
    if (isFull) {
      throw new OutOfCapacityException();
    }
    this.eventWriter.write(event);
    this.eventWriter.flush();
    this.count += 1;
    return capacityBytes - this.out.size();
  }

  public void renameTo(String fileName) throws IOException {
    if (this.file.getName().equals(fileName)) {
      return;
    }
    File newFile = new File(fileName);
    if (!this.file.renameTo(newFile)) {
      throw new IOException("fail to rename file");
    }
    // ignore delete failure
    this.file.delete();
    this.file = newFile;
  }

  public File getFile() {
    return file;
  }

  @Override
  public void close() throws IOException {
    this.reset();
  }

  public int getCount() {
    return count;
  }

  void reset() throws IOException {
    if (this.eventWriter != null) {
      this.eventWriter.flush();
      this.eventWriter.close();
      this.eventWriter = null;
      this.out = null;
      this.count = 0;
    }
  }

  private void prepare() throws IOException {
    if (eventWriter == null) {
      out = new DataOutputStream(new DeflaterOutputStream(new FileOutputStream(this.file), true));
      eventWriter = new EventWriter(new EntryWriter(out), mapper);
    }
  }
}
