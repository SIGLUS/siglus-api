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

package org.openlmis.stockmanagement.service.referencedata;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.stockmanagement.dto.referencedata.OrderableFulfillDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockmanagementOrderableFulfillReferenceDataService extends
    BaseReferenceDataService<Map> {

  @Override
  protected String getUrl() {
    return "/api/orderableFulfills/";
  }

  @Override
  protected Class<Map> getResultClass() {
    return Map.class;
  }

  @Override
  protected Class<Map[]> getArrayResultClass() {
    return Map[].class;
  }

  // [SIGLUS change start]
  // [change reason]: support "virtual" program.
  @Autowired
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private ProgramOrderableRepository programOrderableRepository;
  // [SIGLUS change end]

  /**
   * Finds orderables by their ids.
   *
   * @param ids ids to look for.
   * @return a page of orderables
   */
  public Map<UUID, OrderableFulfillDto> findByIds(Collection<UUID> ids) {
    RequestParameters parameters = RequestParameters
        .init()
        .set("id", ids);

    return getMap(null, parameters, UUID.class, OrderableFulfillDto.class);
  }

  /**
   * Finds orderables by facilityId and programId.
   *
   * @param facilityId id of facility which is used during searching orderables
   * @param programId id of program which is used during searching orderables
   * @return a page of orderables
   */
  public Map<UUID, OrderableFulfillDto> findByFacilityIdProgramId(UUID facilityId, UUID programId) {
    // [SIGLUS change start]
    // [change reason]: support "virtual" program.
    ProgramExtension programExtension = programExtensionRepository.findByProgramId(programId);
    if (Boolean.FALSE.equals(programExtension.getIsVirtual())) {
      return findByFacilityIdRealProgramId(facilityId, programId);
    }
    List<ProgramExtension> realPrograms = programExtensionRepository.findByParentId(programId);
    Map<UUID, OrderableFulfillDto> fulfillDtoMaps = Maps.newHashMap();
    for (ProgramExtension realProgram : realPrograms) {
      Map<UUID, OrderableFulfillDto> fulfillDtoMap =
          findByFacilityIdRealProgramId(facilityId, realProgram.getProgramId());
      fulfillDtoMaps.putAll(fulfillDtoMap);
    }
    return fulfillDtoMaps;
  }

  private Map<UUID, OrderableFulfillDto> findByFacilityIdRealProgramId(UUID facilityId,
      UUID programId) {
    if (programOrderableRepository.findByProgramId(programId).isEmpty()) {
      return new HashMap<>();
    }
    // [SIGLUS change end]
    RequestParameters parameters = RequestParameters
        .init()
        .set("facilityId", facilityId)
        .set("programId", programId);

    return getMap(null, parameters, UUID.class, OrderableFulfillDto.class);
  }
}
