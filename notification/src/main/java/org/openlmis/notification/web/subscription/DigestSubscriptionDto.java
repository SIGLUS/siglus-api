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

package org.openlmis.notification.web.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.openlmis.notification.domain.DigestConfiguration;
import org.openlmis.notification.domain.DigestSubscription;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.web.BaseDto;
import org.openlmis.notification.web.ObjectReferenceDto;
import org.openlmis.notification.web.digestconfiguration.DigestConfigurationController;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public final class DigestSubscriptionDto
    implements DigestSubscription.Importer, DigestSubscription.Exporter {

  @Setter
  @Getter(AccessLevel.PROTECTED)
  @JsonIgnore
  private String serviceUrl;

  private ObjectReferenceDto digestConfiguration;
  private String cronExpression;
  private Boolean useDigest;
  private NotificationChannel preferredChannel;

  @Override
  @JsonIgnore
  public void setDigestConfiguration(DigestConfiguration configuration) {
    this.digestConfiguration = new ObjectReferenceDto(
        serviceUrl, DigestConfigurationController.RESOURCE_URL, configuration.getId());
  }

  @Override
  @JsonIgnore
  public UUID getDigestConfigurationId() {
    return Optional
        .ofNullable(digestConfiguration)
        .map(BaseDto::getId)
        .orElse(null);
  }
}
