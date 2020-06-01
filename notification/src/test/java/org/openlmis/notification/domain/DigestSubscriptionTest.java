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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_DIGEST_SUBSCRIPTION_INVALID_CHANNEL_FOR_DIGEST;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_INVALID_CRON_EXPRESSION_IN_SUBSCRIPTION;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_MISSING_CRON_EXPRESSION;

import com.google.common.collect.Maps;
import java.util.Map;
import lombok.AllArgsConstructor;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.notification.service.NotificationChannel;
import org.openlmis.notification.testutils.DigestConfigurationDataBuilder;
import org.openlmis.notification.testutils.DigestSubscriptionDataBuilder;
import org.openlmis.notification.testutils.ToStringTestUtils;
import org.openlmis.notification.web.ValidationException;

public class DigestSubscriptionTest {

  private static final String CONFIGURATION = "configuration";
  private static final String TIME = "time";
  private static final String PREFERRED_CHANNEL = "preferredChannel";
  private static final String USE_DIGEST = "useDigest";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldExportData() {
    Map<String, Object> map = Maps.newHashMap();
    DummyExporter exporter = new DummyExporter(map);

    String cronExpression = "0 15 17 1 1 MON";
    DigestConfiguration configuration = new DigestConfigurationDataBuilder().build();
    DigestSubscription subscription = new DigestSubscriptionDataBuilder()
        .withDigestConfiguration(configuration)
        .withCronExpression(cronExpression)
        .build();
    subscription.export(exporter);

    assertThat(map)
        .containsEntry(CONFIGURATION, configuration)
        .containsEntry(TIME, cronExpression);
  }

  @Test
  public void shouldNotCreateNewInstanceIfCronExpressionIsInvalid() {
    exception.expect(ValidationException.class);
    exception.expectMessage(ERROR_INVALID_CRON_EXPRESSION_IN_SUBSCRIPTION);

    DigestSubscription.create(null, null, "* 0/bin * * * *", null, false);
  }

  @Test
  public void shouldNotCreateNewInstanceIfCronExpressionIsMissingAndUsesDigest() {
    DigestSubscription.create(null, null, null, null, false);
  }

  @Test
  public void shouldCreateNewInstanceIfCronExpressionIsMissingAndDigestIsNotUsed() {
    exception.expect(ValidationException.class);
    exception.expectMessage(ERROR_MISSING_CRON_EXPRESSION);

    DigestSubscription.create(null, null, null, null, true);
  }

  @Test
  public void shouldNotCreateNewInstanceIfUseDigestIsSetForNonEmailChannel() {
    exception.expect(ValidationException.class);
    exception.expectMessage(ERROR_DIGEST_SUBSCRIPTION_INVALID_CHANNEL_FOR_DIGEST);

    DigestSubscription.create(null, null, "0 15 17 1 1 MON", NotificationChannel.SMS, true);

  }

  @Test
  public void shouldCreateNewInstanceForCorrectData() {
    // given
    String cronExpression = "* * * * * *";

    // when
    DigestSubscription subscription = DigestSubscription.create(null, null, cronExpression, null,
        false);

    // then
    assertThat(subscription.getCronExpression()).isEqualTo(cronExpression);
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(DigestSubscription.class)
        .withRedefinedSuperclass()
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    ToStringTestUtils.verify(DigestSubscription.class, new DigestSubscription());
  }

  @AllArgsConstructor
  private static final class DummyExporter implements DigestSubscription.Exporter {

    private Map<String, Object> map;

    @Override
    public void setDigestConfiguration(DigestConfiguration configuration) {
      map.put(CONFIGURATION, configuration);
    }

    @Override
    public void setCronExpression(String time) {
      map.put(TIME, time);
    }

    @Override
    public void setPreferredChannel(NotificationChannel preferredChannel) {
      map.put(PREFERRED_CHANNEL, preferredChannel);
    }

    @Override
    public void setUseDigest(Boolean digest) {
      map.put(USE_DIGEST, digest);
    }
  }

}
