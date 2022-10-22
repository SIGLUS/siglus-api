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

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.cdc.JdbcSinker;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("localmachine")
public class MasterDataEventReplayer {

  private final JdbcSinker jdbcSinker;
  private final Machine machine;
  private final FacilityExtensionRepository facilityExtensionRepository;
  private final SiglusAdministrationsService administrationsService;
  private final String TABLE_NAME_FACILITY_EXTENSION = "facility_extension";
  private final String FIELD_FACILITY_ID = "facilityid";
  private final String FIELD_ENABLE_LOCATION_MANAGEMENT = "enablelocationmanagement";

  @EventListener(classes = {MasterDataTableChangeEvent.class})
  public void replay(MasterDataTableChangeEvent masterDataTableChangeEvent) {
    jdbcSinker.sink(masterDataTableChangeEvent.getTableChangeEvents());
    resetDraftAndLocationWhenLocationManagementStatusChange(masterDataTableChangeEvent.getTableChangeEvents());
  }

  private void resetDraftAndLocationWhenLocationManagementStatusChange(List<TableChangeEvent> tableChangeEvents) {
    UUID facilityId = machine.getFacilityId();
    FacilityExtension facilityExtension = facilityExtensionRepository.findOne(facilityId);
    tableChangeEvents.forEach(tableChangeEvent -> {
      if (!tableChangeEvent.getTableName().equals(TABLE_NAME_FACILITY_EXTENSION)) {
        return;
      }
      int indexOfFacilityId = tableChangeEvent.getColumns().indexOf(FIELD_FACILITY_ID);
      int indexOfEnableLocationManagement = tableChangeEvent.getColumns().indexOf(FIELD_ENABLE_LOCATION_MANAGEMENT);
      tableChangeEvent.getRowChangeEvents().forEach(rowChangeEvent -> {
        if (!facilityId.toString().equals(rowChangeEvent.getValues().get(indexOfFacilityId))) {
          return;
        }
        Boolean toBeUpdatedEnableLocalManagement = (Boolean) rowChangeEvent.getValues()
            .get(indexOfEnableLocationManagement);
        if (administrationsService.toggledLocationManagement(facilityExtension, toBeUpdatedEnableLocalManagement)) {
          administrationsService.deleteDrafts(facilityId);
          administrationsService.assignToVirtualLocation(facilityId, toBeUpdatedEnableLocalManagement);
        }
      });
    });
  }
}
