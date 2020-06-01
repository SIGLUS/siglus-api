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

package org.openlmis.notification.testutils;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.openlmis.notification.web.notification.NotificationSearchParams;
import org.springframework.util.LinkedMultiValueMap;

public class NotificationSearchParamsDataBuilder {

  private LinkedMultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();

  public NotificationSearchParamsDataBuilder withUserId(UUID userId) {
    queryMap.set(NotificationSearchParams.USER_ID, userId.toString());
    return this;
  }

  public NotificationSearchParamsDataBuilder withSendingDateFrom(ZonedDateTime sendingDateFrom) {
    queryMap.add(NotificationSearchParams.SENDING_DATE_FROM, sendingDateFrom.toString());
    return this;
  }

  public NotificationSearchParamsDataBuilder withSendingDateTo(ZonedDateTime sendingDateTo) {
    queryMap.add(NotificationSearchParams.SENDING_DATE_TO, sendingDateTo.toString());
    return this;
  }

  public NotificationSearchParamsDataBuilder withInvalidParam() {
    queryMap.add("some-param", "some-value");
    return this;
  }

  public NotificationSearchParams build() {
    return new NotificationSearchParams(queryMap);
  }

}
