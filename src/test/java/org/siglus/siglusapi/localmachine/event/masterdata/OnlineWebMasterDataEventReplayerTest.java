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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.localmachine.EventPayloadCheckUtils;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.cdc.JdbcSinker;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent.RowChangeEvent;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.service.SiglusAdministrationsService;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class OnlineWebMasterDataEventReplayerTest {

  @InjectMocks
  private MasterDataEventReplayer replayer;

  @Mock
  private JdbcSinker jdbcSinker;
  @Mock
  private Machine machine;
  @Mock
  private FacilityExtensionRepository facilityExtensionRepository;
  @Mock
  private SiglusAdministrationsService administrationsService;

  private final UUID facilityId = UUID.randomUUID();
  private final String tableNameFacilityExtension = "facility_extension";
  private final String fieldFacilityId = "facilityid";
  private final String fieldEnableLocationManagement = "enablelocationmanagement";

  @Test
  public void shouldReplaySuccessAndResetDraftAndLocationWhenReplayGivenLocationManagementStatusChange() {
    // given
    MasterDataTableChangeEvent event = buildMasterDataTableChangeEvent();
    when(machine.getFacilityId()).thenReturn(facilityId);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(null);

    // when
    int i = EventPayloadCheckUtils.checkEventSerializeChanges(event, event.getClass());
    replayer.replay(event);

    // then
    assertEquals(0, i);
    verify(administrationsService).deleteDrafts(facilityId);
    verify(administrationsService).assignToVirtualLocation(facilityId, Boolean.TRUE);
  }

  @Test
  public void shouldReplaySuccessAndDoNotResetDraftAndLocationWhenReplayGivenLocationManagementStatusDoNotChange() {
    // given
    MasterDataTableChangeEvent event = buildMasterDataTableChangeEvent();
    when(machine.getFacilityId()).thenReturn(facilityId);
    when(facilityExtensionRepository.findByFacilityId(facilityId)).thenReturn(
        FacilityExtension.builder().enableLocationManagement(Boolean.TRUE).build());

    // when
    int i = EventPayloadCheckUtils.checkEventSerializeChanges(event, event.getClass());
    replayer.replay(event);

    // then
    assertEquals(0, i);
    verify(administrationsService, times(0)).deleteDrafts(facilityId);
    verify(administrationsService, times(0)).assignToVirtualLocation(facilityId, Boolean.FALSE);
  }

  private MasterDataTableChangeEvent buildMasterDataTableChangeEvent() {
    MasterDataTableChangeEvent event = new MasterDataTableChangeEvent();
    event.setTableChangeEvents(
        Lists.newArrayList(buildRightAssignmentsTableChangeEvent(),
            buildFacilityExtensionTableChangeEvent(),
            buildNotCurrentFacilityExtensionTableChangeEvent()));
    return event;
  }

  private TableChangeEvent buildRightAssignmentsTableChangeEvent() {
    return TableChangeEvent.builder()
        .tableName("right_assignments")
        .schemaName("referencedata")
        .schemaVersion("schemaVersion")
        .columns(Lists.newArrayList("userid", "123"))
        .rowChangeEvents(Lists.newArrayList(buildRowChangeEvent()))
        .build();
  }

  private TableChangeEvent buildFacilityExtensionTableChangeEvent() {
    return TableChangeEvent.builder()
        .tableName(tableNameFacilityExtension)
        .schemaName("siglusintegration")
        .schemaVersion("schemaVersion")
        .columns(buildFacilityExtensionColumns())
        .rowChangeEvents(Lists.newArrayList(buildFacilityExtensionRowChangeEvent()))
        .build();
  }

  private TableChangeEvent buildNotCurrentFacilityExtensionTableChangeEvent() {
    return TableChangeEvent.builder()
        .tableName(tableNameFacilityExtension)
        .schemaName("siglusintegration")
        .schemaVersion("schemaVersion")
        .columns(buildFacilityExtensionColumns())
        .rowChangeEvents(Lists.newArrayList(buildNotCurrentFacilityExtensionRowChangeEvent()))
        .build();
  }

  private List<String> buildFacilityExtensionColumns() {
    return Lists.newArrayList(fieldFacilityId, fieldEnableLocationManagement);
  }

  private RowChangeEvent buildFacilityExtensionRowChangeEvent() {
    RowChangeEvent rowChangeEvent = new RowChangeEvent();
    rowChangeEvent.setDeletion(Boolean.FALSE);
    rowChangeEvent.setValues(Lists.newArrayList(facilityId.toString(), Boolean.TRUE));
    return rowChangeEvent;
  }

  private RowChangeEvent buildNotCurrentFacilityExtensionRowChangeEvent() {
    RowChangeEvent rowChangeEvent = new RowChangeEvent();
    rowChangeEvent.setDeletion(Boolean.FALSE);
    rowChangeEvent.setValues(Lists.newArrayList(UUID.randomUUID().toString(), Boolean.TRUE));
    return rowChangeEvent;
  }

  private RowChangeEvent buildRowChangeEvent() {
    RowChangeEvent rowChangeEvent = new RowChangeEvent();
    rowChangeEvent.setDeletion(Boolean.FALSE);
    rowChangeEvent.setValues(Lists.newArrayList(UUID.randomUUID().toString(), "status"));
    return rowChangeEvent;
  }
}