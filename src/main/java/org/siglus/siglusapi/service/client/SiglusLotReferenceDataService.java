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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.codehaus.jackson.map.ObjectMapper;
import org.openlmis.referencedata.web.LotController;
import org.openlmis.stockmanagement.util.RequestParameters;
import org.siglus.siglusapi.constant.FieldConstants;
import org.siglus.siglusapi.dto.LotDto;
import org.siglus.siglusapi.dto.LotSearchParams;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

@Service
@RequiredArgsConstructor
public class SiglusLotReferenceDataService extends BaseReferenceDataService<LotDto> {

  private final LotController lotController;

  @Override
  protected String getUrl() {
    return "/api/lots";
  }

  @Override
  protected Class<LotDto> getResultClass() {
    return LotDto.class;
  }

  @Override
  protected Class<LotDto[]> getArrayResultClass() {
    return LotDto[].class;
  }

  @Override
  public List<LotDto> findAll() {
    return getPage(RequestParameters.init()).getContent();
  }

  public List<LotDto> findAllLot(RequestParameters parameters) {
    return getPage("", parameters).getContent();
  }

  public List<LotDto> getLots(LotSearchParams lotSearchParams) {
    org.openlmis.referencedata.service.LotSearchParams requestParams =
        new org.openlmis.referencedata.service.LotSearchParams(lotSearchParams.getExpirationDate(),
            lotSearchParams.getTradeItemId(),
            lotSearchParams.getLotCode(),
            lotSearchParams.getId());
    Pageable pageable = new PageRequest(0, Integer.MAX_VALUE);
    return lotController.getLots(requestParams, pageable).getContent().stream().map(LotDto::from)
        .collect(Collectors.toList());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public LotDto saveLot(LotDto lotDto) {
    ObjectMapper objectMapper = new ObjectMapper();

    BindingResult bindingResult = new MapBindingResult(objectMapper.convertValue(lotDto, Map.class), "lotDto");

    final org.openlmis.referencedata.dto.LotDto lotDtoRequest = new org.openlmis.referencedata.dto.LotDto();
    BeanUtils.copyProperties(lotDto, lotDtoRequest);

    org.openlmis.referencedata.dto.LotDto lotDtoResponse = lotController.createLot(lotDtoRequest, bindingResult);
    LotDto result = new LotDto();
    BeanUtils.copyProperties(lotDtoResponse, result);

    return result;
  }

  public void batchSaveLot(List<LotDto> lotDtos) {
    if (org.apache.commons.collections.CollectionUtils.isEmpty(lotDtos)) {
      return;
    }
    final List<org.openlmis.referencedata.dto.LotDto> lotDtoRequests = new ArrayList<>();
    lotDtos.forEach(lotDto -> {
      final org.openlmis.referencedata.dto.LotDto lotDtoRequestItem = new org.openlmis.referencedata.dto.LotDto();
      BeanUtils.copyProperties(lotDto, lotDtoRequestItem);

      lotDtoRequests.add(lotDtoRequestItem);
    });
    lotController.batchCreateNewLot(lotDtoRequests);
  }

  public List<LotDto> findByIds(Collection<UUID> ids) {
    return CollectionUtils.isEmpty(ids)
        ? Collections.emptyList()
        : getPage(RequestParameters.init().set(FieldConstants.ID, ids)).getContent();
  }

}
