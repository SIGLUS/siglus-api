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
import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.domain.LocalTransferProperties;
import org.openlmis.fulfillment.util.Message;
import org.springframework.stereotype.Component;

@Component
public class FtpTransferPropertiesValidator extends BaseValidator {

  private static final String PROTOCOL = "protocol";
  private static final String USERNAME = "username";
  private static final String HOST = "serverHost";
  private static final String PORT = "serverPort";
  private static final String REMOTE_DIRECTORY = "remoteDirectory";
  private static final String LOCAL_DIRECTORY = "localDirectory";
  private static final String PASSIVE_MODE = "passiveMode";

  /**
   * Valides the given local transfer properties.
   *
   * @param target instance of {@link LocalTransferProperties} that should be validated.
   */
  public List<Message.LocalizedMessage> validate(FtpTransferProperties target) {
    List<Message.LocalizedMessage> errors = Lists.newArrayList();

    rejectIfNull(errors, target.getProtocol(), PROTOCOL);
    rejectIfBlank(errors, target.getUsername(), USERNAME);
    rejectIfBlank(errors, target.getPassword(), "password");
    rejectIfBlank(errors, target.getServerHost(), HOST);
    rejectIfLessThanZero(errors, target.getServerPort(), PORT);
    rejectIfBlank(errors, target.getRemoteDirectory(), REMOTE_DIRECTORY);
    rejectIfBlank(errors, target.getLocalDirectory(), LOCAL_DIRECTORY);
    rejectIfNull(errors, target.getPassiveMode(), PASSIVE_MODE);

    return errors;
  }

}
