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

package org.siglus.siglusapi.dto.android.response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import org.openlmis.stockmanagement.dto.referencedata.OrderableDto;

@Data
public class ProductResponse {

  private String productCode;
  private Boolean archived;
  private String fullProductName;
  private String description;
  private Long netContent;
  private Long packRoundingThreshold;
  private Boolean roundToZero;
  private String programCode;
  private Boolean isKit;
  private List<ProductChildResponse> children = Collections.emptyList();
  private Boolean isBasic;
  private Boolean isNos;
  private Boolean isHiv;
  private String lastUpdated;

  public static ProductResponse fromOrderable(OrderableDto orderable,
      Map<UUID, OrderableDto> productMap) {
    ProductResponse resp = new ProductResponse();
    resp.setProductCode(orderable.getProductCode());
    resp.setFullProductName(orderable.getFullProductName());
    resp.setDescription(orderable.getDescription());
    resp.setNetContent(orderable.getNetContent());
    resp.setPackRoundingThreshold(orderable.getPackRoundingThreshold());
    resp.setRoundToZero(orderable.getRoundToZero());
    resp.setProgramCode(orderable.getExtraData().get("programCode"));
    resp.setChildren(orderable.getChildren().stream()
        .map(child -> ProductChildResponse.fromOrderableChild(child, productMap))
        .collect(Collectors.toList()));
    resp.setIsKit(!orderable.getChildren().isEmpty());
    resp.setIsBasic(Boolean.parseBoolean(orderable.getExtraData().get("isBasic")));
    resp.setIsHiv(Boolean.parseBoolean(orderable.getExtraData().get("isHiv")));
    resp.setIsNos(Boolean.parseBoolean(orderable.getExtraData().get("isNos")));
    resp.setLastUpdated(orderable.getMeta().getLastUpdated().toInstant().toString());
    return resp;
  }
}
