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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.siglus.common.domain.ProgramExtension;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.QueryOrderableSearchParams;
import org.siglus.common.repository.ProgramExtensionRepository;
import org.siglus.common.util.referencedata.Pagination;
import org.siglus.siglusapi.dto.OrderableExpirationDateDto;
import org.siglus.siglusapi.dto.SiglusOrderableDto;
import org.siglus.siglusapi.dto.SiglusProgramOrderableDto;
import org.siglus.siglusapi.repository.SiglusOrderableRepository;
import org.siglus.siglusapi.service.client.SiglusOrderableReferenceDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusOrderableService {

  @Autowired
  private SiglusOrderableReferenceDataService orderableReferenceDataService;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Autowired
  private ProgramExtensionRepository programExtensionRepository;

  @Autowired
  private SiglusOrderableRepository siglusOrderableRepository;

  public Page<SiglusOrderableDto> searchOrderables(QueryOrderableSearchParams searchParams,
      Pageable pageable, UUID facilityId) {
    Map<UUID, ProgramExtension> programExtensions =
        programExtensionRepository.findAll().stream().collect(Collectors.toMap(
            ProgramExtension::getProgramId,
            programExtension -> programExtension));
    Page<OrderableDto> orderableDtoPage = orderableReferenceDataService
        .searchOrderables(searchParams, pageable);
    Set<String> archivedProducts = archiveProductService.searchArchivedProducts(facilityId);
    List<SiglusOrderableDto> siglusOrderableDtos = newArrayList();
    orderableDtoPage.getContent().forEach(orderableDto -> {
      SiglusOrderableDto siglusOrderableDto = new SiglusOrderableDto();
      siglusOrderableDtos.add(siglusOrderableDto);
      BeanUtils.copyProperties(orderableDto, siglusOrderableDto);
      Set<SiglusProgramOrderableDto> siglusProgramOrderableDtos = newHashSet();
      orderableDto.getPrograms().forEach(programOrderableDto -> {
        SiglusProgramOrderableDto siglusProgramOrderableDto = new SiglusProgramOrderableDto();
        siglusProgramOrderableDtos.add(siglusProgramOrderableDto);
        BeanUtils.copyProperties(programOrderableDto, siglusProgramOrderableDto);
      });
      siglusOrderableDto.setPrograms(siglusProgramOrderableDtos);
      siglusOrderableDto.setArchived(false);
      if (archivedProducts.contains(orderableDto.getId().toString())) {
        siglusOrderableDto.setArchived(true);
      }
      siglusOrderableDto.getPrograms()
          .forEach(programOrderableDto -> programOrderableDto.setParentId(
              programExtensions.get(programOrderableDto.getProgramId()).getParentId()));
    });
    return Pagination.getPage(siglusOrderableDtos, pageable,
        orderableDtoPage.getNumberOfElements());
  }

  public List<OrderableExpirationDateDto> getOrderableExpirationDate(Set<UUID> orderableIds) {
    return siglusOrderableRepository.findExpirationDate(orderableIds);
  }
}
