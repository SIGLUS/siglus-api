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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import org.junit.Test;

public class EntryReaderTest {
  @Test(expected = IOException.class)
  public void shouldThrowWhenReadGivenMagicNumberNotMatch() throws IOException {
    new EntryReader(new ByteArrayInputStream("0".getBytes())).read();
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowWhenReadGivenClaimedSizeTooLarge() throws IOException {
    // given
    ByteArrayOutputStream dest = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(dest);
    out.writeByte(Entry.MAGIC_NUMBER);
    out.writeLong(1L);
    out.writeInt(Entry.MAX_SIZE_BYTES + 1);
    out.writeChar('-');
    out.flush();
    // then
    new EntryReader(new ByteArrayInputStream(dest.toByteArray())).read();
  }

  @Test(expected = ChecksumNotMatchedException.class)
  public void shouldThrowWhenReadGivenChecksumNotMatch() throws IOException {
    // given
    ByteArrayOutputStream dest = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(dest);
    out.writeByte(Entry.MAGIC_NUMBER);
    long invalidChecksum = 100L;
    out.writeLong(invalidChecksum);
    out.writeInt(1);
    out.writeChar('-');
    out.flush();
    // then
    new EntryReader(new ByteArrayInputStream(dest.toByteArray())).read();
  }

  @Test
  public void shouldReadEntrySuccessfullyGivenValidEntry() throws IOException {
    // given
    ByteArrayOutputStream dest = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(dest);
    String data = "data";
    byte[] dataBytes = data.getBytes();
    CRC32 crc32 = new CRC32();
    crc32.update(dataBytes);
    long checksum = crc32.getValue();
    out.writeByte(Entry.MAGIC_NUMBER);
    out.writeLong(checksum);
    out.writeInt(dataBytes.length);
    out.writeBytes(data);
    out.flush();
    // when
    Entry entry = new EntryReader(new ByteArrayInputStream(dest.toByteArray())).read();
    // then
    assertThat(entry.getData()).isEqualTo(data.getBytes());
  }

  @Test
  public void canReadEntryCreatedByEntryWriter() throws IOException {
    // given
    ByteArrayOutputStream dest = new ByteArrayOutputStream();
    EntryWriter writer = new EntryWriter(dest);
    String data1 = "data1";
    writer.write(new Entry(data1.getBytes()));
    String data2 = "data2";
    writer.write(new Entry(data2.getBytes()));
    EntryReader reader = new EntryReader(new ByteArrayInputStream(dest.toByteArray()));
    // when
    Entry entry1 = reader.read();
    Entry entry2 = reader.read();
    // then
    assertThat(entry1.getData()).isEqualTo(data1.getBytes());
    assertThat(entry2.getData()).isEqualTo(data2.getBytes());
  }
}
