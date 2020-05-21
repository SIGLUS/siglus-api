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

import static org.openlmis.referencedata.util.messagekeys.SupplyPartnerMessageKeys.ERROR_INVALID_PARAMS;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openlmis.referencedata.exception.ValidationMessageException;
import org.openlmis.referencedata.repository.custom.SupplyPartnerRepositoryCustom;
import org.openlmis.referencedata.util.Message;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@ToString
final class SupplyPartnerSearchParams implements SupplyPartnerRepositoryCustom.SearchParams {

  private static final String ID = "id";
  private static final String SUPERVISORY_NODE_ID = "supervisoryNodeId";

  private static final List<String> ALL_PARAMETERS = Lists.newArrayList(ID, SUPERVISORY_NODE_ID);

  private SearchParams queryParams;

  /**
   * Wraps map of query params into an object.
   */
  SupplyPartnerSearchParams(MultiValueMap<String, Object> queryMap) {
    queryParams = new SearchParams(queryMap);
    validate();
  }

  public Set<UUID> getIds() {
    return queryParams.getUuids(ID);
  }

  public Set<UUID> getSupervisoryNodeIds() {
    return queryParams.getUuids(SUPERVISORY_NODE_ID);
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
