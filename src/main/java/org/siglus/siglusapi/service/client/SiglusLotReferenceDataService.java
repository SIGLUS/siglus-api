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
import org.siglus.common.dto.referencedata.LotDto;
import org.siglus.common.dto.referencedata.LotSearchParams;
import org.siglus.common.service.client.BaseReferenceDataService;
import org.siglus.common.util.RequestParameters;
import org.siglus.siglusapi.constant.FieldConstants;
import org.springframework.stereotype.Service;

@Service
public class SiglusLotReferenceDataService extends BaseReferenceDataService<LotDto> {

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
    RequestParameters requestParameters = RequestParameters.init()
        .set(FieldConstants.EXPIRATION_DATE, lotSearchParams.getExpirationDate())
        .set(FieldConstants.TRADE_ITEM_ID, lotSearchParams.getTradeItemId())
        .set(FieldConstants.LOT_CODE, lotSearchParams.getLotCode())
        .set(FieldConstants.ID, lotSearchParams.getId());
    return getPage("/", requestParameters).getContent();
  }

  public LotDto saveLot(LotDto lotDto) {
    return postResult("/", lotDto, getResultClass());
  }

}
