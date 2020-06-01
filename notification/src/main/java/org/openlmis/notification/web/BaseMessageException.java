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

package org.openlmis.notification.web;

import org.openlmis.notification.i18n.Message;

/**
 * Base class for building {@link RuntimeException} with {@link Message}.  This should be
 * extended and the constructor's used.  This class should not be caught nor should it be handled
 * through any Spring handler.  Only sub-classes should have handlers.
 */
public abstract class BaseMessageException extends RuntimeException {
  private final Message message;

  /**
   * Create new exception with the given message key.  Helper method that
   * uses {@link #BaseMessageException(Message)}.
   * @param messageKey the messageKey of a {@link Message}.
   */
  BaseMessageException(String messageKey) {
    this(new Message(messageKey));
  }

  /**
   * Create a new exception with the given message.
   * @param message the message.
   */
  private BaseMessageException(Message message) {
    this(message, null);
  }

  /**
   * Create a new exception with the given message and cause.
   * @param message the message.
   * @param cause the cause of this exception.
   */
  private BaseMessageException(Message message, Throwable cause) {
    super(message.toString(), cause);
    this.message = message;
  }

  /**
   * Get the {@link Message} contained in this exception.
   * @return the message
   */
  public final Message asMessage() {
    return message;
  }

  /**
   * Overrides {@link RuntimeException#getMessage()}.
   *
   * @return a localized string description
   */
  @Override
  public String getMessage() {
    return this.message.toString();
  }
}
