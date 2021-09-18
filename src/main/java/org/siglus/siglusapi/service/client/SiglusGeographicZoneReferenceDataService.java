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
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.siglusapi.constant.PaginationConstants;
import org.siglus.siglusapi.dto.GeographicZoneDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SiglusGeographicZoneReferenceDataService extends
    BaseReferenceDataService<GeographicZoneDto> {
  @Override
  protected String getUrl() {
    return "/api/geographicZones/";
  }

  @Override
  protected Class<GeographicZoneDto> getResultClass() {
    return GeographicZoneDto.class;
  }

  @Override
  protected Class<GeographicZoneDto[]> getArrayResultClass() {
    return GeographicZoneDto[].class;
  }

  public List<GeographicZoneDto> searchAllGeographicZones() {
    Pageable noPagination = new PageRequest(PaginationConstants.DEFAULT_PAGE_NUMBER,
        PaginationConstants.NO_PAGINATION);
    return getPage(RequestParameters.init().setPage(noPagination)).getContent();
  }

  public void updateGeographicZone(GeographicZoneDto dto) {
    put(dto.getId().toString(), dto, Void.class, false);
  }

  public void createGeographicZone(GeographicZoneDto dto) {
    postResult("", dto, Void.class);
  }


}
