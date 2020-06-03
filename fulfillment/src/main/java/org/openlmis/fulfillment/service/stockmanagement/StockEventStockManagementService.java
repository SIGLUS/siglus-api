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

package org.openlmis.fulfillment.service.stockmanagement;

import static org.openlmis.fulfillment.service.request.RequestHelper.createUri;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.openlmis.fulfillment.i18n.MessageKeys;
import org.openlmis.fulfillment.service.ExternalApiException;
import org.openlmis.fulfillment.web.ServerException;
import org.openlmis.fulfillment.web.stockmanagement.StockEventDto;
import org.openlmis.fulfillment.web.util.LocalizedMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

@Service
public class StockEventStockManagementService
    extends BaseStockManagementService<StockEventDto> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
          StockEventStockManagementService.class);

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * Saves the given stock event to the stockmanagement service.
   *
   * @param stockEventDto  the physical inventory to be submitted
   */
  @SuppressWarnings("PMD.PreserveStackTrace")
  public void submit(StockEventDto stockEventDto) {
    String url = getServiceUrl() + getUrl();

    LOGGER.debug("Sending Stock Events to Stock Management: {}", stockEventDto);

    try {
      restTemplate.exchange(
          createUri(url),
          HttpMethod.POST,
          createEntity(stockEventDto),
          UUID.class
      );

    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
        try {
          LocalizedMessageDto localizedMessage =
              objectMapper.readValue(ex.getResponseBodyAsString(), LocalizedMessageDto.class);

          throw new ExternalApiException(ex, localizedMessage);
        } catch (IOException ex2) {
          throw new ServerException(ex2, MessageKeys.ERROR_IO, ex2.getMessage());
        }
      } else {
        throw buildDataRetrievalException(ex);
      }
    }
  }

  @Override
  protected String getUrl() {
    return "/api/stockEvents";
  }

  @Override
  protected Class<StockEventDto> getResultClass() {
    return StockEventDto.class;
  }

  @Override
  protected Class<StockEventDto[]> getArrayResultClass() {
    return StockEventDto[].class;
  }
}
