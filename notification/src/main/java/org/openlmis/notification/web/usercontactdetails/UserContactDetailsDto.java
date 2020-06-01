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

package org.openlmis.notification.web.usercontactdetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.notification.domain.EmailDetails;
import org.openlmis.notification.domain.UserContactDetails;

@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public final class UserContactDetailsDto
    implements UserContactDetails.Exporter, UserContactDetails.Importer {

  @Getter
  @Setter
  private UUID referenceDataUserId;

  @Getter
  @Setter
  private String phoneNumber;

  @Getter
  @Setter
  private Boolean allowNotify;

  @Getter
  private EmailDetailsDto emailDetails;

  @JsonSetter("emailDetails")
  public void setEmailDetails(EmailDetailsDto emailDetails) {
    this.emailDetails = emailDetails;
  }

  @Override
  @JsonIgnore
  public void setEmailDetails(EmailDetails emailDetails) {
    this.emailDetails = new EmailDetailsDto();
    emailDetails.export(this.emailDetails);
  }
}
