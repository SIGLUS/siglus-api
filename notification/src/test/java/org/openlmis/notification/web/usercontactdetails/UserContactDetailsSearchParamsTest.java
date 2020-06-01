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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;

import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.notification.i18n.MessageKeys;
import org.openlmis.notification.testutils.ToStringTestUtils;
import org.openlmis.notification.testutils.UserContactDetailsSearchParamsDataBuilder;
import org.openlmis.notification.web.ValidationException;
import org.springframework.util.LinkedMultiValueMap;

public class UserContactDetailsSearchParamsTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldGetEmailValueFromParameters() {
    UserContactDetailsSearchParams params = new UserContactDetailsSearchParamsDataBuilder()
        .withEmail("test@example.org")
        .build();

    assertThat(params.getEmail()).isEqualTo("test@example.org");
  }

  @Test
  public void shouldGetNullIfMapHasNoEmailProperty() {
    UserContactDetailsSearchParams params = new UserContactDetailsSearchParamsDataBuilder()
        .build();
    assertThat(params.getEmail()).isNull();
  }

  @Test
  public void shouldThrowExceptionIfThereIsUnknownParameterInParameters() {
    exception.expect(ValidationException.class);
    exception.expectMessage(MessageKeys.ERROR_USER_CONTACT_DETAILS_SEARCH_INVALID_PARAMS);

    new UserContactDetailsSearchParamsDataBuilder()
        .withInvalidParam()
        .build();
  }

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(UserContactDetailsSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    UserContactDetailsSearchParams params = new UserContactDetailsSearchParamsDataBuilder()
        .withEmail("test@example.org")
        .withId(UUID.randomUUID())
        .build();

    ToStringTestUtils.verify(UserContactDetailsSearchParams.class, params,
        "EMAIL", "ID", "ALL_PARAMETERS");
  }

  @Test
  public void shouldReturnListOfIdsParsedToUuid() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();

    UserContactDetailsSearchParams params = new UserContactDetailsSearchParamsDataBuilder()
        .withId(id1)
        .withId(id2)
        .withId(id3)
        .build();

    Assert.assertThat(params.getIds(), hasItems(id1, id2, id3));
  }

  @Test
  public void shouldReturnEmptySetOfIdsIfNoneHaveBeenProvided() {
    LinkedMultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
    UserContactDetailsSearchParams params = new UserContactDetailsSearchParams(queryMap);

    assertThat(params.getIds()).isNotNull().isEmpty();
  }
}
