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

package org.openlmis.notification.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@Embeddable
@ToString
@EqualsAndHashCode
public class EmailDetails {

  @Column(unique = true)
  @Getter
  @Setter
  private String email;

  @Column(nullable = false, columnDefinition = "boolean DEFAULT false")
  @Getter
  @Setter
  private Boolean emailVerified;

  public EmailDetails() {
    this(null, null);
  }

  /**
   * Creates new instance with passed values. If the email parameter is blank, all fields will have
   * a null value. If the emailVerified parameter is null, the false value will be used.
   */
  public EmailDetails(String email, Boolean emailVerified) {
    if (StringUtils.isBlank(email)) {
      this.email = null;
      this.emailVerified = null;
    } else {
      this.email = email;
      this.emailVerified = BooleanUtils.toBoolean(emailVerified);
    }
  }

  /**
   * Construct new email details based on an importer (DTO).
   *
   * @param importer importer (DTO) to use
   * @return new email details
   */
  static EmailDetails newEmailDetails(Importer importer) {
    return null != importer
        ? new EmailDetails(importer.getEmail(), importer.getEmailVerified())
        : new EmailDetails();
  }

  public interface Exporter {
    void setEmail(String email);

    void setEmailVerified(Boolean emailVerified);
  }

  public interface Importer {
    String getEmail();

    Boolean getEmailVerified();
  }

  /**
   * Copy data from the given email details to the instance that implement
   * {@link Exporter} interface.
   */
  public void export(Exporter exporter) {
    exporter.setEmail(email);
    exporter.setEmailVerified(emailVerified);
  }
}
