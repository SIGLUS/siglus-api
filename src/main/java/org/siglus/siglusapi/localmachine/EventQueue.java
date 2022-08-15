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

package org.siglus.siglusapi.localmachine;

import java.util.List;
import lombok.Data;

@Data
public class EventQueue {
  private final MetaInfo metaInfo;

  public EventQueue(MetaInfo metaInfo) {
    this.metaInfo = metaInfo;
  }

  public List<Event> getForOnlineWeb() {
    // FIXME: 2022/8/14
    return null;
  }

  @Data
  public static class MetaInfo {
    private final String ownerId;
  }
}
