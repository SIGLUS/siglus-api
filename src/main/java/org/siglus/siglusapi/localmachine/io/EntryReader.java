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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.CRC32;
import org.apache.http.util.ByteArrayBuffer;

public class EntryReader implements AutoCloseable {

  private final DataInputStream in;
  private final CRC32 crc32 = new CRC32();
  private ByteArrayBuffer readBuffer;

  public EntryReader(InputStream in) {
    this.in = new DataInputStream(in);
  }

  public Entry read() throws IOException {
    byte magicNumber = in.readByte();
    if (Entry.MAGIC_NUMBER != magicNumber) {
      throw new IOException("invalid magic number " + magicNumber);
    }
    final long checksum = in.readLong();
    final int size = in.readInt();
    if (size <= 0 || size > Entry.MAX_SIZE_BYTES) {
      throw new IllegalArgumentException("invalid entry size " + size);
    }
    prepareBuffer(size);
    in.readFully(readBuffer.buffer(), 0, size);
    readBuffer.setLength(size);
    Entry entry = new Entry(readBuffer.toByteArray());
    crc32.reset();
    crc32.update(entry.getData());
    if (crc32.getValue() != checksum) {
      throw new ChecksumNotMatchedException("invalid checksum");
    }
    return entry;
  }

  private void prepareBuffer(int size) {
    if (Objects.isNull(readBuffer)) {
      readBuffer = new ByteArrayBuffer(size);
    } else {
      readBuffer.ensureCapacity(size);
    }
    readBuffer.clear();
  }

  @Override
  public void close() throws IOException {
    this.in.close();
  }
}
