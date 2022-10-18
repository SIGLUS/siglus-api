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

public class EventResource implements AutoCloseable {

  private final ExternalEventDtoMapper mapper;
  private final int capacityBytes;
  private EventWriter eventWriter;
  private DataOutputStream out;
  private ByteArrayOutputStream dest;

  public EventResource(int capacityBytes, ExternalEventDtoMapper mapper) {
    this.capacityBytes = capacityBytes;
    this.mapper = mapper;
  }

  public boolean write(Event event) throws IOException {
    prepare();
    boolean isFull = out.size() >= capacityBytes;
    if (isFull) {
      return false;
    }
    this.eventWriter.write(event);
    this.eventWriter.flush();
    return true;
  }

  public ByteArrayResource toResource() {
    return new ByteArrayResource(dest.toByteArray()) {
      @Override
      public String getFilename() {
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
      this.dest.reset();
      this.eventWriter = null;
      this.out = null;
    }
  }

  public boolean hasData() {
    return this.out != null && this.out.size() > 0;
  }

  private void prepare() {
    if (eventWriter == null) {
      dest = new ByteArrayOutputStream();
      out = new DataOutputStream(dest);
      eventWriter = new EventWriter(new EntryWriter(new DeflaterOutputStream(out)), mapper);
    }
  }
}
