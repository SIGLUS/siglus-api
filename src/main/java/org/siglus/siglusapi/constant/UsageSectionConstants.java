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

public class UsageSectionConstants {

  private UsageSectionConstants() { }

  public static class ConsultationNumberLineItems {
    private ConsultationNumberLineItems() { }

    public static final String GROUP_NAME = "number";
    public static final String COLUMN_NAME = "consultationNumber";
  }

  public static class KitUsageLineItems {
    private KitUsageLineItems() { }

    public static final String COLLECTION_KIT_OPENED = "kitOpened";
    public static final String COLLECTION_KIT_RECEIVED = "kitReceived";
    public static final String SERVICE_CHW = "CHW";
    public static final String SERVICE_HF = "HF";
  }

  public static class RegimenLineItems {
    private RegimenLineItems() { }

    public static final String COLUMN_NAME_PATIENT = "patients";
    public static final String COLUMN_NAME_COMMUNITY = "community";
  }

  public static class PatientLineItems {
    private PatientLineItems() { }

    public static final String TABLE_DISPENSED = "table_dispensed";
    public static final String TABLE_DISPENSED_KEY = "table_dispensed_key";

    public static final String NEW_COLUMN = "new";
    public static final String NEW_COLUMN_0 = "newColumn0";
    public static final String NEW_COLUMN_1 = "newColumn1";
    public static final String NEW_COLUMN_2 = "newColumn2";
    public static final String NEW_COLUMN_3 = "newColumn3";
    public static final String NEW_COLUMN_4 = "newColumn4";
    public static final String NEW_COLUMN_5 = "newColumn5";

    public static final String PATIENT_TYPE = "patientType";
    public static final String NEW_SECTION_0 = "newSection0";
    public static final String NEW_SECTION_1 = "newSection1";
    public static final String NEW_SECTION_2 = "newSection2";
    public static final String NEW_SECTION_3 = "newSection3";
    public static final String NEW_SECTION_4 = "newSection4";

    public static final String TOTAL = "total";

  }

}
