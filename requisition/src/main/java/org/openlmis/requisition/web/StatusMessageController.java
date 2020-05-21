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

package org.openlmis.requisition.web;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.domain.requisition.StatusMessage;
import org.openlmis.requisition.dto.StatusMessageDto;
import org.openlmis.requisition.repository.StatusMessageRepository;
import org.openlmis.requisition.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@Transactional
public class StatusMessageController extends BaseController {

  @Autowired
  private StatusMessageRepository statusMessageRepository;

  @Autowired
  private PermissionService permissionService;

  /**
   * Get all status messages for the specified requisition.
   *
   * @param id ID of requisition to get messages for.
   * @return List of status messages.
   */
  @RequestMapping(value = "/requisitions/{id}/statusMessages", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<StatusMessageDto> getAllRequisitionStatusMessages(@PathVariable("id") UUID id) {
    permissionService.canViewRequisition(id).throwExceptionIfHasErrors();
    List<StatusMessage> statusMessages = statusMessageRepository.findByRequisitionId(id);
    return exportToDtos(statusMessages);
  }

  private List<StatusMessageDto> exportToDtos(List<StatusMessage> statusMessages) {
    return statusMessages.stream()
        .map(this::exportToDto)
        .collect(Collectors.toList());
  }
  
  private StatusMessageDto exportToDto(StatusMessage statusMessage) {
    StatusMessageDto statusMessageDto = new StatusMessageDto();
    statusMessage.export(statusMessageDto);
    return statusMessageDto;
  }
}
