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

package org.openlmis.notification.service;

import static org.apache.commons.lang3.BooleanUtils.isNotTrue;

import java.util.UUID;
import org.openlmis.notification.service.referencedata.RightDto;
import org.openlmis.notification.service.referencedata.UserDto;
import org.openlmis.notification.service.referencedata.UserReferenceDataService;
import org.openlmis.notification.util.AuthenticationHelper;
import org.openlmis.notification.web.MissingPermissionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("PMD.TooManyMethods")
public class PermissionService {
  private static final String USERS_MANAGE = "USERS_MANAGE";

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Value("${auth.server.clientId}")
  private String serviceTokenClientId;

  /**
   * Checks whether current request has access to viewing contact details of user with the given
   * userId.
   *
   * @param userId  the reference data user ID
   */
  public void canManageUserContactDetails(UUID userId) {
    if (!isCurrentUser(userId) && hasNoPermission(USERS_MANAGE, true)) {
      throw new MissingPermissionException(USERS_MANAGE);
    }
  }

  /**
   * Checks whether current request has access to manage user subscriptions.
   */
  public void canManageUserSubscriptions(UUID userId) {
    if (!isCurrentUser(userId) && hasNoPermission(USERS_MANAGE, true)) {
      throw new MissingPermissionException(USERS_MANAGE);
    }
  }

  /**
   * Checks whether current request has access to sending notification.
   */
  public void canSendNotification() {
    if (hasNoPermission(null, false)) {
      throw new MissingPermissionException();
    }
  }

  private boolean isCurrentUser(UUID userId) {
    if (null == userId) {
      return false;
    }

    UserDto user = authenticationHelper.getCurrentUser();

    if (user == null) {
      return false;
    }

    return userId.equals(user.getId());
  }

  private boolean hasNoPermission(String rightName, boolean allowUsers) {
    OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();

    if (authentication.isClientOnly()) {
      return isNotValidServiceToken(authentication);
    }

    if (allowUsers) {
      return isNotValidUserToken(rightName);
    }

    return true;
  }

  private boolean isNotValidUserToken(String rightName) {
    UserDto user = authenticationHelper.getCurrentUser();
    RightDto right = authenticationHelper.getRight(rightName);
    ResultDto<Boolean> result =  userReferenceDataService.hasRight(
        user.getId(), right.getId(), null, null, null
    );

    return null == result || isNotTrue(result.getResult());
  }

  private boolean isNotValidServiceToken(OAuth2Authentication authentication) {
    String clientId = authentication.getOAuth2Request().getClientId();
    return !serviceTokenClientId.equals(clientId);
  }
}
