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

package org.openlmis.requisition.web;

import static org.openlmis.requisition.dto.ReasonDto.newInstance;
import static org.openlmis.requisition.web.RequisitionV2Controller.RESOURCE_URL;
import static org.openlmis.requisition.web.ResourceNames.FACILITIES;
import static org.openlmis.requisition.web.ResourceNames.ORDERABLES;
import static org.openlmis.requisition.web.ResourceNames.PROCESSING_PERIODS;
import static org.openlmis.requisition.web.ResourceNames.PROGRAMS;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openlmis.requisition.domain.requisition.ApprovedProductReference;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionLineItem;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionTemplateDto;
import org.openlmis.requisition.dto.MetadataDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.RequisitionLineItemV2Dto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.dto.VersionObjectReferenceDto;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Transactional
@RestController
@RequestMapping(RESOURCE_URL)
public class RequisitionV2Controller extends BaseRequisitionController {

  public static final String RESOURCE_URL = API_URL + "/v2/requisitions";

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Allows creating new requisitions.
   *
   * @param programId UUID of Program.
   * @param facilityId UUID of Facility.
   * @param emergency Emergency status.
   * @param suggestedPeriod Period for requisition.
   * @return created requisition.
   */
  @PostMapping("/initiate")
  @ResponseStatus(HttpStatus.CREATED)
  public RequisitionV2Dto initiate(@RequestParam(value = "program") UUID programId,
      @RequestParam(value = "facility") UUID facilityId,
      @RequestParam(value = "suggestedPeriod", required = false) UUID suggestedPeriod,
      @RequestParam(value = "emergency") boolean emergency,
      HttpServletRequest request, HttpServletResponse response) {

    Profiler profiler = getProfiler(
        "POST_REQUISITION_INITIATE_V2",
        programId, facilityId, suggestedPeriod, emergency
    );

    InitiateResult result = doInitiate(programId, facilityId, suggestedPeriod, emergency,
        request, profiler);
    Requisition requisition = result.getRequisition();

    RequisitionV2Dto dto = buildDto(requisition, profiler);

    addLocationHeader(request, response, dto.getId(), profiler);

    stopProfiler(profiler, dto);

    return dto;
  }

  /**
   * Allows updating requisitions.
   *
   * @param requisitionId UUID of requisition which we want to update.
   * @param requisitionDto A requisitionDto bound to the request body.
   * @return updated requisition.
   */
  @PutMapping("/{id}")
  public RequisitionV2Dto updateRequisition(@PathVariable("id") UUID requisitionId,
      @RequestBody RequisitionV2Dto requisitionDto,
      HttpServletRequest request, HttpServletResponse response) {
    Profiler profiler = getProfiler("UPDATE_REQUISITION_V2", requisitionId, requisitionDto);

    UpdatePreparationResult result = doUpdatePreparation(requisitionId, requisitionDto,
        request, profiler);

    Requisition requisitionToUpdate = result.getRequisitionToUpdate();

    logger.debug("Updating requisition with id: {}", requisitionId);

    profiler.start("UPDATE");
    requisitionToUpdate.updateFrom(result.getRequisition(),
        result.getOrderables(), result.getApprovedProducts(),
        datePhysicalStockCountCompletedEnabledPredicate.exec(result.getProgram()));

    profiler.start("SAVE");
    requisitionRepository.save(requisitionToUpdate);
    logger.debug("Requisition with id {} saved", requisitionToUpdate.getId());

    ETagResource<RequisitionV2Dto> etaggedResource = new ETagResource<>(
        buildDto(requisitionToUpdate, profiler),
        requisitionToUpdate.getVersion());

    response.setHeader(HttpHeaders.ETAG, etaggedResource.getEtag());

    stopProfiler(profiler, etaggedResource.getResource());

    return etaggedResource.getResource();
  }

  /**
   * Get chosen requisition.
   *
   * @param requisitionId UUID of requisition whose we want to get
   * @return Requisition.
   */
  @GetMapping("/{id}")
  public RequisitionV2Dto getRequisition(@PathVariable("id") UUID requisitionId,
      HttpServletResponse response) {
    Profiler profiler = getProfiler("GET_REQUISITION_V2", requisitionId);

    Requisition requisition = findRequisition(requisitionId, profiler);

    checkPermission(profiler, () -> permissionService.canViewRequisition(requisition));

    RequisitionV2Dto dto = buildDto(requisition, profiler);
    response.setHeader(HttpHeaders.ETAG, ETagResource.buildWeakETag(requisition.getVersion()));

    stopProfiler(profiler, dto);

    return dto;
  }

  private RequisitionV2Dto buildDto(Requisition requisition, Profiler profiler) {
    profiler.start("BUILD_DTO");
    RequisitionV2Dto dto = new RequisitionV2Dto();
    requisition.export(dto);

    dto.setTemplate(BasicRequisitionTemplateDto.newInstance(requisition.getTemplate()));
    dto.setFacility(new ObjectReferenceDto(requisition.getFacilityId(), serviceUrl, FACILITIES));
    dto.setProcessingPeriod(new ObjectReferenceDto(requisition.getProcessingPeriodId(),
        serviceUrl, PROCESSING_PERIODS));
    dto.setProgram(new ObjectReferenceDto(requisition.getProgramId(), serviceUrl, PROGRAMS));

    List<RequisitionLineItem> requisitionLineItems = requisition.getRequisitionLineItems();
    List<RequisitionLineItemV2Dto> lineItems = requisitionLineItems
        .stream()
        .map(line -> {
          // The whole object is not required here
          OrderableDto orderable = new OrderableDto();
          orderable.setId(line.getOrderable().getId());
          orderable.setMeta(new MetadataDto(line.getOrderable().getVersionNumber(), null));

          ApprovedProductDto approvedProduct = new ApprovedProductDto(
              line.getFacilityTypeApprovedProduct().getId(), null, null, null,
              null, null, new MetadataDto(
              line.getFacilityTypeApprovedProduct().getVersionNumber(), null));

          RequisitionLineItemV2Dto lineDto = new RequisitionLineItemV2Dto();
          lineDto.setServiceUrl(serviceUrl);
          line.export(lineDto, orderable, approvedProduct);

          return lineDto;
        })
        .collect(Collectors.toList());

    dto.setRequisitionLineItems(lineItems);

    Set<VersionObjectReferenceDto> availableProducts = new HashSet<>();

    Optional
        .ofNullable(requisition.getAvailableProducts())
        .orElse(Collections.emptySet())
        .stream()
        .map(ApprovedProductReference::getOrderable)
        .forEach(orderable -> {
          VersionObjectReferenceDto reference = new VersionObjectReferenceDto(
              orderable.getId(), serviceUrl, ORDERABLES, orderable.getVersionNumber());

          availableProducts.add(reference);
        });

    dto.setAvailableProducts(availableProducts);
    dto.setStockAdjustmentReasons(newInstance(requisition.getStockAdjustmentReasons()));
    return dto;
  }

}
