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

import static java.util.stream.Collectors.toList;
import static org.siglus.common.constant.ExtraDataConstants.CLIENT_SUBMITTED_TIME;
import static org.siglus.siglusapi.constant.PaginationConstants.UNPAGED;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.dto.BasicRequisitionDto;
import org.openlmis.requisition.dto.RequisitionV2Dto;
import org.openlmis.requisition.repository.custom.RequisitionSearchParams;
import org.openlmis.requisition.web.QueryRequisitionSearchParams;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class SiglusRequisitionRequisitionService extends BaseRequisitionService<RequisitionV2Dto> {

  @Override
  protected String getUrl() {
    return "/api/";
  }

  @Override
  protected Class<RequisitionV2Dto> getResultClass() {
    return RequisitionV2Dto.class;
  }

  @Override
  protected Class<RequisitionV2Dto[]> getArrayResultClass() {
    return RequisitionV2Dto[].class;
  }

  public Page<BasicRequisitionDto> searchRequisitions(RequisitionSearchParams params,
      Pageable pageable) {
    RequestParameters queryParams = RequestParameters.init()
        .set(QueryRequisitionSearchParams.FACILITY, params.getFacility())
        .set(QueryRequisitionSearchParams.PROGRAM, params.getProgram())
        .set(QueryRequisitionSearchParams.INITIATED_DATE_FROM, params.getInitiatedDateFrom())
        .set(QueryRequisitionSearchParams.INITIATED_DATE_TO, params.getInitiatedDateTo())
        .set(QueryRequisitionSearchParams.PROCESSING_PERIOD, params.getProcessingPeriod())
        .set(QueryRequisitionSearchParams.SUPERVISORY_NODE, params.getSupervisoryNode())
        .set(QueryRequisitionSearchParams.REQUISITION_STATUS, params.getRequisitionStatuses())
        .set(QueryRequisitionSearchParams.EMERGENCY, params.getEmergency())
        .set(QueryRequisitionSearchParams.MODIFIED_DATE_FROM, params.getModifiedDateFrom())
        .set(QueryRequisitionSearchParams.MODIFIED_DATE_TO, params.getModifiedDateTo())
        .setPage(pageable);
    return getPage("requisitions/search", queryParams, null, HttpMethod.GET,
        BasicRequisitionDto.class, true);
  }

  public List<RequisitionV2Dto> getPreviousEmergencyRequisition(Requisition requisition) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.set(QueryRequisitionSearchParams.EMERGENCY, Boolean.TRUE.toString());
    queryParams.set(QueryRequisitionSearchParams.PROCESSING_PERIOD, requisition.getProcessingPeriodId().toString());
    queryParams.set(QueryRequisitionSearchParams.FACILITY, requisition.getFacilityId().toString());
    Instant clientSubmittedTime = getClientSubmittedTime(requisition.getExtraData());
    return searchRequisitions(new QueryRequisitionSearchParams(queryParams), UNPAGED)
        .getContent().stream()
        .filter(r -> {
          Instant previousClientSubmittedTime = getClientSubmittedTime(r.getExtraData());
          if (clientSubmittedTime != null && previousClientSubmittedTime != null) {
            return previousClientSubmittedTime.isBefore(clientSubmittedTime);
          }
          return r.getCreatedDate().isBefore(requisition.getCreatedDate());
        })
        .map(r -> searchRequisition(r.getId()))
        .collect(toList());
  }

  public RequisitionV2Dto searchRequisition(UUID id) {
    return findOne("v2/requisitions/" + id.toString(), RequestParameters.init());
  }

  public void deleteRequisition(UUID id) {
    delete("requisitions/" + id.toString(), Boolean.TRUE);
  }

  private Instant getClientSubmittedTime(Map<String, Object> extraData) {
    Object clientSubmittedTime = extraData.get(CLIENT_SUBMITTED_TIME);
    if (clientSubmittedTime == null) {
      return null;
    }
    return Instant.parse(String.valueOf(clientSubmittedTime));
  }

}
