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

package org.siglus.siglusapi.dto.fc;

import static java.util.Comparator.comparing;
import static org.siglus.siglusapi.constant.FcConstants.PRODUCT_API;

import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FcIntegrationResultDto {

  private String api;

  private String startDate;

  private ZonedDateTime lastUpdatedAt;

  private Integer totalObjects;

  private Integer createdObjects;

  private Integer updatedObjects;

  private Boolean finalSuccess;

  private String errorMessage;

  public static FcIntegrationResultDto buildResult(List<? extends ResponseBaseDto> result, String startDate,
      ZonedDateTime previousLastUpdatedAt, boolean finalSuccess, int createCounter, int updateCounter) {
    ZonedDateTime lastUpdatedAt;
    if (result.isEmpty() || !finalSuccess) {
      lastUpdatedAt = previousLastUpdatedAt;
    } else {
      lastUpdatedAt = result.stream()
          .max(comparing(ResponseBaseDto::getLastUpdatedAt))
          .orElseThrow(EntityNotFoundException::new)
          .getLastUpdatedAt();
    }
    return FcIntegrationResultDto.builder()
        .api(PRODUCT_API)
        .startDate(startDate)
        .lastUpdatedAt(lastUpdatedAt)
        .totalObjects(result.size())
        .createdObjects(createCounter)
        .updatedObjects(updateCounter)
        .finalSuccess(finalSuccess)
        .build();
  }

}
