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

import static org.openlmis.notification.i18n.MessageKeys.ERROR_DIGEST_SUBSCRIPTION_INVALID_CHANNEL_FOR_DIGEST;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_INVALID_CRON_EXPRESSION_IN_SUBSCRIPTION;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_MISSING_CRON_EXPRESSION;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.web.ValidationException;
import org.springframework.scheduling.support.CronSequenceGenerator;

@Entity
@Table(name = "digest_subscriptions")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class DigestSubscription extends BaseEntity {

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userContactDetailsId", nullable = false)
  private UserContactDetails userContactDetails;

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "digestConfigurationId", nullable = false)
  private DigestConfiguration digestConfiguration;

  @Getter
  private String cronExpression;

  @Getter
  @Enumerated(EnumType.STRING)
  private NotificationChannel preferredChannel;

  @Getter
  private Boolean useDigest;

  /**
   * Creates new instance of {@link DigestSubscription}.
   *
   * @throws ValidationException if cron expression cannot be parsed.
   */
  public static DigestSubscription create(UserContactDetails userContactDetails,
      DigestConfiguration digestConfiguration, String cronExpression,
      NotificationChannel preferredChannel, Boolean useDigest) {

    if (useDigest && null == cronExpression) {
      throw new ValidationException(ERROR_MISSING_CRON_EXPRESSION);
    }

    if (null != cronExpression) {
      try {
        // the following constructor tries to parse the passed cron expression
        // and throws an IllegalArgumentException exception if it cannot be parsed.
        new CronSequenceGenerator(cronExpression);
      } catch (IllegalArgumentException exp) {
        throw new ValidationException(exp,
          ERROR_INVALID_CRON_EXPRESSION_IN_SUBSCRIPTION, cronExpression);
      }
    }

    if (useDigest && !NotificationChannel.EMAIL.equals(preferredChannel)) {
      throw new ValidationException(
          ERROR_DIGEST_SUBSCRIPTION_INVALID_CHANNEL_FOR_DIGEST, preferredChannel.toString()
      );
    }

    return new DigestSubscription(userContactDetails, digestConfiguration, cronExpression,
      preferredChannel, useDigest);
  }

  /**
   * Exports current status of the object.
   */
  public void export(Exporter exporter) {
    exporter.setDigestConfiguration(digestConfiguration);
    exporter.setCronExpression(cronExpression);
    exporter.setPreferredChannel(preferredChannel);
    exporter.setUseDigest(useDigest);
  }

  public interface Exporter {

    void setDigestConfiguration(DigestConfiguration configuration);

    void setCronExpression(String time);

    void setPreferredChannel(NotificationChannel preferredChannel);

    void setUseDigest(Boolean digest);
  }

  public interface Importer {

    UUID getDigestConfigurationId();

    String getCronExpression();

    NotificationChannel getPreferredChannel();

    Boolean getUseDigest();

  }

}
