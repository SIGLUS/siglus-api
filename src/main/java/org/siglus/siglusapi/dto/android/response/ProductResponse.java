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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import org.siglus.common.dto.referencedata.OrderableDto;
import org.siglus.common.dto.referencedata.ProgramOrderableDto;

@Data
public class ProductResponse {

  private String productCode;
  private Boolean active;
  private Boolean archived;
  private String fullProductName;
  private String description;
  private Long netContent;
  private Long packRoundingThreshold;
  private Boolean roundToZero;
  private String category;
  private String programCode;
  private Boolean isKit;
  private List<ProductChildResponse> children = Collections.emptyList();
  private Boolean isBasic;
  private Boolean isNos;
  private Boolean isHiv;
  private Instant lastUpdated;

  public static ProductResponse fromOrderable(OrderableDto orderable,
      Map<UUID, OrderableDto> productMap) {
    ProductResponse resp = new ProductResponse();
    resp.setProductCode(orderable.getProductCode());
    resp.setFullProductName(orderable.getFullProductName());
    resp.setDescription(orderable.getDescription());
    // TODO should ask Momand how set this
    resp.setActive(true);
    resp.setArchived(orderable.getArchived());
    resp.setNetContent(orderable.getNetContent());
    resp.setPackRoundingThreshold(orderable.getPackRoundingThreshold());
    resp.setRoundToZero(orderable.getRoundToZero());
    ProgramOrderableDto program = orderable.getPrograms().stream().findFirst()
        .orElseThrow(IllegalStateException::new);
    resp.setProgramCode((String) orderable.getExtraData().get("programCode"));
    resp.setCategory(program.getOrderableCategoryDisplayName());
    resp.setChildren(orderable.getChildren().stream()
        .map(child -> ProductChildResponse.fromOrderableChild(child, productMap))
        .collect(Collectors.toList()));
    resp.setIsKit(!orderable.getChildren().isEmpty());
    resp.setIsBasic(parseKey(orderable, "isBasic"));
    resp.setIsHiv(parseKey(orderable, "isHiv"));
    resp.setIsNos(parseKey(orderable, "isNos"));
    resp.setLastUpdated(orderable.getMeta().getLastUpdated().toInstant());
    return resp;
  }

  public static Boolean parseKey(OrderableDto orderable, String key) {
    Object value = orderable.getExtraData().get(key);
    return Optional.ofNullable(value)
        .map(Object::toString)
        .map(Boolean::parseBoolean)
        .orElse(false);
  }

}
