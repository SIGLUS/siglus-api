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

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.service.RequestParameters;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class ApprovedProductReferenceDataService extends
    BaseReferenceDataService<ApprovedProductDto> {

  // [SIGLUS change start]
  // [change reason]: support for additional product
  @Autowired
  private ProgramOrderableRepository programOrderableRepository;

  @Autowired
  private ProgramAdditionalOrderableRepository additionalOrderableRepository;
  // [SIGLUS change end]

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

  /**
   * Retrieves all facility approved products from the reference data service, based on the provided
   * facility and full supply flag.
   *
   * @param facilityId id of the facility
   * @param programId  id of the program
   * @return a collection of approved products matching the search criteria
   */
  public ApproveProductsAggregator getApprovedProducts(UUID facilityId, UUID programId) {
    RequestParameters params = RequestParameters.init();

    params.set("programId", programId);
    params.set("size", Integer.MAX_VALUE);

    // [SIGLUS change start]
    // [change reason]: support for additional product
    if (!programOrderableRepository.findByProgramId(programId).isEmpty()) {
      // [SIGLUS change end]

      Page<ApprovedProductDto> page = getPage(facilityId + "/approvedProducts", params);
      List<ApprovedProductDto> content = page.getContent();
      return new ApproveProductsAggregator(content, programId);

      // [SIGLUS change start]
      // [change reason]: support for additional product
    }
    return new ApproveProductsAggregator(Collections.EMPTY_LIST, programId);
    // [SIGLUS change end]
  }

  // [SIGLUS change start]
  // [change reason]: support for additional product

  public Page<ApprovedProductDto> getApprovedProducts(UUID facilityId, UUID programId,
      Collection<UUID> orderableIds) {
    RequestParameters params = RequestParameters.init();

    params.set("programId", programId);

    if (!CollectionUtils.isEmpty(orderableIds)) {
      params.set("orderableId", orderableIds);
    }

    return getPage(facilityId + "/approvedProducts", params);
  }

  public ApproveProductsAggregator getAdditionalApprovedProducts(UUID facilityId, UUID programId) {
    List<ProgramAdditionalOrderable> additionalOrderables =
        additionalOrderableRepository.findAllByProgramId(programId);

    List<ApprovedProductDto> approvedProducts = new ArrayList<>();
    if (!CollectionUtils.isEmpty(additionalOrderables)) {
      Map<UUID, List<ProgramAdditionalOrderable>> additionalMap =
          additionalOrderables.stream().collect(groupingBy(
              ProgramAdditionalOrderable::getOrderableOriginProgramId));
      for (UUID orderableOriginProgramId : additionalMap.keySet()) {
        List<UUID> orderableIds = additionalMap.get(orderableOriginProgramId)
            .stream()
            .map(orderable ->
                orderable.getAdditionalOrderableId())
            .collect(Collectors.toList());
        approvedProducts
            .addAll(getApprovedProducts(facilityId, orderableOriginProgramId, orderableIds)
                .getContent());
      }
    }

    return new ApproveProductsAggregator(approvedProducts, programId);
  }
  // [SIGLUS change end]

}
