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

package org.openlmis.notification.web.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.notification.i18n.MessageKeys;
import org.openlmis.notification.testutils.NotificationSearchParamsDataBuilder;
import org.openlmis.notification.testutils.ToStringTestUtils;
import org.openlmis.notification.web.ValidationException;

public class NotificationSearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldGetUserIdValueFromParameters() {
    UUID userId = UUID.randomUUID();

    NotificationSearchParams params = new NotificationSearchParamsDataBuilder()
        .withUserId(userId)
        .build();

    assertThat(params.getUserId()).isEqualTo(userId);
  }

  @Test
  public void shouldGetNullIfMapHasNoUserIdProperty() {
    NotificationSearchParams params = new NotificationSearchParamsDataBuilder().build();
    assertThat(params.getUserId()).isNull();
  }

  @Test
  public void shouldGetSendingDateFromValueFromParameters() {
    ZonedDateTime sendingDateFrom = ZonedDateTime.now();

    NotificationSearchParams params = new NotificationSearchParamsDataBuilder()
        .withSendingDateFrom(sendingDateFrom)
        .build();

    assertThat(params.getSendingDateFrom()).isEqualTo(sendingDateFrom);
  }

  @Test
  public void shouldGetNullIfMapHasNoSendingDateFromProperty() {
    NotificationSearchParams params = new NotificationSearchParamsDataBuilder().build();
    assertThat(params.getSendingDateFrom()).isNull();
  }

  @Test
  public void shouldGetSendingDateToValueFromParameters() {
    ZonedDateTime sendingDateTo = ZonedDateTime.now();

    NotificationSearchParams params = new NotificationSearchParamsDataBuilder()
        .withSendingDateTo(sendingDateTo)
        .build();

    assertThat(params.getSendingDateTo()).isEqualTo(sendingDateTo);
  }

  @Test
  public void shouldGetNullIfMapHasNoSendingDateToProperty() {
    NotificationSearchParams params = new NotificationSearchParamsDataBuilder().build();
    assertThat(params.getSendingDateTo()).isNull();
  }

  @Test
  public void shouldThrowExceptionIfThereIsUnknownParameterInParameters() {
    exception.expect(ValidationException.class);
    exception.expectMessage(MessageKeys.ERROR_NOTIFICATION_SEARCH_INVALID_PARAMS);

    new NotificationSearchParamsDataBuilder()
        .withInvalidParam()
        .build();
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(NotificationSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    NotificationSearchParams params = new NotificationSearchParamsDataBuilder()
        .withUserId(UUID.randomUUID())
        .withSendingDateFrom(ZonedDateTime.now().minusDays(7))
        .withSendingDateTo(ZonedDateTime.now())
        .build();

    ToStringTestUtils.verify(NotificationSearchParams.class, params,
        "USER_ID", "SENDING_DATE_FROM", "SENDING_DATE_TO", "ALL_PARAMETERS");
  }
}
