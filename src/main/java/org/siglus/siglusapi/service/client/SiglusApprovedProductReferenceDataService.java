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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderablesAggregator;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.siglus.common.service.client.BaseReferenceDataService;
import org.siglus.common.util.RequestParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
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
      List<ApprovedProductDto> approvedProducts = new ArrayList<>();
      List<ProgramAdditionalOrderable> additionalOrderables =
          additionalOrderableRepository.findAllByProgramId(programId);
      if (!additionalOrderables.isEmpty()) {
        Map<UUID, List<ProgramAdditionalOrderable>> additionalMap =
            additionalOrderables.stream()
                .filter(additionalOrderable ->
                    orderableIds.contains(additionalOrderable.getAdditionalOrderableId()))
                .collect(groupingBy(ProgramAdditionalOrderable::getOrderableOriginProgramId));
        additionalMap.forEach((key, value) -> {
          List<UUID> orderableIdsForProgram = value.stream()
              .map(ProgramAdditionalOrderable::getAdditionalOrderableId)
              .collect(Collectors.toList());
          approvedProducts.addAll(getRequisitionApprovedProducts(facilityId, key,
              orderableIdsForProgram));
        });
      }
      return approvedProducts;
    } else {
      return getRequisitionApprovedProducts(facilityId, programId, orderableIds);
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
  @Cacheable("siglus~api~approved~products")
  public OrderablesAggregator getApprovedProducts(UUID facilityId, UUID programId,
      Collection<UUID> orderableIds) {
    RequestParameters params = RequestParameters.init();

    params.set("programId", programId);

    if (!isEmpty(orderableIds)) {
      params.set("orderableId", orderableIds);
    }
    if (!programOrderableRepository.findByProgramId(programId).isEmpty()) {
      Page<org.openlmis.stockmanagement.dto.referencedata.ApprovedProductDto> approvedProductPage =
          getPage(facilityId + "/approvedProducts", params, null, HttpMethod.GET,
              org.openlmis.stockmanagement.dto.referencedata.ApprovedProductDto.class);
      return new OrderablesAggregator(new ArrayList<>(approvedProductPage.getContent()));
    }
    return new OrderablesAggregator(new ArrayList<>());
  }

  private List<ApprovedProductDto> getRequisitionApprovedProducts(UUID facilityId, UUID programId,
      Collection<UUID> orderableIds) {
    RequestParameters params = RequestParameters.init();

    params.set("programId", programId);

    if (!isEmpty(orderableIds)) {
      params.set("orderableId", orderableIds);
    }
    if (!programOrderableRepository.findByProgramId(programId).isEmpty()) {

      return getPage(facilityId + "/approvedProducts", params).getContent();
    }
    return Collections.emptyList();
  }

}
