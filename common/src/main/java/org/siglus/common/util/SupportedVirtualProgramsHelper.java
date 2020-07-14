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

package org.siglus.common.util;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.domain.referencedata.Facility;
import org.siglus.common.domain.referencedata.SupportedProgram;
import org.siglus.common.repository.FacilityRepository;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupportedVirtualProgramsHelper {

  @Autowired
  private SiglusAuthenticationHelper authenticationHelper;

  @Autowired
  private FacilityRepository facilityRepository;

  @Autowired
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private SiglusDateHelper dateHelper;

  public Set<UUID> findUserSupportedVirtualPrograms() {
    UUID homeFacilityId = authenticationHelper.getCurrentUser().getHomeFacilityId();
    Facility homeFacility = facilityRepository.findOne(homeFacilityId);
    Set<UUID> supportedPrograms = homeFacility.getSupportedPrograms()
        .stream()
        .filter(supportedProgramDto -> {
          LocalDate supportStartDate = supportedProgramDto.getStartDate();
          return supportedProgramDto.getFacilityProgram().getProgram().getActive()
              && supportedProgramDto.getActive()
              && supportStartDate.isBefore(dateHelper.getCurrentDate());
        })
        .map(SupportedProgram::programId)
        .collect(Collectors.toSet());
    List<ProgramExtension> programExtensions = programExtensionRepository.findAll();
    return programExtensions.stream()
        .filter(programExtension -> supportedPrograms.contains(programExtension.getProgramId()))
        .map(ProgramExtension::getParentId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

}
