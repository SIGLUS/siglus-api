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

package org.openlmis.notification.web.notification;

import static java.util.Arrays.asList;
import static org.openlmis.notification.i18n.MessageKeys.ERROR_NOTIFICATION_SEARCH_INVALID_PARAMS;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openlmis.notification.repository.custom.NotificationRepositoryCustom;
import org.openlmis.notification.web.SearchParams;
import org.openlmis.notification.web.ValidationException;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@ToString
public final class NotificationSearchParams implements NotificationRepositoryCustom.SearchParams {
  public static final String USER_ID = "userId";
  public static final String SENDING_DATE_FROM = "sendingDateFrom";
  public static final String SENDING_DATE_TO = "sendingDateTo";

  private static final List<String> ALL_PARAMETERS =
      asList(USER_ID, SENDING_DATE_FROM, SENDING_DATE_TO);

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object.
   */
  public NotificationSearchParams(MultiValueMap<String, String> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Gets {@link UUID} for "userId" key from params.
   *
   * @return UUID value of userId or null if params doesn't contain "userId" key.
   */
  public UUID getUserId() {
    if (!queryParams.containsKey(USER_ID)) {
      return null;
    }
    return queryParams.getUuid(USER_ID);
  }

  /**
   * Gets {@link ZonedDateTime} for "sendingDateFrom" key from params.
   *
   * @return ZonedDateTime value of sendingDateFrom or null if params doesn't contain
   *         "sendingDateFrom" key.
   */
  public ZonedDateTime getSendingDateFrom() {
    if (!queryParams.containsKey(SENDING_DATE_FROM)) {
      return null;
    }
    return queryParams.getZonedDateTime(SENDING_DATE_FROM);
  }

  /**
   * Gets {@link ZonedDateTime} for "sendingDateTo" key from params.
   *
   * @return ZonedDateTime value of sendingDateTo or null if params doesn't contain
   *         "sendingDateTo" key.
   */
  public ZonedDateTime getSendingDateTo() {
    if (!queryParams.containsKey(SENDING_DATE_TO)) {
      return null;
    }
    return queryParams.getZonedDateTime(SENDING_DATE_TO);
  }

  /**
   * Checks if query params are valid. Throws exception if any provided param is not on supported
   * list.
   */
  public void validate() {
    boolean onlyValidParameters = queryParams
        .keySet()
        .stream()
        .allMatch(ALL_PARAMETERS::contains);

    if (!onlyValidParameters) {
      throw new ValidationException(ERROR_NOTIFICATION_SEARCH_INVALID_PARAMS);
    }
  }
}
