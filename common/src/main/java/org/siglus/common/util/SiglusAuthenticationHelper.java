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

package org.siglus.common.util;

import static org.siglus.common.i18n.MessageKeys.ERROR_USER_NOT_FOUND;

import java.util.UUID;
import org.siglus.common.domain.referencedata.User;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.exception.AuthenticationException;
import org.siglus.common.exception.NotFoundException;
import org.siglus.common.repository.UserRepository;
import org.siglus.common.service.client.SiglusUserReferenceDataService;
import org.siglus.common.util.referencedata.messagekeys.UserMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SiglusAuthenticationHelper {

  @Autowired
  private SiglusUserReferenceDataService userReferenceDataService;

  @Autowired
  private UserRepository userRepository;

  public User getCurrentUserDomain() {
    UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    User user = userRepository.findOne(userId);

    if (user == null) {
      throw new NotFoundException(UserMessageKeys.ERROR_NOT_FOUND);
    }

    return user;
  }

  public UserDto getCurrentUser() {
    UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    UserDto user = userReferenceDataService.findOne(userId);
    if (user == null) {
      throw new AuthenticationException(new Message(ERROR_USER_NOT_FOUND, userId));
    }
    return user;
  }

}
