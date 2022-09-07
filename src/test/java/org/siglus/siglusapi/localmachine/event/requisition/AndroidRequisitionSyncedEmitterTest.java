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

package org.siglus.siglusapi.localmachine.event.requisition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.dto.ProgramDto;
import org.siglus.siglusapi.domain.RequisitionExtension;
import org.siglus.siglusapi.dto.UserDto;
import org.siglus.siglusapi.dto.android.request.RequisitionCreateRequest;
import org.siglus.siglusapi.localmachine.EventPayloadCheckUtils;
import org.siglus.siglusapi.localmachine.EventPublisher;
import org.siglus.siglusapi.localmachine.event.BaseEventCommonService;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEmitter;
import org.siglus.siglusapi.localmachine.event.requisition.andriod.AndroidRequisitionSyncedEvent;
import org.siglus.siglusapi.localmachine.eventstore.PayloadSerializer;
import org.siglus.siglusapi.service.SiglusProgramService;
import org.siglus.siglusapi.service.SiglusRequisitionExtensionService;
import org.siglus.siglusapi.util.SiglusAuthenticationHelper;
import org.siglus.siglusapi.web.android.FileBasedTest;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class AndroidRequisitionSyncedEmitterTest extends FileBasedTest {
  @InjectMocks
  private AndroidRequisitionSyncedEmitter androidRequisitionSyncedEmitter;

  @Mock
  private EventPublisher eventPublisher;
  @Mock
  private SiglusAuthenticationHelper authHelper;
  @Mock
  private SiglusProgramService siglusProgramService;
  @Mock
  private BaseEventCommonService baseEventCommonService;
  @Mock
  private SiglusRequisitionExtensionService siglusRequisitionExtensionService;
  private final UUID requisitionId = UUID.randomUUID();

  private final UUID facilityId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private RequisitionCreateRequest req = new RequisitionCreateRequest();

  @Before
  public void setup() {
    UserDto user = new UserDto();
    user.setHomeFacilityId(facilityId);
    user.setId(userId);
    when(authHelper.getCurrentUser()).thenReturn(user);
    req.setActualStartDate(LocalDate.now());
    ProgramDto programDto = new ProgramDto();
    programDto.setId(programId);
    when(siglusProgramService.getProgramByCode(any())).thenReturn(Optional.of(programDto));
    final RequisitionExtension ex = new RequisitionExtension();
    ex.setRequisitionNumberPrefix("test");
    ex.setRequisitionNumber(123);
    when(siglusRequisitionExtensionService.buildRequisitionExtension(any(), any(), any())).thenReturn(ex);
  }

  @Test
  public void shouldSuccessWhenEmit() throws IOException {
    // when
    androidRequisitionSyncedEmitter.emit(req, requisitionId);
    // then
    assertThat(req).isNotNull();
  }

  @Test
  public void shouldSerializeSuccessWhenEmit() {
    AndroidRequisitionSyncedEvent emitted = androidRequisitionSyncedEmitter.emit(req, requisitionId);

    int count = EventPayloadCheckUtils.checkEventSerializeChanges(emitted,
        AndroidRequisitionSyncedEvent.class);
    // then
    assertThat(count).isZero();
  }

  @Test
  public void shouldSuccessWhenEmitWithViaJson() throws IOException {
    String json = readFromFile("buildViaRequisitionCreateRequest.json");
    ObjectMapper objectMapper = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER;

    RequisitionCreateRequest req = objectMapper.readValue(json, RequisitionCreateRequest.class);

    AndroidRequisitionSyncedEvent emitted = androidRequisitionSyncedEmitter.emit(req, requisitionId);

    int count = EventPayloadCheckUtils.checkEventSerializeChanges(emitted,
        AndroidRequisitionSyncedEvent.class);
    // then
    assertThat(count).isZero();
  }

  @Test
  public void shouldSuccessWhenEmitWithMmiaJson() throws IOException {
    String json = readFromFile("buildMmiaRequisitionCreateRequest.json");
    ObjectMapper objectMapper = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER;

    RequisitionCreateRequest req = objectMapper.readValue(json, RequisitionCreateRequest.class);

    AndroidRequisitionSyncedEvent emitted = androidRequisitionSyncedEmitter.emit(req, requisitionId);

    int count = EventPayloadCheckUtils.checkEventSerializeChanges(emitted,
        AndroidRequisitionSyncedEvent.class);
    // then
    assertThat(count).isZero();
  }

  @Test
  public void shouldSuccessWhenEmitWithRapidTestJson() throws IOException {
    String json = readFromFile("buildRapidTestRequisitionCreateRequest.json");
    ObjectMapper objectMapper = PayloadSerializer.LOCALMACHINE_EVENT_OBJECT_MAPPER;

    RequisitionCreateRequest req = objectMapper.readValue(json, RequisitionCreateRequest.class);

    AndroidRequisitionSyncedEvent emitted = androidRequisitionSyncedEmitter.emit(req, requisitionId);

    int count = EventPayloadCheckUtils.checkEventSerializeChanges(emitted,
        AndroidRequisitionSyncedEvent.class);
    // then
    assertThat(count).isZero();
  }
}
