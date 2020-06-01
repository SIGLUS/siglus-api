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

import java.time.ZonedDateTime;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.openlmis.notification.testutils.EmailVerificationTokenDataBuilder;

public class EmailVerificationTokenTest {
  private EmailVerificationToken token = new EmailVerificationTokenDataBuilder().build();

  @Test
  public void shouldBeExpired() {
    token.setExpiryDate(ZonedDateTime.now().minusDays(5));

    assertThat(token.isExpired()).isTrue();
  }

  @Test
  public void shouldNotBeExpired() {
    token.setExpiryDate(ZonedDateTime.now().plusDays(5));

    assertThat(token.isExpired()).isFalse();
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(EmailVerificationToken.class)
        .withRedefinedSuperclass()
        .verify();
  }

}
