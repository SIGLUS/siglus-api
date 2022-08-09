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

import org.siglus.siglusapi.dto.Message;

/**
 * Base class for exceptions using Message.
 */
public abstract class BaseMessageException extends RuntimeException {

  private final Message message;

  private boolean isBusinessError = false;

  private Object businessErrorExtraData;

  protected BaseMessageException(String messageKey) {
    this(new Message(messageKey));
  }

  protected BaseMessageException(Message message) {
    this.message = message;
  }

  protected BaseMessageException(Message message, Throwable cause) {
    super(cause);
    this.message = message;
  }

  public Message asMessage() {
    return message;
  }

  @Override
  public String getMessage() {
    return this.message.toString();
  }

  public boolean isAndroidException() {
    return false;
  }

  public boolean isBusinessError() {
    return isBusinessError;
  }

  protected void setBusinessError(boolean businessError) {
    isBusinessError = businessError;
  }

  public Object getBusinessErrorExtraData() {
    return businessErrorExtraData;
  }

  protected void setBusinessErrorExtraData(Object businessErrorExtraData) {
    this.businessErrorExtraData = businessErrorExtraData;
  }
}
