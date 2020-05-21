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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.FacilityDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProcessingPeriodDto;
import org.openlmis.requisition.dto.ProgramDto;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.dto.RequisitionDto;
import org.openlmis.requisition.dto.RequisitionPeriodDto;
import org.openlmis.requisition.dto.RequisitionWithSupplyingDepotsDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.dto.UserDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.i18n.MessageKeys;
import org.openlmis.requisition.repository.custom.DefaultRequisitionSearchParams;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.service.RequisitionStatusNotifier;
import org.openlmis.requisition.service.referencedata.SupervisoryNodeReferenceDataService;
import org.openlmis.requisition.utils.Message;
import org.openlmis.requisition.utils.Pagination;
import org.slf4j.profiler.Profiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("PMD.TooManyMethods")
@Controller
@Transactional
public class RequisitionController extends BaseRequisitionController {

  private static final String BUILD_DTO_LIST = "BUILD_DTO_LIST";

  @Autowired
  private RequisitionStatusNotifier requisitionStatusNotifier;

  @Autowired
  private SupervisoryNodeReferenceDataService supervisoryNodeService;

  /**
   * Allows creating new requisitions.
   *
   * @param programId UUID of Program.
   * @param facilityId UUID of Facility.
   * @param emergency Emergency status.
   * @param suggestedPeriod Period for requisition.
   * @return created requisition.
   */
  @PostMapping(RESOURCE_URL + "/initiate")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public RequisitionDto initiate(@RequestParam(value = "program") UUID programId,
      @RequestParam(value = "facility") UUID facilityId,
      @RequestParam(value = "suggestedPeriod", required = false) UUID suggestedPeriod,
      @RequestParam(value = "emergency") boolean emergency,
      HttpServletRequest request,
      HttpServletResponse response) {

    Profiler profiler = getProfiler(
        "POST_REQUISITION_INITIATE",
        programId, facilityId, suggestedPeriod, emergency
    );

    InitiateResult result = doInitiate(programId, facilityId, suggestedPeriod, emergency,
        request, profiler);

    RequisitionDto requisitionDto = buildDto(
        profiler, result.getRequisition(),
        findOrderables(profiler, () -> result.getRequisition().getAllOrderables()),
        result.getApproveProducts().getAllGroupByIdentity(),
        result.getFacility(), result.getProgram(), result.getPeriod()
    );

    addLocationHeader(request, response, requisitionDto.getId(), profiler);

    stopProfiler(profiler, requisitionDto);

    return requisitionDto;
  }

  /**
   * Returns processing periods for unprocessed requisitions.
   *
   * @param programId UUID of the Program.
   * @param facilityId UUID of the Facility.
   * @param emergency true for periods to initiate an emergency requisition; false otherwise.
   * @return processing periods.
   */
  @GetMapping(RESOURCE_URL + "/periodsForInitiate")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Collection<RequisitionPeriodDto> getProcessingPeriodIds(
      @RequestParam(value = "programId") UUID programId,
      @RequestParam(value = "facilityId") UUID facilityId,
      @RequestParam(value = "emergency") boolean emergency) {
    Profiler profiler = getProfiler("GET_PERIODS_FOR_INITIATE_REQUISITION", programId,
        facilityId, emergency);

    if (null == facilityId || null == programId) {
      throw new ValidationMessageException(
          new Message(MessageKeys.ERROR_REQUISITION_PERIODS_FOR_INITIATE_MISSING_PARAMETERS));
    }

    checkPermission(
        profiler,
        () -> permissionService.canInitOrAuthorizeRequisition(programId, facilityId)
    );

    profiler.start("CHECK_IF_FACILITY_SUPPORTS_PROGRAM");
    facilitySupportsProgramHelper.checkIfFacilitySupportsProgram(facilityId, programId);

    profiler.start("GET_PERIODS");
    Collection<RequisitionPeriodDto> periods = periodService.getPeriods(
        programId, facilityId, emergency
    );

    stopProfiler(profiler, periods);

    return periods;
  }

  /**
   * Submits earlier initiated requisition.
   */
  @PostMapping(RESOURCE_URL + "/{id}/submit")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public BasicRequisitionDto submitRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {

    Profiler profiler = getProfiler("SUBMIT_REQUISITION", requisitionId);

    Requisition requisition = findRequisition(requisitionId, profiler);
    checkPermission(profiler, () -> permissionService.canSubmitRequisition(requisition));

    validateIdempotencyKey(request, profiler);

    Map<VersionIdentityDto, OrderableDto> orderables = findOrderables(
        profiler, () -> getLineItemOrderableIdentities(requisition)
    );

    Map<VersionIdentityDto, ApprovedProductDto> approvedProducts = findApprovedProducts(
        () -> getLineItemApprovedProductIdentities(requisition), profiler);

    validateForStatusChange(requisition, orderables, approvedProducts, profiler);

    ProcessingPeriodDto period = periodService.getPeriod(requisition.getProcessingPeriodId());
    checkIfPeriodIsValid(requisition, period, profiler);

    logger.debug("Submitting a requisition with id " + requisition.getId());

    ProgramDto program = findProgram(requisition.getProgramId(), profiler);

    profiler.start("SUBMIT");
    requisition.submit(orderables, getCurrentUser(profiler).getId(),
        program.getSkipAuthorization());

    profiler.start("SAVE");
    requisitionService.saveStatusMessage(requisition, authenticationHelper.getCurrentUser());
    requisitionRepository.save(requisition);

    callStatusChangeProcessor(profiler, requisition);
    logger.debug("Requisition with id " + requisition.getId() + " submitted");

    BasicRequisitionDto dto = buildBasicDto(profiler, requisition);

    addLocationHeader(request, response, dto.getId(), profiler);

    stopProfiler(profiler, dto);

    return dto;
  }

  /**
   * Deletes requisition with the given id.
   */
  @DeleteMapping(RESOURCE_URL + "/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteRequisition(@PathVariable("id") UUID requisitionId) {
    Profiler profiler = getProfiler("DELETE_REQUISITION", requisitionId);
    Requisition requisition = findRequisition(requisitionId, profiler);
    checkPermission(profiler, () -> permissionService.canDeleteRequisition(requisition));

    profiler.start("DELETE");
    requisitionService.delete(requisition);

    stopProfiler(profiler);
  }

  /**
   * Allows updating requisitions.
   *
   * @param requisitionDto A requisitionDto bound to the request body.
   * @param requisitionId UUID of requisition which we want to update.
   * @return updated requisition.
   */
  @PutMapping(RESOURCE_URL + "/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RequisitionDto updateRequisition(@RequestBody RequisitionDto requisitionDto,
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {
    Profiler profiler = getProfiler("UPDATE_REQUISITION", requisitionId, requisitionDto);

    UpdatePreparationResult result = doUpdatePreparation(requisitionId, requisitionDto,
        request, profiler);

    Requisition requisitionToUpdate = result.getRequisitionToUpdate();

    logger.debug("Updating requisition with id: {}", requisitionId);

    FacilityDto facility = findFacility(requisitionToUpdate.getFacilityId(), profiler);

    UpdateParams params = new UpdateParams(requisitionToUpdate, result.getRequisition(),
        result.getOrderables(), facility, result.getProgram(), result.getPeriod(),
        result.getApprovedProducts());

    ETagResource<RequisitionDto> etaggedResource = doUpdate(params, profiler);

    stopProfiler(profiler, etaggedResource.getResource());

    response.setHeader(HttpHeaders.ETAG, etaggedResource.getEtag());
    return etaggedResource.getResource();
  }

  /**
   * Get chosen requisition.
   *
   * @param requisitionId UUID of requisition whose we want to get
   * @return Requisition.
   */
  @GetMapping(RESOURCE_URL + "/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public RequisitionDto getRequisition(@PathVariable("id") UUID requisitionId,
      HttpServletResponse response) {
    Profiler profiler = getProfiler("GET_REQUISITION", requisitionId);
    Requisition requisition = findRequisition(requisitionId, profiler);
    checkPermission(profiler, () -> permissionService.canViewRequisition(requisition));
    RequisitionDto requisitionDto = buildDto(
        profiler, requisition,
        findOrderables(profiler, requisition::getAllOrderables),
        findApprovedProducts(requisition::getAllApprovedProductIdentities, profiler),
        findFacility(requisition.getFacilityId(), profiler),
        findProgram(requisition.getProgramId(), profiler),
        null
    );

    stopProfiler(profiler, requisitionDto);

    response.setHeader(HttpHeaders.ETAG, ETagResource.buildWeakETag(requisition.getVersion()));
    return requisitionDto;
  }

  /**
   * Finds requisitions matching all of the provided parameters.
   */
  @GetMapping(RESOURCE_URL + "/search")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<BasicRequisitionDto> searchRequisitions(
      @RequestParam MultiValueMap<String, String> queryParams,
      Pageable pageable) {

    RequisitionSearchParams params = new QueryRequisitionSearchParams(queryParams);

    Profiler profiler = getProfiler("REQUISITIONS_SEARCH", params);

    profiler.start("REQUISITION_SERVICE_SEARCH");
    Page<Requisition> requisitionPage = requisitionService.searchRequisitions(params, pageable);

    profiler.start("REQUISITION_DTO_BUILD");
    Page<BasicRequisitionDto> requisitionDtoPage = Pagination.getPage(
        basicRequisitionDtoBuilder.build(requisitionPage.getContent()),
        pageable,
        requisitionPage.getTotalElements());

    stopProfiler(profiler, requisitionDtoPage);
    return requisitionDtoPage;
  }

  /**
   * Skipping chosen requisition period.
   */
  @PutMapping(RESOURCE_URL + "/{id}/skip")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public BasicRequisitionDto skipRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {

    Profiler profiler = getProfiler("SKIP_REQUISITION", requisitionId);

    Requisition requisition = findRequisition(requisitionId, profiler);

    checkPermission(profiler, () -> permissionService.canUpdateRequisition(requisition));

    validateIdempotencyKey(request, profiler);

    ProgramDto program = findProgram(requisition.getProgramId(), profiler);
    UserDto user = getCurrentUser(profiler);

    requisition.skip(program.getPeriodsSkippable(), user.getId());
    Requisition skippedRequisition = requisitionRepository.save(requisition);

    callStatusChangeProcessor(profiler, skippedRequisition);

    BasicRequisitionDto dto = buildBasicDto(profiler, skippedRequisition);

    addLocationHeader(request, response, dto.getId(), profiler);

    stopProfiler(profiler, dto);
    return dto;
  }

  /**
   * Rejecting requisition which is waiting for approve.
   */
  @PutMapping(RESOURCE_URL + "/{id}/reject")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public BasicRequisitionDto rejectRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {

    Profiler profiler = getProfiler("REJECT", requisitionId);

    Requisition requisition = findRequisition(requisitionId, profiler);
    checkPermission(profiler, () -> permissionService.canApproveRequisition(requisition));

    validateIdempotencyKey(request, profiler);

    Map<VersionIdentityDto, OrderableDto> orderables = findOrderables(
        profiler, () -> getLineItemOrderableIdentities(requisition)
    );

    profiler.start("REJECT");
    Requisition rejectedRequisition = requisitionService.reject(requisition, orderables);

    callStatusChangeProcessor(profiler, rejectedRequisition);

    profiler.start("NOTIFY_STATUS_CHANGED");
    requisitionStatusNotifier
        .notifyStatusChanged(rejectedRequisition, LocaleContextHolder.getLocale());

    BasicRequisitionDto dto = buildBasicDto(profiler, rejectedRequisition);

    addLocationHeader(request, response, dto.getId(), profiler);

    stopProfiler(profiler, dto);
    return dto;
  }

  /**
   * Approve specified by id requisition.
   */
  @PostMapping(RESOURCE_URL + "/{id}/approve")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public BasicRequisitionDto approveRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {

    Profiler profiler = getProfiler("APPROVE_REQUISITION", requisitionId);

    Requisition requisition = findRequisition(requisitionId, profiler);
    UserDto user = getCurrentUser(profiler);

    checkPermission(profiler,
        () -> requisitionService.validateCanApproveRequisition(requisition, user.getId()));

    validateIdempotencyKey(request, profiler);

    Map<VersionIdentityDto, OrderableDto> orderables = findOrderables(profiler, requisition);

    Map<VersionIdentityDto, ApprovedProductDto> approvedProducts = findApprovedProducts(
        () -> getLineItemApprovedProductIdentities(requisition), profiler);

    validateForStatusChange(requisition, orderables, approvedProducts, profiler);

    SupervisoryNodeDto supervisoryNodeDto = getSupervisoryNodeDto(profiler, requisition);
    ProcessingPeriodDto period = periodService.getPeriod(requisition.getProcessingPeriodId());
    List<SupplyLineDto> supplyLines = period.isReportOnly()
        ? Collections.emptyList()
        : getSupplyLineDtos(profiler, requisition);

    profiler.start("DO_APPROVE");
    ApproveParams approveParams = new ApproveParams(user, supervisoryNodeDto, orderables,
        supplyLines, period);
    doApprove(requisition, approveParams);

    BasicRequisitionDto requisitionDto = buildBasicDto(profiler, requisition);

    if (!requisition.getTemplate().isPopulateStockOnHandFromStockCards()) {
      submitStockEvent(requisition, user.getId(), orderables);
    }

    addLocationHeader(request, response, requisitionDto.getId(), profiler);

    stopProfiler(profiler, requisitionDto);
    return requisitionDto;
  }

  /**
   * Get requisitions to approve for right supervisor.
   *
   * @return Approved requisitions.
   */
  @GetMapping(RESOURCE_URL + "/requisitionsForApproval")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<BasicRequisitionDto> requisitionsForApproval(
      @RequestParam(value = "program", required = false) UUID programId,
      Pageable pageable) {
    Profiler profiler = getProfiler("REQUISITIONS_FOR_APPROVAL", programId, pageable);
    UserDto user = getCurrentUser(profiler);

    profiler.start("REQUISITION_SERVICE_GET_FOR_APPROVAL");
    Page<Requisition> approvalRequisitions = requisitionService
        .getRequisitionsForApproval(user, programId, pageable);

    profiler.start(BUILD_DTO_LIST);
    Page<BasicRequisitionDto> dtoPage = Pagination.getPage(
        basicRequisitionDtoBuilder.build(approvalRequisitions.getContent()),
        pageable,
        approvalRequisitions.getTotalElements());

    stopProfiler(profiler);
    return dtoPage;
  }

  /**
   * Get all submitted Requisitions.
   *
   * @return Submitted requisitions.
   */
  @GetMapping(RESOURCE_URL + "/submitted")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<RequisitionDto> getSubmittedRequisitions(Pageable pageable) {
    Profiler profiler = getProfiler("GET_SUBMITTED_REQUISITIONS", pageable);

    profiler.start("SEARCH_REQUISITIONS");
    RequisitionSearchParams params = new DefaultRequisitionSearchParams(
        null, null, null, null, null, null, null, null, null,
        EnumSet.of(RequisitionStatus.SUBMITTED));

    Page<Requisition> submittedRequisitions = requisitionService
        .searchRequisitions(params, pageable);

    profiler.start(BUILD_DTO_LIST);
    Page<RequisitionDto> page = Pagination.getPage(
        requisitionDtoBuilder.build(submittedRequisitions.getContent()),
        pageable,
        submittedRequisitions.getTotalElements());

    stopProfiler(profiler);
    return page;
  }

  /**
   * Authorize given requisition.
   *
   * @param requisitionId UUID of Requisition to authorize.
   * @return authorized Requisition.
   */
  @PostMapping(RESOURCE_URL + "/{id}/authorize")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public BasicRequisitionDto authorizeRequisition(
      @PathVariable("id") UUID requisitionId,
      HttpServletRequest request,
      HttpServletResponse response) {

    Profiler profiler = getProfiler("AUTHORIZE_REQUISITION", requisitionId);

    Requisition requisition = findRequisition(requisitionId, profiler);
    checkPermission(profiler, () -> permissionService.canAuthorizeRequisition(requisition));

    validateIdempotencyKey(request, profiler);

    Map<VersionIdentityDto, OrderableDto> orderables = findOrderables(
        profiler, () -> getLineItemOrderableIdentities(requisition)
    );

    Map<VersionIdentityDto, ApprovedProductDto> approvedProducts = findApprovedProducts(
        () -> getLineItemApprovedProductIdentities(requisition), profiler);

    validateForStatusChange(requisition, orderables, approvedProducts, profiler);

    ProcessingPeriodDto period = periodService.getPeriod(requisition.getProcessingPeriodId());
    checkIfPeriodIsValid(requisition, period, profiler);

    UserDto user = getCurrentUser(profiler);

    profiler.start("AUTHORIZE");
    requisition.authorize(orderables, user.getId());

    profiler.start("SAVE");
    requisitionService.saveStatusMessage(requisition, user);
    requisitionRepository.save(requisition);

    callStatusChangeProcessor(profiler, requisition);
    logger.debug("Requisition: " + requisitionId + " authorized.");

    BasicRequisitionDto dto = buildBasicDto(profiler, requisition);

    addLocationHeader(request, response, dto.getId(), profiler);

    stopProfiler(profiler, dto);
    return dto;
  }

  /**
   * Get approved requisitions matching all of provided parameters.
   *
   * @param programId UUID of the program to be used as filter
   * @param facilityId UUID of the facility to be used as filter
   * @param pageable   Pageable object that allows client to optionally add "page" (page number)
   *                   and "size" (page size) query parameters to the request.
   * @return Page of approved requisitions.
   */
  @GetMapping(RESOURCE_URL + "/requisitionsForConvert")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public Page<RequisitionWithSupplyingDepotsDto> listForConvertToOrder(
      @RequestParam(required = false) UUID programId,
      @RequestParam(required = false) UUID facilityId,
      @SortDefault.SortDefaults({
          @SortDefault(sort = {"emergency"}, direction = Direction.DESC),
          @SortDefault(sort = {"programId"}, direction = Direction.ASC)
      }) Pageable pageable) {
    Profiler profiler = getProfiler(
        "GET_REQUISITIONS_FOR_CONVERT",
        programId, facilityId, pageable
    );

    profiler.start("SEARCH_FOR_APPROVED_REQUISITIONS");
    Page<RequisitionWithSupplyingDepotsDto> page = requisitionService.searchApprovedRequisitions(
        facilityId, programId, pageable);

    stopProfiler(profiler, page);
    return page;
  }

  /**
   * Converting Requisition list to orders.
   *
   * @param list List of Requisitions with their supplyingDepots that will be converted to Orders
   */
  @PostMapping(RESOURCE_URL + "/convertToOrder")
  @ResponseStatus(HttpStatus.CREATED)
  public void convertToOrder(@RequestBody List<ReleasableRequisitionDto> list) {
    Profiler profiler = getProfiler("CONVERT_TO_ORDER", list);
    checkPermission(profiler, () -> permissionService.canConvertToOrder(list));

    profiler.start("CONVERT");
    requisitionService.convertToOrder(list, getCurrentUser(profiler));

    stopProfiler(profiler);
  }

  private SupervisoryNodeDto getSupervisoryNodeDto(Profiler profiler, Requisition requisition) {
    profiler.start("GET_SUPERVISORY_NODE");
    return supervisoryNodeService
        .findOne(requisition.getSupervisoryNodeId());
  }

  private Map<VersionIdentityDto, OrderableDto> findOrderables(Profiler profiler,
      Requisition requisition) {
    return findOrderables(profiler, () -> getLineItemOrderableIdentities(requisition));
  }

  private List<SupplyLineDto> getSupplyLineDtos(Profiler profiler, Requisition requisition) {
    profiler.start("GET_SUPPLY_LINE");
    return supplyLineReferenceDataService.search(
        requisition.getProgramId(), requisition.getSupervisoryNodeId());
  }

}
