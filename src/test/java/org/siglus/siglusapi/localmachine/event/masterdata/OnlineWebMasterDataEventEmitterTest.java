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

package org.siglus.siglusapi.localmachine.event.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.referencedata.domain.User;
import org.openlmis.referencedata.repository.UserRepository;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.cdc.CdcRecord;
import org.siglus.siglusapi.localmachine.cdc.CdcRecordMapper;
import org.siglus.siglusapi.localmachine.server.OnlineWebService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class OnlineWebMasterDataEventEmitterTest {

  @InjectMocks
  private MasterDataEventEmitter emitter;

  @Mock
  private CdcRecordMapper cdcRecordMapper;

  @Mock
  private EventPublisher eventPublisher;

  @Mock
  private UserRepository userRepository;

  @Mock
  private OnlineWebService onlineWebService;

  private static final UUID userId1 = UUID.randomUUID();

  private static final UUID userId2 = UUID.randomUUID();

  private static final UUID facilityId1 = UUID.randomUUID();

  private static final UUID facilityId2 = UUID.randomUUID();

  @Test
  public void shouldNotEvictSnapshotWhenReceiveChangeRecordsNotFromIncompatibles() {
    // given
    CdcRecord incompatibleRecord =
        CdcRecord.builder().schema("siglusintegration").table("hf_cmm").build();
    // when
    emitter.on(Collections.singletonList(incompatibleRecord));
    // then
    verify(onlineWebService, times(0)).evictAllMasterDataSnapshots();
  }

  @Test
  public void shouldEvictSnapshotWhenReceiveChangeRecordsFromIncompatibles() {
    // given
    CdcRecord incompatibleRecord =
        CdcRecord.builder().schema("siglusintegration").table("facility_extension").build();
    // when
    emitter.on(Collections.singletonList(incompatibleRecord));
    // then
    verify(onlineWebService, times(1)).evictAllMasterDataSnapshots();
  }

  @Test
  public void shouldEmitSuccessfully() {
    // given
    when(userRepository.findAll()).thenReturn(generateUsers());
    Map<String, Object> payload1 = new HashMap<>();
    payload1.put("userid", userId1);
    Map<String, Object> payload2 = new HashMap<>();
    payload2.put("userid", userId2);
    CdcRecord cdcRecord1 = buildCdcRecord("c", "role_assignments", null);
    CdcRecord cdcRecord2 = buildCdcRecord("d", "right_assignments", null);
    CdcRecord cdcRecord3 = buildCdcRecord("c", "right_assignments", payload1);
    CdcRecord cdcRecord4 = buildCdcRecord("c", "right_assignments", payload2);

    // when
    emitter.on(Arrays.asList(cdcRecord1, cdcRecord2, cdcRecord3, cdcRecord4));

    // then
    verify(cdcRecordMapper, times(1)).buildAlreadyGroupedEvents(Collections.singletonList(cdcRecord3));
    verify(cdcRecordMapper, times(1)).buildAlreadyGroupedEvents(Collections.singletonList(cdcRecord4));
    verify(cdcRecordMapper, times(1)).buildEvents(Arrays.asList(cdcRecord1, cdcRecord2));
    verify(eventPublisher, times(1))
        .emitMasterDataEvent(any(MasterDataTableChangeEvent.class), eq(facilityId1));
    verify(eventPublisher, times(1))
        .emitMasterDataEvent(any(MasterDataTableChangeEvent.class), eq(facilityId2));
    verify(eventPublisher, times(1))
        .emitMasterDataEvent(any(MasterDataTableChangeEvent.class), eq(null));
    assertThat(emitter.acceptedTables().length).isGreaterThan(0);
  }

  private List<User> generateUsers() {
    User user1 = new User();
    user1.setId(userId1);
    user1.setHomeFacilityId(facilityId1);
    User user2 = new User();
    user2.setId(userId2);
    user2.setHomeFacilityId(facilityId2);
    return Arrays.asList(user1, user2);
  }

  private CdcRecord buildCdcRecord(String operationCode, String table, Map<String, Object> payload) {
    return CdcRecord.builder()
        .payload(payload)
        .schema("referencedata")
        .table(table)
        .operationCode(operationCode)
        .build();
  }
}