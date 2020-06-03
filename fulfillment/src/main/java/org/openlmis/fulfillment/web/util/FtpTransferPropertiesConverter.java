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

import org.openlmis.fulfillment.domain.FtpTransferProperties;
import org.openlmis.fulfillment.service.ExporterBuilder;

final class FtpTransferPropertiesConverter
    implements TransferPropertiesConverter<FtpTransferProperties, FtpTransferPropertiesDto> {

  @Override
  public boolean supports(Class clazz) {
    return FtpTransferProperties.class.equals(clazz)
        || FtpTransferPropertiesDto.class.equals(clazz);
  }

  @Override
  public FtpTransferProperties toDomain(FtpTransferPropertiesDto dto) {
    return FtpTransferProperties.newInstance(dto);
  }

  @Override
  public FtpTransferPropertiesDto toDto(FtpTransferProperties domain,
                                        ExporterBuilder exporter) {
    FtpTransferPropertiesDto dto = new FtpTransferPropertiesDto();
    exporter.export(domain, dto);

    return dto;
  }

}
