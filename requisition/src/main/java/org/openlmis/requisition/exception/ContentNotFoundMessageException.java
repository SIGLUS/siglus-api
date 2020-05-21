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

package org.openlmis.requisition.exception;

import org.openlmis.requisition.utils.Message;

/**
 * Exception thrown when resource was not found.
 */
public class ContentNotFoundMessageException extends BaseMessageException {
  private static final long serialVersionUID = 6586834132550189267L;

  public ContentNotFoundMessageException(String message, Object... params) {
    this(new Message(message, params));
  }

  public ContentNotFoundMessageException(Message message) {
    super(message);
  }

}
