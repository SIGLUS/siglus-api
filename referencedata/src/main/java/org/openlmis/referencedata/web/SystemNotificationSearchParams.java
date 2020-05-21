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

package org.openlmis.referencedata.web;

import static java.util.Arrays.asList;
import static org.openlmis.referencedata.util.messagekeys.SystemNotificationMessageKeys.ERROR_INVALID_PARAMS;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.custom.SystemNotificationRepositoryCustom;
import org.openlmis.referencedata.util.Message;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@ToString
public final class SystemNotificationSearchParams implements
    SystemNotificationRepositoryCustom.SearchParams {

  private static final String AUTHOR_ID = "authorId";
  private static final String IS_DISPLAYED = "isDisplayed";
  private static final String EXPAND = "expand";

  private static final List<String> ALL_PARAMETERS = asList(AUTHOR_ID, IS_DISPLAYED, EXPAND);

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object.
   */
  public SystemNotificationSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  /**
   * Gets {@link UUID} for authorId key from params.
   *
   * @return UUID value of author or null if params doesn't contain "authorId" key.
   */
  public UUID getAuthorId() {
    if (!queryParams.containsKey(AUTHOR_ID)) {
      return null;
    }
    return queryParams.getUuid(AUTHOR_ID);
  }

  /**
   * Gets {@link Boolean} for isDisplayed key from params.
   *
   * @return Boolean value of isDisplayed flag or null if params doesn't contain "isDisplayed" key.
   */
  public Boolean getIsDisplayed() {
    if (!queryParams.containsKey(IS_DISPLAYED)) {
      return null;
    }
    return queryParams.getBoolean(IS_DISPLAYED);
  }

  public Set<String> getExpand() {
    return queryParams.getStrings(EXPAND);
  }

  /**
   * Checks if query params are valid. Returns false if any provided param is not on supported list.
   */
  public void validate() {
    if (!ALL_PARAMETERS.containsAll(queryParams.keySet())) {
      throw new ValidationMessageException(new Message(ERROR_INVALID_PARAMS));
    }
  }
}
