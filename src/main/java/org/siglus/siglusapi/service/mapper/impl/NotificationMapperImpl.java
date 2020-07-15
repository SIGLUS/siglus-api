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

package org.siglus.siglusapi.service.mapper.impl;

import org.siglus.siglusapi.domain.Notification;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.service.mapper.NotificationMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapperImpl implements NotificationMapper {

  @Override
  public NotificationDto from(Notification notification) {
    if (notification == null) {
      return null;
    }
    NotificationDto dto = new NotificationDto();
    dto.setId(notification.getId());
    dto.setEmergencyFlag(notification.getEmergency());
    dto.setSourceFacilityName(notification.getSourceFacilityName());
    dto.setRefId(notification.getRefId());
    dto.setStatus(notification.getRefStatus());
    return dto;
  }

}
