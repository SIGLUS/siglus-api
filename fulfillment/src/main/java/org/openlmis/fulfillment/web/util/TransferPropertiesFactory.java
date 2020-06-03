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

package org.openlmis.fulfillment.web.util;

import com.google.common.collect.Lists;
import java.util.List;
import org.openlmis.fulfillment.domain.TransferProperties;
import org.openlmis.fulfillment.service.ExporterBuilder;

public final class TransferPropertiesFactory {
  private static final List<TransferPropertiesConverter> CONVERTERS = Lists.newArrayList(
      new FtpTransferPropertiesConverter(), new LocalTransferPropertiesConverter()
  );

  private TransferPropertiesFactory() {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new instance of {@link TransferProperties} based on data from dto.
   *
   * @param dto transfer properties.
   * @return new instance of {@link TransferProperties}.
   */
  public static TransferProperties newInstance(TransferPropertiesDto dto) {
    TransferPropertiesConverter converter = CONVERTERS
        .stream()
        .filter(c -> c.supports(dto.getClass()))
        .findFirst()
        .orElse(null);

    if (null == converter) {
      throw new IllegalArgumentException("The given dto type is not supported: " + dto.getClass());
    }

    return converter.toDomain(dto);
  }

  /**
   * Creates a new instance of {@link TransferPropertiesDto} based on data from domain class.
   *
   * @param domain an instance of {@link TransferProperties}.
   * @return new instance of {@link TransferPropertiesDto}.
   */
  public static TransferPropertiesDto newInstance(TransferProperties domain,
                                                  ExporterBuilder exporter) {
    TransferPropertiesConverter converter = CONVERTERS
        .stream()
        .filter(c -> c.supports(domain.getClass()))
        .findFirst()
        .orElse(null);

    if (null == converter) {
      throw new IllegalArgumentException(
          "The given domain type is not supported: " + domain.getClass()
      );
    }

    return converter.toDto(domain, exporter);
  }

}
