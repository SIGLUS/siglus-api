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

package org.siglus.siglusapi.constant;

import static org.siglus.common.domain.referencedata.Code.code;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.siglus.common.domain.referencedata.Code;
import org.siglus.siglusapi.dto.android.request.AndroidHeader;

public class AndroidConstants {

  private AndroidConstants() {}

  public static final String USER_NAME = "UserName";

  public static final String FACILITY_CODE = "FacilityCode";

  public static final String FACILITY_NAME = "FacilityName";

  public static final String UNIQUE_ID = "UniqueId";

  public static final String DEVICE_INFO = "DeviceInfo";

  public static final String VERSION_CODE = "VersionCode";

  public static final String ANDROID_SDK_VERSION = "AndroidSDKVersion";

  public static final Code SCHEDULE_CODE = code("Android-M1");

  // please change the format in master data if there is any update on this formatter
  public static final DateTimeFormatter PERIOD_NAME_FORMATTER = DateTimeFormatter
      .ofPattern("MMM dd-yyyy", Locale.ENGLISH);

  public static AndroidHeader getAndroidHeader(HttpServletRequest request) {
    return AndroidHeader.builder()
        .username(request.getHeader(USER_NAME))
        .facilityCode(request.getHeader(FACILITY_CODE))
        .facilityName(request.getHeader(FACILITY_NAME))
        .uniqueId(request.getHeader(UNIQUE_ID))
        .deviceInfo(request.getHeader(DEVICE_INFO))
        .versionCode(request.getHeader(VERSION_CODE))
        .androidSdkVersion(request.getHeader(ANDROID_SDK_VERSION))
            .build();
  }
}
