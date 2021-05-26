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

package org.siglus.siglusapi.web;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.siglus.siglusapi.domain.NotificationType;
import org.siglus.siglusapi.dto.NotificationDto;
import org.siglus.siglusapi.service.SiglusNotificationService;
import org.siglus.siglusapi.service.SiglusNotificationService.ViewableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/siglusapi/notifications")
@AllArgsConstructor
public class SiglusNotificationController {

  private static final int ALWAYS_FETCH_FIRST_PAGE = 0;

  private final SiglusNotificationService service;

  @GetMapping
  public List<NotificationDto> list(@RequestParam(name = "size", defaultValue = "5") int size,
      @RequestParam(name = "latestOnTop", defaultValue = "true") boolean latestOnTop,
      @RequestParam(name = "type", defaultValue = "TODO") NotificationType type) {
    Direction direction = Direction.ASC;
    if (latestOnTop) {
      direction = Direction.DESC;
    }
    Sort sort = new Sort(direction, "createdDate");
    Pageable pageable = new PageRequest(ALWAYS_FETCH_FIRST_PAGE, size, sort);
    Page<NotificationDto> notificationDtos = service.searchNotifications(pageable, type);
    return notificationDtos.getContent();
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> view(@PathVariable("id") UUID notificationId) {
    ViewableStatus viewableStatus = service.viewNotification(notificationId);
    if (viewableStatus == ViewableStatus.PROCESSED) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

}
