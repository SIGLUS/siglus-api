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

package org.openlmis.referencedata.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;

/**
 * Value class of a localized message.  Useful for JSON serialization, logging, etc...
 */
public final class LocalizedMessage {

  public static final String MESSAGE_KEY_FIELD = "messageKey";
  public static final String MESSAGE_FIELD = "message";

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String messageKey;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String message;

  LocalizedMessage(String messageKey, String message) {
    Validate.notBlank(message);
    this.messageKey = messageKey;
    this.message = message;
  }

  @Override
  public String toString() {
    return messageKey + ": " + message;
  }

  public String asMessage() {
    return message;
  }
}
