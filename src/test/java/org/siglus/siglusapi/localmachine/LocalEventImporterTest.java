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

package org.siglus.siglusapi.localmachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalEventImporterTest {
  @Mock private Machine machine;
  @InjectMocks private LocalEventImporter localEventImporter;

  @Test
  public void returnTrueWhenCheckIfSupportedFacilityGivenFacilityInSupportedFacilityList() {
    // given
    UUID receiverFacilityId = UUID.randomUUID();
    given(machine.fetchSupportedFacilityIds())
        .willReturn(Collections.singleton(receiverFacilityId.toString()));
    // then
    assertThat(localEventImporter.supportedFacility(receiverFacilityId)).isTrue();
  }

  @Test
  public void returnFalseWhenCheckIfSupportedFacilityGivenFacilityNotInSupportedFacilityList() {
    // given
    UUID receiverFacilityId = UUID.randomUUID();
    given(machine.fetchSupportedFacilityIds()).willReturn(Collections.emptySet());
    // then
    assertThat(localEventImporter.supportedFacility(receiverFacilityId)).isFalse();
  }

  @Test
  public void returnFalseWhenCheckIfSupportedFacilityGivenNullFacility() {
    // given
    given(machine.fetchSupportedFacilityIds()).willReturn(Collections.emptySet());
    // then
    assertThat(localEventImporter.supportedFacility(null)).isFalse();
  }

  @Test
  public void shouldSetReceiverSyncedWhenResetStatusGivenImTheReceiver() {
    // given
    UUID receiverFacilityId = UUID.randomUUID();
    given(machine.fetchSupportedFacilityIds())
        .willReturn(Collections.singleton(receiverFacilityId.toString()));
    Event event = Event.builder().receiverId(receiverFacilityId).receiverSynced(false).build();
    // when
    localEventImporter.resetStatus(Collections.singletonList(event));
    // then
    assertThat(event.isReceiverSynced()).isTrue();
  }

  @Test
  public void shouldSetLocalReplayedFalseWhenResetStatus() {
    // given
    Event event = Event.builder().localReplayed(true).build();
    // when
    localEventImporter.resetStatus(Collections.singletonList(event));
    // then
    assertThat(event.isLocalReplayed()).isFalse();
  }
}
