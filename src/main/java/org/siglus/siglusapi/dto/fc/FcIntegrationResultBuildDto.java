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

  public FcIntegrationResultBuildDto(String api, List<? extends ResponseBaseDto> result, String startDate, ZonedDateTime previousLastUpdatedAt, boolean finalSuccess, int createCounter, int updateCounter, String errorMessage) {
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
