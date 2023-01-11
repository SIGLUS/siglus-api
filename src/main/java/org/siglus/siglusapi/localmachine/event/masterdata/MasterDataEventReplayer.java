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
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.siglus.siglusapi.domain.FacilityExtension;
import org.siglus.siglusapi.localmachine.Machine;
import org.siglus.siglusapi.localmachine.cdc.JdbcSinker;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent;
import org.siglus.siglusapi.localmachine.cdc.TableChangeEvent.RowChangeEvent;
import org.siglus.siglusapi.repository.FacilityExtensionRepository;
import org.siglus.siglusapi.service.SiglusAdministrationsService;
import org.siglus.siglusapi.service.SiglusCacheService;
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
  private final SiglusCacheService siglusCacheService;
  private static final String TABLE_NAME_FACILITY_EXTENSION = "facility_extension";
  private static final String FIELD_FACILITY_ID = "facilityid";
  private static final String FIELD_ENABLE_LOCATION_MANAGEMENT = "enablelocationmanagement";

  @EventListener(classes = {MasterDataTableChangeEvent.class})
  public void replay(MasterDataTableChangeEvent masterDataTableChangeEvent) {
    List<TableChangeEvent> tableChangeEvents = masterDataTableChangeEvent.getTableChangeEvents();
    resetDraftAndLocationWhenToggledLocationManagement(tableChangeEvents);
    jdbcSinker.sink(tableChangeEvents);
    if (needClearCache(tableChangeEvents)) {
      siglusCacheService.invalidateCache();
    }
  }

  private boolean needClearCache(List<TableChangeEvent> tableChangeEvents) {
    if (CollectionUtils.isEmpty(tableChangeEvents)) {
      return false;
    }
    String tableFullName = tableChangeEvents.get(0).getTableFullName();
    return !MasterDataEventEmitter.doNotClearCacheTableNames.contains(tableFullName);
  }

  private void resetDraftAndLocationWhenToggledLocationManagement(List<TableChangeEvent> tableChangeEvents) {
    UUID facilityId = machine.getLocalFacilityId();
    FacilityExtension facilityExtension = facilityExtensionRepository.findByFacilityId(facilityId);
    boolean locationManagement = getEnableLocationManagement(facilityExtension);
    boolean toggledLocationManagement = false;
    for (TableChangeEvent tableChangeEvent : tableChangeEvents) {
      if (!tableChangeEvent.getTableName().equals(TABLE_NAME_FACILITY_EXTENSION)) {
        continue;
      }
      int indexOfFacilityId = tableChangeEvent.getColumns().indexOf(FIELD_FACILITY_ID);
      int indexOfLocationManagement = tableChangeEvent.getColumns().indexOf(FIELD_ENABLE_LOCATION_MANAGEMENT);
      for (RowChangeEvent rowChangeEvent : tableChangeEvent.getRowChangeEvents()) {
        if (!facilityId.toString().equals(rowChangeEvent.getValues().get(indexOfFacilityId))) {
          continue;
        }
        Boolean toBeUpdatedLocalManagement =
            escapeNullBoolean((Boolean) rowChangeEvent.getValues().get(indexOfLocationManagement));
        if (BooleanUtils.isTrue(locationManagement != toBeUpdatedLocalManagement)) {
          toggledLocationManagement = true;
          locationManagement = toBeUpdatedLocalManagement;
        }
      }
    }
    if (toggledLocationManagement) {
      administrationsService.deleteDrafts(facilityId);
      administrationsService.assignToVirtualLocation(facilityId, locationManagement, null);
    }
  }

  static Boolean getEnableLocationManagement(FacilityExtension facilityExtension) {
    if (Objects.isNull(facilityExtension)) {
      return Boolean.FALSE;
    }
    Boolean locationManagement = facilityExtension.getEnableLocationManagement();
    return escapeNullBoolean(locationManagement);
  }

  static Boolean escapeNullBoolean(Boolean locationManagement) {
    if (Objects.isNull(locationManagement)) {
      return Boolean.FALSE;
    }
    return locationManagement;
  }
}
