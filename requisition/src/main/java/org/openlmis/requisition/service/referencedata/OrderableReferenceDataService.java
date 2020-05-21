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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openlmis.requisition.domain.requisition.VersionEntityReference;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.VersionIdentityDto;
import org.openlmis.requisition.service.RequestParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class OrderableReferenceDataService
        extends BaseReferenceDataService<OrderableDto> {

  @Override
  protected String getUrl() {
    return "/api/orderables/";
  }

  @Override
  protected Class<OrderableDto> getResultClass() {
    return OrderableDto.class;
  }

  @Override
  protected Class<OrderableDto[]> getArrayResultClass() {
    return OrderableDto[].class;
  }

  @Override
  public List<OrderableDto> findAll() {
    return getPage(RequestParameters.init()).getContent();
  }

  /**
   * Finds orderables by their identities.
   */
  public List<OrderableDto> findByIdentities(Set<VersionEntityReference> references) {
    if (CollectionUtils.isEmpty(references)) {
      return Collections.emptyList();
    }

    List<VersionIdentityDto> identities = references
        .stream()
        .map(ref -> new VersionIdentityDto(ref.getId(), ref.getVersionNumber()))
        .collect(Collectors.toList());

    OrderableSearchParams payload = new OrderableSearchParams(
        null, null, null, identities, 0, identities.size());

    return getPage("/search", RequestParameters.init(), payload).getContent();
  }
}
