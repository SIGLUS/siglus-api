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

package org.openlmis.referencedata.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openlmis.referencedata.ToStringTestUtils;
import org.openlmis.referencedata.dto.VersionIdentityDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.testbuilder.FacilityTypeApprovedProductSearchParamsDataBuilder;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeApprovedProductMessageKeys;

public class FacilityTypeApprovedProductSearchParamsTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void equalsContract() {
    EqualsVerifier
        .forClass(FacilityTypeApprovedProductSearchParams.class)
        .suppress(Warning.NONFINAL_FIELDS)
        .withRedefinedSuperclass()
        .verify();
  }

  @Test
  public void shouldImplementToString() {
    FacilityTypeApprovedProductSearchParams searchParams =
        new FacilityTypeApprovedProductSearchParams();

    ToStringTestUtils.verify(FacilityTypeApprovedProductSearchParams.class, searchParams);
  }

  @Test
  public void shouldConvertIdentitiesToPairs() {
    // given
    FacilityTypeApprovedProductSearchParams searchParams =
        new FacilityTypeApprovedProductSearchParamsDataBuilder()
            .withIdentity(UUID.randomUUID(), 1L)
            .withIdentity(UUID.randomUUID(), 2L)
            .build();

    // when
    Set<Pair<UUID, Long>> pairs = searchParams.getIdentityPairs();

    // then
    for (VersionIdentityDto identity : searchParams.getIdentities()) {
      assertThat(pairs).contains(ImmutablePair.of(identity.getId(), identity.getVersionNumber()));
    }
  }

  @Test
  public void shouldThrowExceptionIfVersionIdentityDoesNotHaveIdField() {
    // given
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeApprovedProductMessageKeys.ERROR_INVALID_VERSION_IDENTITY);

    FacilityTypeApprovedProductSearchParams searchParams =
        new FacilityTypeApprovedProductSearchParamsDataBuilder()
            .withIdentity(null, 1L)
            .build();

    // when
    searchParams.getIdentityPairs();
  }

  @Test
  public void shouldThrowExceptionIfVersionIdentityDoesNotHaveVersionIdField() {
    // given
    exception.expect(ValidationMessageException.class);
    exception.expectMessage(FacilityTypeApprovedProductMessageKeys.ERROR_INVALID_VERSION_IDENTITY);

    FacilityTypeApprovedProductSearchParams searchParams =
        new FacilityTypeApprovedProductSearchParamsDataBuilder()
            .withIdentity(UUID.randomUUID(), null)
            .build();

    // when
    searchParams.getIdentityPairs();
  }
}
