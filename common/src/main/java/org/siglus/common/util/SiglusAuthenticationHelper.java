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

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.siglus.common.dto.referencedata.UserDto;
import org.siglus.common.exception.AuthenticationException;
import org.siglus.common.service.client.SiglusUserReferenceDataService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SiglusAuthenticationHelper {

  private final SiglusUserReferenceDataService userService;

  public Optional<UUID> getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof UUID) {
      return Optional.of((UUID) principal);
    } else if (principal instanceof UserDto) {
      UserDto userDto = (UserDto) principal;
      return Optional.ofNullable(userDto.getId());
    }
    throw new UnsupportedOperationException(principal.getClass() + ":" + principal);
  }

  public UserDto getCurrentUser() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof UUID) {
      UUID userId = (UUID) principal;
      UserDto user = userService.findOne(userId);
      if (user == null) {
        throw new AuthenticationException(new Message(ERROR_USER_NOT_FOUND, userId));
      }
      Authentication authentication = new UsernamePasswordAuthenticationToken(user, "N/A");
      SecurityContextHolder.getContext().setAuthentication(authentication);
      return user;
    } else if (principal instanceof UserDto) {
      return (UserDto) principal;
    }
    throw new UnsupportedOperationException(principal.getClass() + ":" + principal);
  }

  public Collection<PermissionString> getCurrentUserPermissionStrings() {
    return userService
        .getPermissionStrings(getCurrentUserId().orElseThrow(this::authenticateFail))
        .stream()
        .map(PermissionString::new)
        .collect(Collectors.toList());
  }

  private AuthenticationException authenticateFail() {
    return new AuthenticationException(new Message(ERROR_USER_NOT_FOUND, "null"));
  }

}
