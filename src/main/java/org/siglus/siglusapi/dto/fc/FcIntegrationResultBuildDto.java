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

public class FcIntegrationResultBuildDto {

  private final String api;
  private final List<? extends ResponseBaseDto> result;
  private final String startDate;
  private final ZonedDateTime previousLastUpdatedAt;
  private final boolean finalSuccess;
  private final int createCounter;
  private final int updateCounter;
  private final String errorMessage;

  public FcIntegrationResultBuildDto(String api, List<? extends ResponseBaseDto> result, String startDate,
      ZonedDateTime previousLastUpdatedAt, boolean finalSuccess, int createCounter, int updateCounter,
      String errorMessage) {
    this.api = api;
    this.result = result;
    this.startDate = startDate;
    this.previousLastUpdatedAt = previousLastUpdatedAt;
    this.finalSuccess = finalSuccess;
    this.createCounter = createCounter;
    this.updateCounter = updateCounter;
    this.errorMessage = errorMessage;
  }

  public String getApi() {
    return api;
  }

  public List<? extends ResponseBaseDto> getResult() {
    return result;
  }

  public String getStartDate() {
    return startDate;
  }

  public ZonedDateTime getPreviousLastUpdatedAt() {
    return previousLastUpdatedAt;
  }

  public boolean isFinalSuccess() {
    return finalSuccess;
  }

  public int getCreateCounter() {
    return createCounter;
  }

  public int getUpdateCounter() {
    return updateCounter;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
