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
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;

public class EntryWriter implements AutoCloseable {

  private final DataOutputStream out;
  private final CRC32 crc32 = new CRC32();

  public EntryWriter(OutputStream out) {
    this.out = new DataOutputStream(out);
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  public void write(Entry entry) throws IOException {
    crc32.reset();
    out.writeByte(Entry.MAGIC_NUMBER);
    crc32.update(entry.getData());
    out.writeLong(crc32.getValue());
    out.writeInt(entry.getData().length);
    out.write(entry.getData());
  }

  public void flush() throws IOException {
    out.flush();
  }
}
