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

package org.siglus.siglusapi.exception;

import lombok.Getter;
import org.siglus.siglusapi.dto.Message;

public class InvalidProgramCodeException extends BaseMessageException {

  @Getter
  private final String programCode;
  private final boolean isAndroidException;

  private InvalidProgramCodeException(String programCode, boolean isAndroidException) {
    super(new Message("siglusapi.error.android.sync.invalid.programCode", programCode));
    this.programCode = programCode;
    this.isAndroidException = isAndroidException;
  }

  public static InvalidProgramCodeException asNormalException(String programCode) {
    return new InvalidProgramCodeException(programCode, false);
  }

  public static InvalidProgramCodeException asAndroidException(String programCode) {
    return new InvalidProgramCodeException(programCode, true);
  }

  @Override
  public boolean isAndroidException() {
    return isAndroidException;
  }

}
