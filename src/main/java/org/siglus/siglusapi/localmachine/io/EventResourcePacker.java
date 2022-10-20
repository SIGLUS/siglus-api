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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.springframework.core.io.ByteArrayResource;

public class EventResourcePacker implements AutoCloseable {

  private final ExternalEventDtoMapper mapper;
  private final int capacityBytes;
  private EventWriter eventWriter;
  private DataOutputStream out;
  private ByteArrayOutputStream dest;

  public EventResourcePacker(int capacityBytes, ExternalEventDtoMapper mapper) {
    this.capacityBytes = capacityBytes;
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
    return capacityBytes - out.size();
  }

  public ByteArrayResource toResource() {
    return new ByteArrayResource(dest.toByteArray()) {
      @Override
      public String getFilename() {
        // tricky: need to set file name otherwise rest api call will fail
        return "events.dat";
      }
    };
  }

  @Override
  public void close() throws IOException {
    this.reset();
  }

  public void reset() throws IOException {
    if (this.eventWriter != null) {
      this.eventWriter.flush();
      this.eventWriter.close();
      this.eventWriter = null;
      this.dest = null;
      this.out = null;
    }
  }

  private void prepare() {
    if (eventWriter == null) {
      dest = new ByteArrayOutputStream();
      this.out = new DataOutputStream(new DeflaterOutputStream(dest, true));
      eventWriter = new EventWriter(new EntryWriter(this.out), mapper);
    }
  }
}
