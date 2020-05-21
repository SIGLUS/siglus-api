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

package org.openlmis.referencedata.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.util.UUID;
import org.openlmis.referencedata.exception.UnauthorizedException;
import org.openlmis.referencedata.repository.RightAssignmentRepository;
import org.openlmis.referencedata.repository.UserRepository;
import org.openlmis.referencedata.util.Message;
import org.openlmis.referencedata.util.messagekeys.SystemMessageKeys;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

@Service
public class RightService {
  private static final XLogger XLOGGER = XLoggerFactory.getXLogger(RightService.class);

  private static final String MESSAGEKEY_ERROR_UNAUTHORIZED = SystemMessageKeys.ERROR_UNAUTHORIZED;
  private static final String MESSAGEKEY_ERROR_UNAUTHORIZED_GENERIC =
      SystemMessageKeys.ERROR_UNAUTHORIZED_GENERIC;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RightAssignmentRepository rightAssignmentRepository;

  @Autowired
  private AuthenticationHelper authenticationHelper;

  @Value("${auth.server.clientId}")
  private String serviceTokenClientId;

  @Value("${auth.server.clientId.apiKey.prefix}")
  private String apiKeyPrefix;

  /**
   * Check the client has the admin right specified.
   *
   * @param rightName the name of the right to check
   * @throws UnauthorizedException in case the client has got no right to access the resource
   */
  public void checkAdminRight(String rightName) {
    checkAdminRight(rightName, true);
  }

  /**
   * Check the client has the admin right specified.
   *
   * @param rightName the name of the right to check
   * @param allowServiceTokens whether to allow service-level tokens with root access
   * @throws UnauthorizedException in case the client has got no right to access the resource
   */
  public void checkAdminRight(String rightName, boolean allowServiceTokens) {
    checkAdminRight(rightName, allowServiceTokens, null);
  }

  public void checkAdminRight(String rightName, boolean allowServiceTokens, UUID expectedUserId) {
    checkAdminRight(rightName, true, allowServiceTokens, false, expectedUserId);
  }

  /**
   * Check the client has the admin right specified.
   *
   * @param rightName the name of the right to check
   * @param allowServiceTokens whether to allow service-level tokens with root access
   * @param expectedUserId id of the user that can bypass the right check
   *                       e.g. to retrieve his own info
   * @throws UnauthorizedException in case the client has got no right to access the resource
   */
  private void checkAdminRight(String rightName, boolean allowUserTokens,
                               boolean allowServiceTokens, boolean allowApiKey,
                               UUID expectedUserId) {
    XLOGGER.entry(rightName, allowUserTokens, allowServiceTokens, allowApiKey, expectedUserId);

    if (!hasRight(rightName, allowUserTokens, allowServiceTokens, allowApiKey, expectedUserId)) {
      // at this point, token is unauthorized
      XLOGGER.exit("Token not valid");
      Message message = isBlank(rightName)
          ? new Message(MESSAGEKEY_ERROR_UNAUTHORIZED_GENERIC)
          : new Message(MESSAGEKEY_ERROR_UNAUTHORIZED, rightName);

      throw new UnauthorizedException(message);
    }
  }

  public boolean hasRight(String rightName) {
    return hasRight(rightName, true, true, false, null);
  }

  private boolean hasRight(String rightName, boolean allowUserTokens, boolean allowServiceTokens,
      boolean allowApiKey, UUID expectedUserId) {
    OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();

    if (authentication.isClientOnly()) {
      if (checkServiceToken(allowServiceTokens, allowApiKey, authentication)) {
        XLOGGER.exit("service token or API Key");
        return true;
      }
    } else {
      if (checkUserToken(rightName, allowUserTokens, expectedUserId)) {
        XLOGGER.exit("User has right");
        return true;
      }
    }

    return false;
  }

  /**
   * Check the client is a trusted client ("root" access).
   */
  public void checkRootAccess() {
    checkAdminRight(null, false, true, false, null);
  }

  private boolean checkUserToken(String rightName, boolean allowUserTokens, UUID expectedUserId) {
    if (!allowUserTokens) {
      return false;
    }

    UUID userId = authenticationHelper.getCurrentUser().getId();

    if (null != expectedUserId
        && userId.equals(expectedUserId)
        && userRepository.exists(userId)) {
      XLOGGER.exit("user id allowed to bypass right check");
      return true;
    }

    if (rightAssignmentRepository.existsByUserIdAndRightName(userId, rightName)) {
      XLOGGER.exit("User has right");
      return true;
    }

    return false;
  }

  private boolean checkServiceToken(boolean allowServiceTokens, boolean allowApiKey,
                                    OAuth2Authentication authentication) {
    String clientId = authentication.getOAuth2Request().getClientId();

    if (serviceTokenClientId.equals(clientId)) {
      return allowServiceTokens;
    }

    if (startsWith(clientId, apiKeyPrefix)) {
      return allowApiKey;
    }

    return false;
  }


}
