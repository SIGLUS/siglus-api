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

import static org.siglus.siglusapi.constant.ProgramConstants.ALL_PRODUCTS_PROGRAM_ID;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.openlmis.referencedata.domain.ProgramOrderable;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.DispensableDto;
import org.openlmis.requisition.service.RequisitionService;
import org.siglus.common.domain.ProgramOrderablesExtension;
import org.siglus.common.repository.ProgramOrderablesExtensionRepository;
import org.siglus.siglusapi.dto.ProgramProductDto;
import org.siglus.siglusapi.dto.SupportedProgramDto;
import org.siglus.siglusapi.repository.SiglusProgramOrderableRepository;
import org.siglus.siglusapi.util.SupportedProgramsHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class SiglusApprovedProductService {

  @Autowired
  private SupportedProgramsHelper supportedProgramsHelper;

  @Autowired
  private RequisitionService requisitionService;

  @Autowired
  private SiglusProgramOrderableRepository siglusProgramOrderableRepository;

  @Autowired
  private ProgramOrderablesExtensionRepository programOrderablesExtensionRepository;

  @Autowired
  private SiglusArchiveProductService archiveProductService;

  @Deprecated
  public List<ApprovedProductDto> getApprovedProducts(UUID facilityId, UUID programId) {
    List<SupportedProgramDto> supportedPrograms = getAndCheckProgram(programId);
    List<ApprovedProductDto> approvedProducts = getApprovedProducts(facilityId, supportedPrograms);
    Set<UUID> orderableIds = approvedProducts.stream()
        .map(product -> product.getOrderable().getId()).collect(Collectors.toSet());
    if (!ObjectUtils.isEmpty(orderableIds)) {
      Map<UUID, ProgramOrderablesExtension> extensionMap =
          programOrderablesExtensionRepository.findAllByOrderableIdIn(orderableIds)
              .stream().collect(Collectors.toMap(ProgramOrderablesExtension::getOrderableId, item -> item));
      approvedProducts.forEach(
          product -> {
            ProgramOrderablesExtension extension = extensionMap.get(product.getOrderable().getId());
            if (!ObjectUtils.isEmpty(extension)) {
              product.getOrderable().setDispensable(new DispensableDto(extension.getUnit(), extension.getUnit()));
            }
          }
      );
    }
    return approvedProducts;
  }

  private List<ApprovedProductDto> getApprovedProducts(UUID facilityId, List<SupportedProgramDto> supportedPrograms) {
    return supportedPrograms.stream().map(programDto ->
            requisitionService.getApprovedProducts(facilityId, programDto.getId()))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  public List<ProgramProductDto> getApprovedProductsForFacility(UUID facilityId, UUID programId) {
    List<SupportedProgramDto> programDtos = getAndCheckProgram(programId);
    Set<UUID> archivedProductSet = archiveProductService.searchArchivedProductsByFacilityId(facilityId)
        .stream().map(UUID::fromString).collect(Collectors.toSet());
    List<UUID> programIds = programDtos.stream().map(SupportedProgramDto::getId).collect(Collectors.toList());
    List<ProgramOrderable> programOrderables =
        siglusProgramOrderableRepository.findMaxVersionOrderableByProgramIds(programIds)
            .stream()
            .filter(programOrderable -> !archivedProductSet.contains(programOrderable.getProduct().getId()))
            .collect(Collectors.toList());
    Set<UUID> orderableIds = programOrderables.stream()
        .map(product -> product.getProduct().getId()).collect(Collectors.toSet());
    Map<UUID, ProgramOrderablesExtension> extensionMap =
        programOrderablesExtensionRepository.findAllByOrderableIdIn(orderableIds)
            .stream().collect(Collectors.toMap(ProgramOrderablesExtension::getOrderableId, Function.identity()));
    return programOrderables.stream().map(
        programOrderable -> {
          ProgramOrderablesExtension extension = extensionMap.get(programOrderable.getProduct().getId());
          return new ProgramProductDto(programOrderable, extension);
        })
        .collect(Collectors.toList());
  }

  private List<SupportedProgramDto> getAndCheckProgram(UUID programId) {
    List<SupportedProgramDto> supportedPrograms = supportedProgramsHelper.findHomeFacilitySupportedPrograms();
    if (!ALL_PRODUCTS_PROGRAM_ID.equals(programId)) {
      Optional<SupportedProgramDto> programDto = supportedPrograms.stream()
          .filter(program -> Objects.equals(program.getId(), programId)).findFirst();
      if (programDto.isPresent()) {
        supportedPrograms = Collections.singletonList(programDto.get());
      } else {
        throw new IllegalArgumentException("unsupported program: " + programId);
      }
    }
    return supportedPrograms;
  }
}
