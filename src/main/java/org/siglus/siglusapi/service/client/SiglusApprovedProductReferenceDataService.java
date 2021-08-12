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

package org.siglus.siglusapi.service.client;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.siglus.common.constant.CacheConstants.SIGLUS_APPROVED_PRODUCTS_BY_ORDERABLES;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.siglus.common.service.client.BaseReferenceDataService;
import org.siglus.common.util.RequestParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SiglusApprovedProductReferenceDataService extends
    BaseReferenceDataService<ApprovedProductDto> {

  @Override
  protected String getUrl() {
    return "/api/facilities/";
  }

  @Override
  protected Class<ApprovedProductDto> getResultClass() {
    return ApprovedProductDto.class;
  }

  @Override
  protected Class<ApprovedProductDto[]> getArrayResultClass() {
    return ApprovedProductDto[].class;
  }

  @Autowired
  private ProgramOrderableRepository programOrderableRepository;

  @Autowired
  private ProgramAdditionalOrderableRepository additionalOrderableRepository;


  public List<ApprovedProductDto> getApprovedProducts(UUID facilityId, UUID programId,
      Collection<UUID> orderableIds, boolean reportOnly) {
    if (reportOnly) {
      return additionalOrderableRepository.findAllByProgramId(programId).stream()
          .filter(o -> orderableIds.contains(o.getAdditionalOrderableId()))
          .collect(groupingBy(ProgramAdditionalOrderable::getOrderableOriginProgramId))
          .entrySet().stream()
          .map(e -> getAdditionalApprovedProducts(facilityId, e.getKey(), e.getValue()))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    } else {
      return getApprovedProducts(facilityId, programId, orderableIds);
    }
  }

  /**
   * Retrieves all facility approved products fromGroups the reference data service, based on the
   * provided facility and full supply flag. It can be optionally filtered by the program ID. The
   * result is wrapped to a separate class to improve the performance
   *
   * @param facilityId id of the facility
   * @param programId  id of the program
   * @return wrapped collection of approved products matching the search criteria
   */
  @Cacheable(value = SIGLUS_APPROVED_PRODUCTS_BY_ORDERABLES, keyGenerator = "cacheKeyGenerator")
  public List<ApprovedProductDto> getApprovedProducts(UUID facilityId, UUID programId, Collection<UUID> orderableIds) {
    RequestParameters params = RequestParameters.init();

    params.set("programId", programId);

    if (!isEmpty(orderableIds)) {
      params.set("orderableId", orderableIds);
    }
    if (programOrderableRepository.countByProgramId(programId) > 0) {

      return getPage(facilityId + "/approvedProducts", params).getContent();
    }
    return Collections.emptyList();
  }

  private List<ApprovedProductDto> getAdditionalApprovedProducts(UUID facilityId,
      UUID programId, Collection<ProgramAdditionalOrderable> orderables) {
    RequestParameters params = RequestParameters.init();

    params.set("programId", programId);

    if (!isEmpty(orderables)) {
      List<UUID> orderableIds = orderables.stream()
          .map(ProgramAdditionalOrderable::getAdditionalOrderableId)
          .collect(Collectors.toList());
      params.set("orderableId", orderableIds);
    }
    if (programOrderableRepository.countByProgramId(programId) > 0) {

      return getPage(facilityId + "/approvedProducts", params).getContent();
    }
    return Collections.emptyList();
  }

}
