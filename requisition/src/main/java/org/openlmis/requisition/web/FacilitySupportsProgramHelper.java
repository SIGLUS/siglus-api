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

package org.openlmis.requisition.web;

import static org.openlmis.requisition.i18n.MessageKeys.ERROR_FACILITY_DOES_NOT_SUPPORT_PROGRAM;

import java.util.List;
import java.util.UUID;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.SupportedProgramDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.service.referencedata.FacilityReferenceDataService;
import org.openlmis.requisition.utils.DateHelper;
import org.openlmis.requisition.utils.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class FacilitySupportsProgramHelper {

  @Autowired
  FacilityReferenceDataService facilityReferenceDataService;

  @Autowired
  DateHelper dateHelper;

  /**
   * Method check if facility supports program.
   *
   * @param facilityId UUID of the Facility.
   * @param programId UUID of the Program.
   */
  void checkIfFacilitySupportsProgram(UUID facilityId, UUID programId) {
    FacilityDto facility = facilityReferenceDataService.findOne(facilityId);
    checkIfFacilitySupportsProgram(facility, programId);
  }

  /**
   * Method check if facility supports program.
   *
   * @param facility Facility.
   * @param programId programId UUID of the Program.
   */
  void checkIfFacilitySupportsProgram(FacilityDto facility, UUID programId) {
    List<SupportedProgramDto> supportedPrograms = facility.getSupportedPrograms();

    if (!isProgramSupported(supportedPrograms, programId)) {
      throw new ValidationMessageException(
          new Message(ERROR_FACILITY_DOES_NOT_SUPPORT_PROGRAM, facility.getId(), programId));
    }
  }

  /**
   * Find a supported program entry from the given facility for the given program. If entry does
   * not exist the {@code null} value will be returned.
   */
  SupportedProgramDto getSupportedProgram(FacilityDto facility, UUID programId) {
    return facility
        .getSupportedPrograms()
        .stream()
        .filter(supportedProgram -> find(supportedProgram, programId))
        .findFirst()
        .orElse(null);
  }

  private boolean isProgramSupported(List<SupportedProgramDto> supportedPrograms,
                                     UUID programId) {
    return supportedPrograms
        .stream()
        .anyMatch(supportedProgram -> find(supportedProgram, programId));
  }

  private boolean find(SupportedProgramDto supportedProgram, UUID programId) {
    return supportedProgram.getId().equals(programId)
        && supportedProgram.isSupportActive() && supportedProgram.isProgramActive()
        && dateHelper.isDateBeforeNow(supportedProgram.getSupportStartDate());
  }

}
