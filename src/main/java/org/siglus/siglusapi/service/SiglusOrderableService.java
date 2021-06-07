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

package org.siglus.siglusapi.service;

import static org.siglus.siglusapi.constant.FieldConstants.CODE;
import static org.siglus.siglusapi.constant.FieldConstants.FULL_PRODUCT_NAME;
import static org.siglus.siglusapi.constant.FieldConstants.PRODUCT_CODE;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.siglus.common.domain.ProgramAdditionalOrderable;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.QueryOrderableSearchParams;
import org.siglus.common.repository.ProgramAdditionalOrderableRepository;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class SiglusOrderableService {

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private SiglusOrderableRepository siglusOrderableRepository;

  @Autowired
  private ProgramAdditionalOrderableRepository programAdditionalOrderableRepository;

  public Page<OrderableDto> searchOrderables(QueryOrderableSearchParams searchParams,
      Pageable pageable, UUID facilityId) {
    Page<OrderableDto> orderableDtoPage = orderableReferenceDataService
        .searchOrderables(searchParams, pageable);
    Set<String> archivedProducts = archiveProductService
        .searchArchivedProductsByFacilityId(facilityId);
    orderableDtoPage.getContent().forEach(orderableDto -> orderableDto
        .setArchived(archivedProducts.contains(orderableDto.getId().toString())));
    return orderableDtoPage;
  }

  public List<OrderableExpirationDateDto> getOrderableExpirationDate(Set<UUID> orderableIds,
      UUID facilityId) {
    return siglusOrderableRepository.findExpirationDate(orderableIds, facilityId);
  }

  public Page<OrderableDto> additionalToAdd(UUID programId, QueryOrderableSearchParams searchParams,
      Pageable pageable) {
    Pageable noPagination = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER,
        Pagination.NO_PAGINATION, pageable.getSort());
    List<OrderableDto> orderableDtos = orderableReferenceDataService
        .searchOrderables(searchParams, noPagination).getContent();
    Set<UUID> additionalOrderableIds = programAdditionalOrderableRepository
        .findAllByProgramId(programId)
        .stream()
        .map(ProgramAdditionalOrderable::getAdditionalOrderableId)
        .collect(Collectors.toSet());
    orderableDtos = orderableDtos.stream()
        .filter(orderableDto -> {
          if (!CollectionUtils.isEmpty(orderableDto.getPrograms())) {
            return !programId.equals(orderableDto.getPrograms().stream().findFirst().get()
                .getProgramId());
          }
          return false;
        })
        .filter(orderableDto -> !additionalOrderableIds.contains(orderableDto.getId()))
        .collect(Collectors.toList());
    if (null == pageable.getSort()) {
      return Pagination.getPage(orderableDtos, pageable);
    }
    if (pageable.getSort().toString().contains(FULL_PRODUCT_NAME)) {
      orderableDtos = orderableDtos.stream()
          .sorted(Comparator.comparing(OrderableDto::getFullProductName))
          .collect(Collectors.toList());
    }
    if (pageable.getSort().toString().contains(PRODUCT_CODE)) {
      orderableDtos = orderableDtos.stream()
          .sorted(Comparator.comparing(OrderableDto::getProductCode))
          .collect(Collectors.toList());
    }
    return Pagination.getPage(orderableDtos, pageable);
  }

  public OrderableDto getOrderableByCode(String productCode) {
    Pageable noPagination = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER,
        Pagination.NO_PAGINATION);
    MultiValueMap<String, Object> queryParams = new LinkedMultiValueMap<>();
    queryParams.set(CODE, productCode);
    QueryOrderableSearchParams searchParams = new QueryOrderableSearchParams(queryParams);
    List<OrderableDto> orderableDtos = orderableReferenceDataService
        .searchOrderables(searchParams, noPagination).getContent();
    return orderableDtos.stream()
        .filter(orderableDto -> productCode.equals(orderableDto.getProductCode()))
        .findFirst()
        .orElse(null);
  }

}
