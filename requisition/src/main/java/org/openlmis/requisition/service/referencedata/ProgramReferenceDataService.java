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

package org.openlmis.requisition.service.referencedata;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;

@Service
public class ProgramReferenceDataService extends BaseReferenceDataService<ProgramDto> {

  @Override
  protected String getUrl() {
    return "/api/programs/";
  }

  @Override
  protected Class<ProgramDto> getResultClass() {
    return ProgramDto.class;
  }

  @Override
  protected Class<ProgramDto[]> getArrayResultClass() {
    return ProgramDto[].class;
  }

  /**
   * This method retrieves Programs with programName similar with name parameter.
   *
   * @param programName Field with string to find similar name.
   * @return List of ProgramDtos with similar programName.
   */
  public Collection<ProgramDto> search(String programName) {
    return findAll("search", RequestParameters.init().set("name", programName));
  }

  /**
   * This method retrieves program for given ids.
   *
   * @param programIds list of program ids.
   * @return List of ProgramDto.
   */
  public List<ProgramDto> search(Set<UUID> programIds) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("id", programIds);

    return findAll("", parameters);
  }

}
