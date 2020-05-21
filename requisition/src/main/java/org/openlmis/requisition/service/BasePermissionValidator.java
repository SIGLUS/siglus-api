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

package org.openlmis.requisition.service;

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;

import org.openlmis.requisition.errorhandling.ValidationResult;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

abstract class BasePermissionValidator implements PermissionValidator {

  private final XLogger logger = XLoggerFactory.getXLogger(getClass());

  @Value("${auth.server.clientId}")
  private String serviceTokenClientId;

  @Override
  public ValidationResult hasPermission(PermissionValidationDetails details) {
    logger.entry(details);

    Profiler profiler = new Profiler("CHECK_PERMISSION");
    profiler.setLogger(logger);

    profiler.start("GET_AUTHENTICATION");
    OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder
        .getContext()
        .getAuthentication();

    boolean hasPermission = authentication.isClientOnly()
        ? checkServiceToken(authentication, profiler)
        : checkUserToken(details, profiler);

    profiler.start("CREATE_VALIDATION_RESULT");
    ValidationResult result = hasPermission
        ? ValidationResult.success()
        : ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, details.getRightName());

    profiler.stop().log();
    logger.exit(result);

    return result;
  }

  abstract boolean checkUserToken(PermissionValidationDetails details, Profiler profiler);

  private boolean checkServiceToken(OAuth2Authentication authentication, Profiler profiler) {
    profiler.start("CHECK_IS_SERVICE_TOKEN");
    String clientId = authentication.getOAuth2Request().getClientId();

    // we accept only service's requests
    return serviceTokenClientId.equals(clientId);
  }
}
