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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.service.RequestParameters;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.common.repository.ProgramOrderableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class ApprovedProductReferenceDataService extends
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

  // [SIGLUS change start]
  // [change reason]: need support virtual program
  @Autowired
  public ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private ProgramOrderableRepository programOrderableRepository;
  // [SIGLUS change end]

  /**
   * Retrieves all facility approved products from the reference data service, based on the
   * provided facility and full supply flag.
   *
   * @param facilityId id of the facility
   * @param programId  id of the program
   * @return a collection of approved products matching the search criteria
   */
  public ApproveProductsAggregator getApprovedProducts(UUID facilityId, UUID programId) {
    // [SIGLUS change start]
    // [change reason]: need support virtual program

    List<ApprovedProductDto> approvedProductDtos = getApprovedProductsByVirtualProgram(facilityId,
        programId);
    return new ApproveProductsAggregator(approvedProductDtos, programId);
  }

  private List<ApprovedProductDto> getApprovedProductsByVirtualProgram(UUID facilityId,
      UUID programId) {
    List<ApprovedProductDto> approvedProducts = new ArrayList<>();
    ProgramExtension programExtension = programExtensionRepository.findByProgramId(programId);
    if (Boolean.FALSE.equals(programExtension.getIsVirtual())) {
      List<ApprovedProductDto> approvedProductDtos =
          getApprovedProductsByRealProgram(facilityId, programId).getFullSupplyProducts();
      approvedProducts.addAll(approvedProductDtos);

    } else {
      List<ProgramExtension> realPrograms = programExtensionRepository.findByParentId(programId);
      for (ProgramExtension realProgram : realPrograms) {
        if (!programOrderableRepository.findByProgramId(realProgram.getProgramId()).isEmpty()) {
          List<ApprovedProductDto> approvedProductDtos =
              getApprovedProductsByRealProgram(facilityId, realProgram.getProgramId())
                  .getFullSupplyProducts();
          approvedProducts.addAll(approvedProductDtos);
        }
      }
    }
    return approvedProducts.stream()
        .filter(distinctByKey(approvedProductDto -> approvedProductDto.getOrderable().getId()))
        .collect(Collectors.toList());
  }

  private ApproveProductsAggregator getApprovedProductsByRealProgram(UUID facilityId,
      UUID programId) {
    // [SIGLUS change end]
    RequestParameters params = RequestParameters.init();
    params.set("programId", programId);
    params.set("size", Integer.MAX_VALUE);

    Page<ApprovedProductDto> page = getPage(facilityId + "/approvedProducts", params);
    List<ApprovedProductDto> content = page.getContent();

    return new ApproveProductsAggregator(content, programId);
  }

  // [SIGLUS change start]
  // [change reason]: add new distinct function
  private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
  // [SIGLUS change end]

}
