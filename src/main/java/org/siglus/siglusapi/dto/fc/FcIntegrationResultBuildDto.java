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

import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.siglus.siglusapi.domain.FcIntegrationChanges;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FcIntegrationResultBuildDto {

  private String api;

  private List<? extends ResponseBaseDto> result;

  private String startDate;

  private ZonedDateTime previousLastUpdatedAt;

  private boolean finalSuccess;

  private int createCounter;

  private int updateCounter;

  private String errorMessage;

  private List<FcIntegrationChanges> fcIntegrationChanges;
}
