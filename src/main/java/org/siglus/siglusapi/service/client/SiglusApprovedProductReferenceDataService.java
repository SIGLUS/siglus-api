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

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.openlmis.stockmanagement.dto.referencedata.ApprovedProductDto;
import org.openlmis.stockmanagement.dto.referencedata.OrderablesAggregator;
import org.openlmis.stockmanagement.service.referencedata.BaseReferenceDataService;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.domain.ProgramExtension;
import org.siglus.siglusapi.repository.ProgramExtensionRepository;
import org.siglus.siglusapi.repository.ProgramOrderableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private ProgramOrderableRepository programOrderableRepository;

  /**
   * Retrieves all facility approved products from the reference data service, based on the provided
   * facility and full supply flag. It can be optionally filtered by the program ID. The result is
   * wrapped to a separate class to improve the performance
   *
   * @param facilityId id of the facility
   * @param programId  id of the program
   * @return wrapped collection of approved products matching the search criteria
   */
  public OrderablesAggregator getApprovedProducts(UUID facilityId, UUID programId,
      Collection<UUID> orderableIds) {
    List<ApprovedProductDto> approvedProductDtos = getApprovedProductsByVirtualProgram(facilityId,
        programId, orderableIds);
    return new OrderablesAggregator(approvedProductDtos);
  }

  private List<ApprovedProductDto> getApprovedProductsByVirtualProgram(UUID facilityId,
      UUID programId,
      Collection<UUID> orderableIds) {
    List<ApprovedProductDto> approvedProducts = new ArrayList<>();
    ProgramExtension programExtension = programExtensionRepository.findByProgramId(programId);
    if (Boolean.FALSE.equals(programExtension.getIsVirtual())) {
      List<ApprovedProductDto> approvedProductDtos =
          getApprovedProductsByRealProgram(facilityId, programId, orderableIds).getContent();
      approvedProducts.addAll(approvedProductDtos);

    } else {
      List<ProgramExtension> realPrograms = programExtensionRepository.findByParentId(programId);
      for (ProgramExtension realProgram : realPrograms) {
        if (!programOrderableRepository.findByProgramId(realProgram.getProgramId()).isEmpty()) {
          List<ApprovedProductDto> approvedProductDtos =
              getApprovedProductsByRealProgram(facilityId, realProgram.getProgramId(), orderableIds)
                  .getContent();
          approvedProducts.addAll(approvedProductDtos);
        }
      }
    }
    return approvedProducts.stream()
        .filter(distinctByKey(approvedProductDto -> approvedProductDto.getOrderable().getId()))
        .collect(Collectors.toList());
  }

  private Page<ApprovedProductDto> getApprovedProductsByRealProgram(UUID facilityId, UUID programId,
      Collection<UUID> orderableIds) {
    RequestParameters params = RequestParameters.init();

    params.set(FieldConstants.PROGRAM_ID, programId);

    if (!isEmpty(orderableIds)) {
      params.set(FieldConstants.ORDERABLE_ID, orderableIds);
    }

    return getPage(facilityId + "/approvedProducts", params);
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }

}
