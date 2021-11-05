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

package org.siglus.siglusapi.util;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_USER_NOT_FOUND;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.javers.common.collections.Sets;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.siglus.siglusapi.service.client.SiglusUserReferenceDataService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SiglusAuthenticationHelper {

  private final SiglusUserReferenceDataService userService;

  public Optional<UUID> getCurrentUserId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    Set<String> trustedClients = Sets.asSet("trusted-client", "fc-client");
    if (principal == null || trustedClients.contains(principal.toString())) {
      return Optional.empty();
    }
    return Optional.of((UUID) principal);
  }

  public UserDto getCurrentUser() {
    Optional<UUID> currentUserId = getCurrentUserId();
    if (Optional.empty().equals(currentUserId)) {
      return null;
    }
    return currentUserId.map(userService::findOne).orElseThrow(() -> authenticateFail(currentUserId.orElse(null)));
  }

  public Collection<PermissionString> getCurrentUserPermissionStrings() {
    return userService
        .getPermissionStrings(getCurrentUserId().orElseThrow(() -> authenticateFail(null)))
        .stream()
        .map(PermissionString::new)
        .collect(Collectors.toList());
  }

  private AuthenticationException authenticateFail(UUID currentUserId) {
    return new AuthenticationException(new Message(ERROR_USER_NOT_FOUND, currentUserId));
  }

}
