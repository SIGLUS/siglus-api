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

public abstract class ProgramOrderableBuilderDeserializerMessageKeys extends MessageKeys {
  private static final String PROGRAM_ORDERABLE_BUILDER_DESERIALIZER =
      "programOrderableBuilderDeserializer";
  private static final String NOT_INJECTED = "notInjected";
  private static final String ERROR = join(SERVICE_ERROR, PROGRAM_ORDERABLE_BUILDER_DESERIALIZER);

  public static final String ERROR_PROGRAM_REPOSITORY_NULL = join(ERROR, PROGRAM_REPOSITORY, NULL);
  public static final String ERROR_DEFAULT_SERIALIZER_NULL = join(ERROR, "defaultSerializer", NULL);
  public static final String ERROR_ORDERABLE_DISPLAY_CATEGORY_REPOSITORY_NULL =
      join(ERROR, ORDERABLE_DISPLAY_CATEGORY_REPOSITORY, NULL);
  public static final String ERROR_PROGRAM_REPOSITORY_NOT_INJECTED =
      join(ERROR, PROGRAM_REPOSITORY, NOT_INJECTED);
}
