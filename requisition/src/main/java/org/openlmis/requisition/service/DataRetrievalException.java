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

package org.openlmis.requisition.service;

import lombok.Getter;
import org.openlmis.requisition.exception.BaseMessageException;
import org.openlmis.requisition.utils.Message;
import org.springframework.http.HttpStatus;

/**
 * Signals we were unable to retrieve reference data
 * due to a communication error.
 */
@Getter
public class DataRetrievalException extends BaseMessageException {
  private final String resource;
  private final HttpStatus status;
  private final String response;

  /**
   * Constructs the exception.
   *
   * @param resource the resource that we were trying to retrieve
   * @param status   the http status that was returned
   * @param response the response from service
   */
  public DataRetrievalException(Message message, String resource,
                                HttpStatus status, String response) {
    super(message);
    this.resource = resource;
    this.status = status;
    this.response = response;
  }
  
}
