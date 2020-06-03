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

package org.openlmis.fulfillment.web.util;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.fulfillment.domain.ExternalStatus;
import org.openlmis.fulfillment.domain.StatusMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusMessageDto implements StatusMessage.Exporter, StatusMessage.Importer {
  private UUID id;
  private UUID authorId;
  private ExternalStatus status;
  private String body;

  /**
   * Create new instance of StatusMessageDto based on given {@link StatusMessage}.
   * @param statusMessage instance of StatusMessage
   * @return new instance of StatusMessageDto.
   */
  public static StatusMessageDto newInstance(StatusMessage statusMessage) {
    StatusMessageDto statusMessageDto = new StatusMessageDto();
    statusMessage.export(statusMessageDto);
    return statusMessageDto;
  }
}