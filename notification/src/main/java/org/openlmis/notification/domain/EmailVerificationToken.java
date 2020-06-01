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

import java.time.ZonedDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_verification_tokens")
@EqualsAndHashCode(callSuper = true)
public class EmailVerificationToken extends BaseEntity {

  @Getter
  @Setter
  @Column(nullable = false, columnDefinition = "timestamp with time zone")
  private ZonedDateTime expiryDate;

  @Getter
  @Setter
  @OneToOne
  @JoinColumn(name = "userContactDetailsId", nullable = false, unique = true)
  private UserContactDetails userContactDetails;

  @Getter
  @Setter
  private String emailAddress;

  public boolean isExpired() {
    return expiryDate.isBefore(ZonedDateTime.now());
  }

  /**
   * Export this object to the specified exporter (DTO).
   *
   * @param exporter exporter to export to
   */
  public void export(Exporter exporter) {
    exporter.setToken(getId());
    exporter.setEmailAddress(emailAddress);
    exporter.setExpiryDate(expiryDate);
  }

  public interface Exporter {

    void setToken(UUID token);

    void setExpiryDate(ZonedDateTime expiryDate);

    void setEmailAddress(String emailAddress);

  }

}
