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

    private RegimenLineItems() {
    }

    public static final String COLUMN_NAME_PATIENT = "patients";
    public static final String COLUMN_NAME_COMMUNITY = "community";
  }

  public static class PatientLineItems {

    private PatientLineItems() {
    }

    public static final String TABLE_DISPENSED = "table_dispensed";
    public static final String TABLE_DISPENSED_KEY = "table_dispensed_key";

    public static final String TOTAL_COLUMN = "total";
    public static final String NEW_COLUMN = "new";
    public static final String NEW_COLUMN_0 = "newColumn0";
    public static final String NEW_COLUMN_1 = "newColumn1";
    public static final String NEW_COLUMN_2 = "newColumn2";
    public static final String NEW_COLUMN_3 = "newColumn3";
    public static final String NEW_COLUMN_4 = "newColumn4";

    public static final String STLINHAS = "1stLinhas";

    public static final String PATIENT_TYPE = "patientType";
    public static final String NEW_SECTION_0 = "newSection0";
    public static final String NEW_SECTION_1 = "newSection1";
    public static final String NEW_SECTION_2 = "newSection2";
    public static final String NEW_SECTION_3 = "newSection3";
    public static final String NEW_SECTION_4 = "newSection4";
    public static final String NEW_SECTION_5 = "newSection5";
    public static final String NEW_SECTION_6 = "newSection6";
    public static final String NEW_SECTION_7 = "newSection7";

    public static final String TABLE_ARVT_KEY = "table_arvt_key";
    public static final String TABLE_PATIENTS_KEY = "table_patients_key";
    public static final String TABLE_PROPHYLAXY_KEY = "table_prophylaxy_key";
    public static final String TABLE_DISPENSED_DS_KEY = "table_dispensed_ds_key";
    public static final String TABLE_DISPENSED_DT_KEY = "table_dispensed_dt_key";
    public static final String TABLE_DISPENSED_DM_KEY = "table_dispensed_dm_key";

    public static final String TABLE_TRAV_LABEL_NEW_KEY = "table_trav_label_new_key";
    public static final String TABLE_TRAV_LABEL_MAINTENANCE_KEY = "table_trav_label_maintenance_key";
    public static final String TABLE_TRAV_LABEL_TRANSIT_KEY = "table_trav_label_transit_key";
    public static final String TABLE_TRAV_LABEL_TRANSFERS_KEY = "table_trav_label_transfers_key";
    public static final String TABLE_TRAV_LABEL_ALTERATION_KEY = "table_trav_label_alteration_key";

    public static final String TABLE_PATIENTS_ADULTS_KEY = "table_patients_adults_key";
    public static final String TABLE_PATIENTS_0TO4_KEY = "table_patients_0to4_key";
    public static final String TABLE_PATIENTS_5TO9_KEY = "table_patients_5to9_key";
    public static final String TABLE_PATIENTS_10TO14_KEY = "table_patients_10to14_key";

    public static final String TABLE_PROPHYLAXIS_PPE_KEY = "table_prophylaxis_ppe_key";
    public static final String TABLE_PROPHYLAXIS_PREP_KEY = "table_prophylaxis_prep_key";
    public static final String TABLE_PROPHYLAXIS_CHILD_KEY = "table_prophylaxis_child_key";
    public static final String TABLE_PROPHYLAXIS_VALUE_KEY = "table_prophylaxis_value_key";


    public static final String DISPENSED_DS5 = "dispensed_ds5";
    public static final String DISPENSED_DS4 = "dispensed_ds4";
    public static final String DISPENSED_DS3 = "dispensed_ds3";
    public static final String DISPENSED_DS2 = "dispensed_ds2";
    public static final String DISPENSED_DS1 = "dispensed_ds1";
    public static final String DISPENSED_DS = "dispensed_ds";

    public static final String DISPENSED_DT2 = "dispensed_dt2";
    public static final String DISPENSED_DT1 = "dispensed_dt1";
    public static final String DISPENSED_DT = "dispensed_dt";

    public static final String DISPENSED_DM = "dispensed_dm";

    public static final String CONTAIN_DS = "ds";
    public static final String CONTAIN_DT = "dt";
    public static final String CONTAIN_DM = "dm";

    public static final String KEY_REGIME_3LINES_1 = "key_regime_3lines_1";
    public static final String KEY_REGIME_3LINES_2 = "key_regime_3lines_2";
    public static final String KEY_REGIME_3LINES_3 = "key_regime_3lines_3";
  }

  public static class UsageInformationLineItems {

    private UsageInformationLineItems() {
    }

    public static final String SERVICE_TOTAL = "total";
    public static final String SERVICE_CHW = "newColumn0";
    public static final String SERVICE_HF = "HF";
    public static final String EXISTENT_STOCK = "existentStock";
    public static final String TREATMENTS_ATTENDED = "treatmentsAttended";
  }

}
