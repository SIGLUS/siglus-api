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

package org.openlmis.notification.util;

import org.openlmis.notification.domain.EmailDetails;

public class EmailDetailsDataBuilder {
  private static int instanceNumber = 0;

  private String email;
  private Boolean emailVerified = true;

  /**
   * Builds instance of {@link EmailDetailsDataBuilder} with sample data.
   */
  public EmailDetailsDataBuilder() {
    instanceNumber++;

    email = instanceNumber + "example@mail.com";
  }

  public EmailDetailsDataBuilder withUnverifiedFlag() {
    this.emailVerified = false;
    return this;
  }

  public EmailDetailsDataBuilder withEmail(String email) {
    this.email = email;
    return this;
  }

  public EmailDetailsDataBuilder withVerified(Boolean verified) {
    this.emailVerified = verified;
    return this;
  }

  /**
   * Builds instance of {@link EmailDetails}.
   */
  public EmailDetails build() {
    return new EmailDetails(email, emailVerified);
  }
}
