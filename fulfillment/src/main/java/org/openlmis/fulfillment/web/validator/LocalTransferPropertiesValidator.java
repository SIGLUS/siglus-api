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

package org.openlmis.fulfillment.web.validator;

import com.google.common.collect.Lists;
import java.util.List;
import org.openlmis.fulfillment.domain.LocalTransferProperties;
import org.openlmis.fulfillment.util.Message;
import org.springframework.stereotype.Component;

@Component
public class LocalTransferPropertiesValidator extends BaseValidator {

  private static final String PATH = "path";

  /**
   * Valides the given local transfer properties.
   *
   * @param target instance of {@link LocalTransferProperties} that should be validated.
   */
  public List<Message.LocalizedMessage> validate(LocalTransferProperties target) {
    List<Message.LocalizedMessage> errors = Lists.newArrayList();

    rejectIfBlank(errors, target.getPath(), PATH);

    return errors;
  }

}
