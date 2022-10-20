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

import lombok.Data;

// Entry layout
// |--magic number (unsigned byte)--|--checksum(long)--|--size(int)--|--data bytes--|
@Data
public class Entry {
  public static final byte MAGIC_NUMBER = '|';
  public static final int MAX_SIZE_BYTES = 100 * 1024 * 1024;
  private byte[] data;

  public Entry(byte[] data) {
    this.data = data;
  }
}
