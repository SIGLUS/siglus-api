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

package org.openlmis.referencedata.util.messagekeys;

public abstract class OrderableMessageKeys extends MessageKeys {
  private static final String ERROR = join(SERVICE_ERROR, ORDERABLE);
  private static final String REQUIRED = "required";
  private static final String VALUE = "value";
  private static final String NOT_SUPPORTED = "notSupported";

  public static final String ERROR_NOT_FOUND = join(ERROR, NOT_FOUND);
  public static final String ERROR_NULL = join(ERROR, NULL);
  public static final String ERROR_ID_MISMATCH = join(ERROR, ID_MISMATCH);
  public static final String ERROR_PRODUCT_CODE_REQUIRED = join(ERROR, "productCode", REQUIRED);
  public static final String ERROR_PACK_ROUNDING_THRESHOLD_REQUIRED =
      join(ERROR, "packRoundingThreshold", REQUIRED);
  public static final String ERROR_ROUND_TO_ZERO_REQUIRED =
      join(ERROR, "roundToZero", REQUIRED);
  public static final String ERROR_NET_CONTENT_REQUIRED =
      join(ERROR, "netContent", REQUIRED);
  public static final String ERROR_DISPENSABLE_REQUIRED = join(ERROR, "dispensable", REQUIRED);
  public static final String ERROR_INVALID_PARAMS = join(ERROR, SEARCH, INVALID_PARAMS);
  public static final String ERROR_DUPLICATED = join(ERROR, DUPLICATED);
  public static final String ERROR_PROGRAMS_DUPLICATED =
      join(ERROR, "programOrderable", DUPLICATED);

  public static final String ERROR_INVALID_VERSION_IDENTITY =
      join(ERROR, SEARCH, "invalidVersionIdentity");
  public static final String ERROR_MINIMUM_TEMPERATURE =
          join(ERROR, "minimumTemperature");
  public static final String ERROR_MAXIMUM_TEMPERATURE =
          join(ERROR, "maximumTemperature");
  public static final String ERROR_MINIMUM_TEMPERATURE_UNIT_CODE =
          join(ERROR_MINIMUM_TEMPERATURE, "temperatureMeasurementUnitCode");
  public static final String ERROR_MAXIMUM_TEMPERATURE_UNIT_CODE =
          join(ERROR_MAXIMUM_TEMPERATURE, "temperatureMeasurementUnitCode");
  public static final String ERROR_MINIMUM_TEMPERATURE_UNIT_CODE_NOT_SUPPORTED =
          join(ERROR_MINIMUM_TEMPERATURE_UNIT_CODE, NOT_SUPPORTED);
  public static final String ERROR_MAXIMUM_TEMPERATURE_UNIT_CODE_NOT_SUPPORTED =
          join(ERROR_MAXIMUM_TEMPERATURE_UNIT_CODE, NOT_SUPPORTED);
  public static final String ERROR_MINIMUM_TEMPERATURE_UNIT_CODE_REQUIRED =
          join(ERROR_MINIMUM_TEMPERATURE_UNIT_CODE, REQUIRED);
  public static final String ERROR_MAXIMUM_TEMPERATURE_UNIT_CODE_REQUIRED =
          join(ERROR_MAXIMUM_TEMPERATURE_UNIT_CODE, REQUIRED);
  public static final String ERROR_MINIMUM_TEMPERATURE_VALUE =
          join(ERROR_MINIMUM_TEMPERATURE, VALUE);
  public static final String ERROR_MAXIMUM_TEMPERATURE_VALUE =
          join(ERROR_MAXIMUM_TEMPERATURE, VALUE);
  public static final String ERROR_MINIMUM_TEMPERATURE_VALUE_REQUIRED =
          join(ERROR_MINIMUM_TEMPERATURE_VALUE, REQUIRED);
  public static final String ERROR_MAXIMUM_TEMPERATURE_VALUE_REQUIRED =
          join(ERROR_MAXIMUM_TEMPERATURE_VALUE, REQUIRED);
  public static final String ERROR_IN_BOX_CUBE_DIMENSION =
          join(ERROR, "inBoxCubeDimension");
  public static final String ERROR_IN_BOX_CUBE_DIMENSION_UNIT_CODE =
          join(ERROR_IN_BOX_CUBE_DIMENSION, "measurementUnitCode");
  public static final String ERROR_IN_BOX_CUBE_DIMENSION_UNIT_CODE_NOT_SUPPORTED =
          join(ERROR_IN_BOX_CUBE_DIMENSION_UNIT_CODE, NOT_SUPPORTED);
  public static final String ERROR_IN_BOX_CUBE_DIMENSION_UNIT_CODE_REQUIRED =
          join(ERROR_IN_BOX_CUBE_DIMENSION_UNIT_CODE, REQUIRED);
  public static final String ERROR_IN_BOX_CUBE_DIMENSION_VALUE =
          join(ERROR_IN_BOX_CUBE_DIMENSION, VALUE);
  public static final String ERROR_IN_BOX_CUBE_DIMENSION_VALUE_REQUIRED =
          join(ERROR_IN_BOX_CUBE_DIMENSION_VALUE, REQUIRED);
}
