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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheConstants {

  public static final String CACHE_KEY_GENERATOR = "org.siglus.siglusapi.config.CacheKeyGenerator";

  public static final String SIGLUS_DESTINATIONS = "siglus-destinations";

  public static final String SIGLUS_SOURCES = "siglus-sources";

  public static final String SIGLUS_REASONS = "siglus-reasons";

  public static final String SIGLUS_PROGRAMS = "siglus-programs";

  public static final String SIGLUS_PROGRAM = "siglus-program";

  public static final String SIGLUS_PROGRAM_BY_CODE = "siglus-program-by-code";

  public static final String SIGLUS_APPROVED_PRODUCTS = "siglus-approved-products";

  public static final String SIGLUS_APPROVED_PRODUCTS_BY_ORDERABLES = "siglus-approved-products-by-orderables";

  public static final String SIGLUS_ORDERABLES = "siglus-orderables";

  public static final String SIGLUS_FACILITY = "siglus-facility";

  public static final String SIGLUS_USER = "siglus-user";

  public static final String SIGLUS_PROGRAM_ORDERABLES = "siglus-program-orderables";

  public static final String SIGLUS_REQUISITION_GROUPS = "siglus-requisition-groups";

  public static final String SIGLUS_KIT_ORDERABLES = "siglus-kit-orderables";
}
