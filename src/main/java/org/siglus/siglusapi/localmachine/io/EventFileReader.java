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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.InflaterInputStream;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.localmachine.Event;
import org.siglus.siglusapi.localmachine.ExternalEventDtoMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class EventFileReader {
  private final ExternalEventDtoMapper mapper;

  public List<Event> readAll(MultipartFile file) throws IOException {
    EventReader reader =
        new EventReader(
            new EntryReader(
                new InflaterInputStream(new BufferedInputStream(file.getInputStream()))),
            mapper);
    List<Event> events = new LinkedList<>();
    while (true) {
      try {
        events.add(reader.read());
      } catch (EOFException eof) {
        break;
      }
    }
    reader.close();
    return events;
  }
}
