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

package org.siglus.siglusapi.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Immutable value object for a message.
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class Message implements Serializable {

  private static final String MESSAGE_KEY = "messageKey";
  private static final String MESSAGE_STR = "message";

  @Include
  private final String key;
  private final Serializable[] params;

  public Message(String messageKey, Serializable... messageParameters) {
    Validate.notBlank(messageKey);
    this.key = messageKey.trim();
    this.params = messageParameters;
  }

  public static Message createFromMessageKeyStr(String messageStr) {
    try {
      if (null == messageStr || "null".equalsIgnoreCase(messageStr)) {
        return null;
      }
      HashMap<String, String> hashMap = new ObjectMapper().readValue(messageStr, HashMap.class);
      if (StringUtils.isNotEmpty(hashMap.get(MESSAGE_STR))) {
        return new Message(hashMap.get(MESSAGE_STR));
      }
      return new Message(hashMap.get(MESSAGE_KEY));
    } catch (IOException e) {
      return new Message(messageStr);
    }
  }

  @Override
  public String toString() {
    return key + ": " + StringUtils.join(params, ", ");
  }

}
