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

import java.util.List;
import org.siglus.common.dto.referencedata.OpenLmisGeographicZoneDto;
import org.siglus.common.util.RequestParameters;
import org.siglus.common.util.referencedata.Pagination;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusGeographicZoneReferenceDataService extends
    BaseReferenceDataService<OpenLmisGeographicZoneDto> {
  @Override
  protected String getUrl() {
    return "/api/geographicZones/";
  }

  @Override
  protected Class<OpenLmisGeographicZoneDto> getResultClass() {
    return OpenLmisGeographicZoneDto.class;
  }

  @Override
  protected Class<OpenLmisGeographicZoneDto[]> getArrayResultClass() {
    return OpenLmisGeographicZoneDto[].class;
  }

  public List<OpenLmisGeographicZoneDto> searchAllGeographicZones() {
    Pageable noPagination = new PageRequest(Pagination.DEFAULT_PAGE_NUMBER,
        Pagination.NO_PAGINATION);
    return getPage(RequestParameters.init().setPage(noPagination)).getContent();
  }

  public void updateGeographicZone(OpenLmisGeographicZoneDto dto) {
    put(dto.getId().toString(), dto, Void.class, false);
  }

  public void createGeographicZone(OpenLmisGeographicZoneDto dto) {
    postResult("", dto, Void.class);
  }


}
