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

package org.openlmis.referencedata.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.openlmis.referencedata.domain.Code;
import org.openlmis.referencedata.domain.Facility;
import org.openlmis.referencedata.domain.FacilityOperator;
import org.openlmis.referencedata.domain.FacilityType;
import org.openlmis.referencedata.domain.GeographicZone;
import org.openlmis.referencedata.domain.Program;
import org.openlmis.referencedata.domain.SupportedProgram;
import org.openlmis.referencedata.domain.SupportedProgramPrimaryKey;
import org.openlmis.referencedata.dto.FacilityDto;
import org.openlmis.referencedata.dto.SupportedProgramDto;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.FacilityOperatorRepository;
import org.openlmis.referencedata.repository.FacilityRepository;
import org.openlmis.referencedata.repository.FacilityTypeRepository;
import org.openlmis.referencedata.repository.GeographicZoneRepository;
import org.openlmis.referencedata.repository.ProgramRepository;
import org.openlmis.referencedata.util.messagekeys.FacilityOperatorMessageKeys;
import org.openlmis.referencedata.util.messagekeys.FacilityTypeMessageKeys;
import org.openlmis.referencedata.util.messagekeys.GeographicZoneMessageKeys;
import org.openlmis.referencedata.util.messagekeys.ProgramMessageKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FacilityBuilder implements DomainResourceBuilder<FacilityDto, Facility> {

  @Autowired
  private GeographicZoneRepository geographicZoneRepository;

  @Autowired
  private FacilityTypeRepository facilityTypeRepository;

  @Autowired
  private FacilityOperatorRepository facilityOperatorRepository;

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private FacilityRepository facilityRepository;

  /**
   * Creates new {@link Facility} based on data from importer.
   */
  public Facility build(FacilityDto importer) {
    final GeographicZone geographicZone = findResource(
        geographicZoneRepository::findOne, importer.getGeographicZone(),
        GeographicZoneMessageKeys.ERROR_NOT_FOUND);
    final FacilityType facilityType = findResource(
        facilityTypeRepository::findOne, importer.getType(),
        FacilityTypeMessageKeys.ERROR_NOT_FOUND);
    final FacilityOperator facilityOperator = null == importer.getOperator()
        ? null
        : findResource(facilityOperatorRepository::findOne, importer.getOperator(),
            FacilityOperatorMessageKeys.ERROR_NOT_FOUND);

    Facility facility;

    if (null == importer.getId()) {
      facility = new Facility();
    } else {
      facility = facilityRepository.findOne(importer.getId());

      if (null == facility) {
        facility = new Facility();
        facility.setId(importer.getId());
      }
    }

    facility.updateFrom(importer);
    facility.setGeographicZone(geographicZone);
    facility.setType(facilityType);
    facility.setOperator(facilityOperator);
    addSupportedPrograms(importer.getSupportedPrograms(), facility);

    return facility;
  }

  private void addSupportedPrograms(Set<SupportedProgramDto> supportedPrograms,
      Facility facility) {
    if (null == supportedPrograms || supportedPrograms.isEmpty()) {
      return;
    }

    Set<SupportedProgram> newSupportedPrograms = supportedPrograms
        .stream()
        .map(dto -> createSupportedProgram(dto, facility))
        .collect(Collectors.toSet());

    facility.removeAllSupportedPrograms();
    newSupportedPrograms.forEach(facility::addSupportedProgram);
  }

  private SupportedProgram createSupportedProgram(SupportedProgramDto supportedProgram,
      Facility facility) {
    Program program = findProgram(supportedProgram);
    SupportedProgramPrimaryKey primaryKey = new SupportedProgramPrimaryKey(facility, program);

    return new SupportedProgram(
        primaryKey, supportedProgram.isSupportActive(),
        supportedProgram.isSupportLocallyFulfilled(),
        supportedProgram.getSupportStartDate());
  }

  private Program findProgram(SupportedProgramDto supportedProgram) {
    Program program;

    if (null != supportedProgram.getCode()) {
      program = programRepository.findByCode(Code.code(supportedProgram.getCode()));
    } else if (null != supportedProgram.getId()) {
      program = programRepository.findOne(supportedProgram.getId());
    } else {
      throw new ValidationMessageException(ProgramMessageKeys.ERROR_CODE_OR_ID_REQUIRED);
    }

    if (program == null) {
      throw new ValidationMessageException(ProgramMessageKeys.ERROR_NOT_FOUND);
    }

    return program;
  }

}
