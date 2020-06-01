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

package org.openlmis.notification.service;

import lombok.Getter;
import org.openlmis.notification.i18n.Message;

@Getter
public class NotificationException extends RuntimeException {
  private final String messageKey;
  private final String[] params;

  /**
   * Creates new Notification exception with message key and params.
   *
   * @param messageKey key that is related with exception message.
   * @param params     params that will be used in the exception message.
   */
  public NotificationException(String messageKey, String... params) {
    super(messageKey);
    this.messageKey = messageKey;
    this.params = params;
  }

  /**
   * Creates new Notification exception with message key and params.
   *
   * @param cause      the cause.
   * @param messageKey key that is related with exception message.
   * @param params     params that will be used in the exception message.
   */
  public NotificationException(Throwable cause, String messageKey, String... params) {
    super(messageKey, cause);
    this.messageKey = messageKey;
    this.params = params;
  }

  public Message asMessage() {
    return new Message(messageKey, (Object[])params);
  }

  /**
   * Overrides Exception's public String getMessage().
   *
   * @return a localized string description
   */
  @Override
  public String getMessage() {
    return asMessage().toString();
  }
}
