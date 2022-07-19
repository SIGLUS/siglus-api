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

package org.siglus.siglusapi.migration;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_PERMISSION_NOT_SUPPORTED;

import java.util.Optional;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataMigrationGuard {
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Value("${datamigration.secret}")
  private String encodedSecret;

  public void assertAuthorized(String secret) {
    secret = Optional.ofNullable(secret).orElse("");
    if (encoder.matches(secret, encodedSecret)) {
      return;
    }
    throw new AuthenticationException(new Message(ERROR_PERMISSION_NOT_SUPPORTED));
  }
}