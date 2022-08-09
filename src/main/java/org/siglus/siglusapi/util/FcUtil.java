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

package org.siglus.siglusapi.util;

import static org.siglus.siglusapi.constant.FcConstants.STATUS_ACTIVE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.siglus.siglusapi.domain.FcIntegrationChanges;

public class FcUtil {

  public static final String PATTERN = "^[A-z0-9]*$";
  public static final String CREATE = "create";
  public static final String UPDATE = "update";

  private FcUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static boolean isActive(String status) {
    return status.equalsIgnoreCase(STATUS_ACTIVE);
  }

  public static boolean isNotMatchedCode(String code) {
    Pattern p = Pattern.compile(PATTERN);
    Matcher m = p.matcher(code);
    return !m.matches();
  }

  public static FcIntegrationChanges buildCreateFcIntegrationChanges(String apiCategory, String code,
      String fcContent) {
    return FcIntegrationChanges.builder()
        .type(CREATE)
        .category(apiCategory)
        .code(code)
        .fcContent(fcContent)
        .build();
  }

  public static FcIntegrationChanges buildUpdateFcIntegrationChanges(String apiCategory, String code, String fcContent,
      String originContent) {
    return FcIntegrationChanges.builder()
        .type(UPDATE)
        .category(apiCategory)
        .code(code)
        .fcContent(fcContent)
        .originContent(originContent)
        .build();
  }

  public static FcIntegrationChanges buildUpdateFcIntegrationChanges(String apiCategory, String code, String fcContent,
      String originContent, boolean isUpdateProgram) {
    return FcIntegrationChanges.builder()
        .type(UPDATE)
        .category(apiCategory)
        .code(code)
        .fcContent(fcContent)
        .originContent(originContent)
        .updateProgram(isUpdateProgram)
        .build();
  }
}
