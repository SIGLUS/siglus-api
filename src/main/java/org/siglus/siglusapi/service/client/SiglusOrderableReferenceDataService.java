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

import static org.siglus.siglusapi.constant.CacheConstants.CACHE_KEY_GENERATOR;
import static org.siglus.siglusapi.constant.CacheConstants.SIGLUS_ORDERABLES;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.openlmis.referencedata.dto.OrderableDto;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.dto.QueryOrderableSearchParams;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SiglusOrderableReferenceDataService extends BaseReferenceDataService<OrderableDto> {

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

  public List<OrderableDto> findByIds(Collection<UUID> ids) {
    return CollectionUtils.isEmpty(ids)
        ? Collections.emptyList()
        : getPage(RequestParameters.init().set(FieldConstants.ID, ids)).getContent();
  }

  @Cacheable(value = SIGLUS_ORDERABLES, keyGenerator = CACHE_KEY_GENERATOR)
  public Page<OrderableDto> searchOrderables(QueryOrderableSearchParams searchParams, Pageable pageable) {
    RequestParameters parameters = RequestParameters.init()
        .set(FieldConstants.CODE, searchParams.getCode())
        .set(FieldConstants.NAME, searchParams.getName())
        .set(FieldConstants.PROGRAM, searchParams.getProgramCode())
        .set(FieldConstants.ID, searchParams.getIds())
        .setPage(pageable);
    return getPage(parameters);
  }

  public OrderableDto create(OrderableDto orderableDto) {
    return put("", orderableDto, getResultClass(), false);
  }

  public OrderableDto update(OrderableDto orderableDto) {
    return put(orderableDto.getId().toString(), orderableDto, getResultClass(), false);
  }
}
