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

import java.util.Set;
import lombok.NoArgsConstructor;
import org.javers.common.collections.Sets;
import org.springframework.data.util.Pair;

@NoArgsConstructor
public class UsageSectionConstants {

  @NoArgsConstructor
  public static class ConsultationNumberLineItems {
    public static final String GROUP_NAME = "number";
    public static final String COLUMN_NAME = "consultationNumber";
  }

  @NoArgsConstructor
  public static class KitUsageLineItems {
    public static final String COLLECTION_KIT_OPENED = "kitOpened";
    public static final String COLLECTION_KIT_RECEIVED = "kitReceived";
    public static final String SERVICE_CHW = "CHW";
    public static final String SERVICE_HF = "HF";
  }

  @NoArgsConstructor
  public static class RegimenLineItems {
    public static final String COLUMN_NAME_PATIENT = "patients";
    public static final String COLUMN_NAME_COMMUNITY = "community";
  }

  @NoArgsConstructor
  public static class MmiaPatientLineItems {
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
    public static final String NEW_SECTION_8 = "newSection8";

    // MMIA Patient data section: 7 tables, last 3 tables will be auto calculated
    public static final String TABLE_ARVT_KEY = "table_arvt_key"; // table 1: "patientType" section
    public static final String TABLE_PATIENTS_KEY = "table_patients_key"; // table 2: "newSection0" section
    public static final String TABLE_PROPHYLAXY_KEY = "table_prophylaxy_key"; // table 3: "newSection1" section
    public static final String TABLE_TOTAL_KEY = "table_total_key"; // table 4: "newSection8" section
    public static final String TABLE_DISPENSED_DS_KEY = "table_dispensed_ds_key"; // table 5: "newSection2" section
    public static final String TABLE_DISPENSED_DT_KEY = "table_dispensed_dt_key"; // table 6: "newSection3" section
    public static final String TABLE_DISPENSED_DM_KEY = "table_dispensed_dm_key"; // table 7: "newSection4" section

    // MMIA Patient data section table 1: Tipo de doentes em TARV
    public static final String TABLE_TRAV_LABEL_NEW_KEY = "table_trav_label_new_key";
    public static final String TABLE_TRAV_LABEL_MAINTENANCE_KEY = "table_trav_label_maintenance_key";
    public static final String TABLE_TRAV_LABEL_TRANSIT_KEY = "table_trav_label_transit_key";
    public static final String TABLE_TRAV_LABEL_TRANSFERS_KEY = "table_trav_label_transfers_key";
    public static final String TABLE_TRAV_LABEL_ALTERATION_KEY = "table_trav_label_alteration_key";

    // MMIA Patient data section table 2: Faixa Etária dos Pacientes TARV
    public static final String TABLE_PATIENTS_ADULTS_KEY = "table_patients_adults_key";
    public static final String TABLE_PATIENTS_0TO4_KEY = "table_patients_0to4_key";
    public static final String TABLE_PATIENTS_5TO9_KEY = "table_patients_5to9_key";
    public static final String TABLE_PATIENTS_10TO14_KEY = "table_patients_10to14_key";

    // MMIA Patient data section table 3: Profilaxia
    public static final String TABLE_PROPHYLAXIS_PPE_KEY = "table_prophylaxis_ppe_key";
    public static final String TABLE_PROPHYLAXIS_PREP_KEY = "table_prophylaxis_prep_key";
    public static final String TABLE_PROPHYLAXIS_CHILD_KEY = "table_prophylaxis_child_key";
    public static final String TABLE_PROPHYLAXIS_TOTAL_KEY = "table_prophylaxis_total_key";

    // MMIA Patient data section table 4: Total global
    public static final String TABLE_TOTAL_PATIENT_KEY = "table_total_patient_key";
    public static final String TABLE_TOTAL_MONTH_KEY = "table_total_month_key";

    // MMIA Patient data section table 5: Tipo de dispensa - Dispensa para 6 Meses (DS)
    public static final String DISPENSED_DS5 = "dispensed_ds5";
    public static final String DISPENSED_DS4 = "dispensed_ds4";
    public static final String DISPENSED_DS3 = "dispensed_ds3";
    public static final String DISPENSED_DS2 = "dispensed_ds2";
    public static final String DISPENSED_DS1 = "dispensed_ds1";
    public static final String DISPENSED_DS = "dispensed_ds";

    // MMIA Patient data section table 6: Tipo de dispensa - Dispensa para 3 Meses (DT)
    public static final String DISPENSED_DT2 = "dispensed_dt2";
    public static final String DISPENSED_DT1 = "dispensed_dt1";
    public static final String DISPENSED_DT = "dispensed_dt";

    // MMIA Patient data section table 7: Tipo de dispensa - Dispensa Mensal(DM)
    public static final String DISPENSED_DM = "dispensed_dm";

    public static final String CONTAIN_DS = "ds";
    public static final String CONTAIN_DT = "dt";
    public static final String CONTAIN_DM = "dm";

    public static final String KEY_REGIME_3LINES_1 = "key_regime_3lines_1";
    public static final String KEY_REGIME_3LINES_2 = "key_regime_3lines_2";
    public static final String KEY_REGIME_3LINES_3 = "key_regime_3lines_3";
  }

  @NoArgsConstructor
  public static class UsageInformationLineItems {
    public static final String SERVICE_TOTAL = "total";
    public static final String SERVICE_CHW = "newColumn0";
    public static final String SERVICE_HF = "HF";
    public static final String EXISTENT_STOCK = "existentStock";
    public static final String TREATMENTS_ATTENDED = "treatmentsAttended";
  }

  @NoArgsConstructor
  public static class TestConsumptionLineItems {
    public static final String TOTAL = "total";
    public static final String NEW_COLUMN_0 = "newColumn0";
    public static final String NEW_COLUMN_1 = "newColumn1";
    public static final String NEW_COLUMN_2 = "newColumn2";
    public static final String NEW_COLUMN_3 = "newColumn3";
    public static final String NEW_COLUMN_4 = "newColumn4";
    public static final String NEW_COLUMN_5 = "newColumn5";
    public static final String NEW_COLUMN_6 = "newColumn6";
    public static final String NEW_COLUMN_7 = "newColumn7";
    public static final String PROJECT_HIVDETERMINE = "hivDetermine";
    public static final String PROJECT_CONSUMO = "consumo";
    public static final String PROJECT_POSITIVE = "positive";
    public static final String PROJECT_UNJUSTIFIED = "unjustified";
    public static final String SERVICE_HF = "HF";
    public static final String SERVICE_APES = "APES";
  }

  @NoArgsConstructor
  // MMTB Patient data section: 6 tables
  public static class MmtbPatientLineItems {
    // table 1: Fases de Tratamento (Adulto), "newSection3"
    public static final String TABLE_1_KEY = "table_treatment_adult_key";
    public static final String TABLE_1_VALUE = "newSection3";
    public static final String TABLE_1_COLUMN_1_KEY = "table_treatment_adult_key_sensitive_intensive";
    public static final String TABLE_1_COLUMN_1_VALUE = "new";
    public static final String TABLE_1_COLUMN_2_KEY = "table_treatment_adult_key_sensitive_maintenance";
    public static final String TABLE_1_COLUMN_2_VALUE = "newColumn6";
    public static final String TABLE_1_COLUMN_3_KEY = "table_treatment_adult_key_mr_induction";
    public static final String TABLE_1_COLUMN_3_VALUE = "newColumn0";
    public static final String TABLE_1_COLUMN_4_KEY = "table_treatment_adult_key_mr_intensive";
    public static final String TABLE_1_COLUMN_4_VALUE = "newColumn1";
    public static final String TABLE_1_COLUMN_5_KEY = "table_treatment_adult_key_mr_maintenance";
    public static final String TABLE_1_COLUMN_5_VALUE = "newColumn2";
    public static final String TABLE_1_COLUMN_6_KEY = "table_treatment_adult_key_xr_induction";
    public static final String TABLE_1_COLUMN_6_VALUE = "newColumn3";
    public static final String TABLE_1_COLUMN_7_KEY = "table_treatment_adult_key_xr_maintenance";
    public static final String TABLE_1_COLUMN_7_VALUE = "newColumn4";
    // table 2: Fases de Tratamento (Pediatrico), "newSection4"
    public static final String TABLE_2_KEY = "table_treatment_pediatric_key";
    public static final String TABLE_2_VALUE = "newSection4";
    public static final String TABLE_2_COLUMN_1_KEY = "table_treatment_pediatric_key_sensitive_intensive";
    public static final String TABLE_2_COLUMN_1_VALUE = "new";
    public static final String TABLE_2_COLUMN_2_KEY = "table_treatment_pediatric_key_sensitive_maintenance";
    public static final String TABLE_2_COLUMN_2_VALUE = "newColumn6";
    public static final String TABLE_2_COLUMN_3_KEY = "table_treatment_pediatric_key_mr_induction";
    public static final String TABLE_2_COLUMN_3_VALUE = "newColumn0";
    public static final String TABLE_2_COLUMN_4_KEY = "table_treatment_pediatric_key_mr_intensive";
    public static final String TABLE_2_COLUMN_4_VALUE = "newColumn1";
    public static final String TABLE_2_COLUMN_5_KEY = "table_treatment_pediatric_key_mr_maintenance";
    public static final String TABLE_2_COLUMN_5_VALUE = "newColumn2";
    public static final String TABLE_2_COLUMN_6_KEY = "table_treatment_pediatric_key_xr_maintenance";
    public static final String TABLE_2_COLUMN_6_VALUE = "newColumn3";
    public static final String TABLE_2_COLUMN_7_KEY = "table_treatment_pediatric_key_xr_intensive";
    public static final String TABLE_2_COLUMN_7_VALUE = "newColumn4";
    // table 3: PUs e Farmácia Ambulatório, "newSection2"
    public static final String TABLE_3_KEY = "table_pharmacy_product_key";
    public static final String TABLE_3_VALUE = "newSection2";
    public static final String TABLE_3_COLUMN_1_KEY = "table_pharmacy_product_key_isoniazida_100_mg";
    public static final String TABLE_3_COLUMN_1_VALUE = "new";
    public static final String TABLE_3_COLUMN_2_KEY = "table_pharmacy_product_key_isoniazida_300_mg";
    public static final String TABLE_3_COLUMN_2_VALUE = "newColumn0";
    public static final String TABLE_3_COLUMN_3_KEY = "table_pharmacy_product_key_levofloxacina_100_mg";
    public static final String TABLE_3_COLUMN_3_VALUE = "newColumn1";
    public static final String TABLE_3_COLUMN_4_KEY = "table_pharmacy_product_key_levofloxacina_250_mg";
    public static final String TABLE_3_COLUMN_4_VALUE = "newColumn2";
    public static final String TABLE_3_COLUMN_5_KEY = "table_pharmacy_product_key_rifapentina_300_mg_isoniazida_300_mg";
    public static final String TABLE_3_COLUMN_5_VALUE = "newColumn3";
    public static final String TABLE_3_COLUMN_6_KEY = "table_pharmacy_product_key_rifapentina_150_mg";
    public static final String TABLE_3_COLUMN_6_VALUE = "newColumn4";
    public static final String TABLE_3_COLUMN_7_KEY = "table_pharmacy_product_key_piridoxina_25_mg";
    public static final String TABLE_3_COLUMN_7_VALUE = "newColumn5";
    public static final String TABLE_3_COLUMN_8_KEY = "table_pharmacy_product_key_piridoxina_50_mg";
    public static final String TABLE_3_COLUMN_8_VALUE = "newColumn6";
    // table 4: Pacientes Novos no Sector da TB, "patientType"
    public static final String TABLE_4_KEY = "table_new_patients_key";
    public static final String TABLE_4_VALUE = "patientType";
    public static final String TABLE_4_COLUMN_1_KEY = "table_new_patients_key_new_adult_sensitive";
    public static final String TABLE_4_COLUMN_1_VALUE = "new";
    public static final String TABLE_4_COLUMN_2_KEY = "table_new_patients_key_new_adult_mr";
    public static final String TABLE_4_COLUMN_2_VALUE = "newColumn0";
    public static final String TABLE_4_COLUMN_3_KEY = "table_new_patients_key_new_adult_xr";
    public static final String TABLE_4_COLUMN_3_VALUE = "newColumn1";
    public static final String TABLE_4_COLUMN_4_KEY = "table_new_patients_key_new_child_sensitive";
    public static final String TABLE_4_COLUMN_4_VALUE = "newColumn2";
    public static final String TABLE_4_COLUMN_5_KEY = "table_new_patients_key_new_child_mr";
    public static final String TABLE_4_COLUMN_5_VALUE = "newColumn3";
    public static final String TABLE_4_COLUMN_6_KEY = "table_new_patients_key_new_child_xr";
    public static final String TABLE_4_COLUMN_6_VALUE = "newColumn4";
    public static final String TABLE_4_COLUMN_7_KEY = "table_new_patients_key_total";
    public static final String TABLE_4_COLUMN_7_VALUE = "total";
    // table 5: Seguimento Profilaxias (Pus e Farmácia Pública), "newSection0"
    public static final String TABLE_5_KEY = "table_prophylaxis_key";
    public static final String TABLE_5_VALUE = "newSection0";
    public static final String TABLE_5_COLUMN_1_KEY = "table_prophylaxis_key_initial";
    public static final String TABLE_5_COLUMN_1_VALUE = "new";
    public static final String TABLE_5_COLUMN_2_KEY = "table_prophylaxis_key_continuous_maintenance";
    public static final String TABLE_5_COLUMN_2_VALUE = "newColumn0";
    public static final String TABLE_5_COLUMN_3_KEY = "table_prophylaxis_key_final_last_dismissal";
    public static final String TABLE_5_COLUMN_3_VALUE = "newColumn1";
    public static final String TABLE_5_COLUMN_4_KEY = "table_prophylaxis_key_total";
    public static final String TABLE_5_COLUMN_4_VALUE = "total";
    // table 6: Tipo de Dispensa dos Profilacticos, "newSection1"
    public static final String TABLE_6_KEY = "table_prophylactics_key";
    public static final String TABLE_6_VALUE = "newSection1";
    public static final String TABLE_6_COLUMN_1_KEY = "table_prophylactics_key_monthly";
    public static final String TABLE_6_COLUMN_1_VALUE = "new";
    public static final String TABLE_6_COLUMN_2_KEY = "table_prophylactics_key_trimenstral";
    public static final String TABLE_6_COLUMN_2_VALUE = "newColumn0";
    public static final String TABLE_6_COLUMN_3_KEY = "table_prophylactics_key_total";
    public static final String TABLE_6_COLUMN_3_VALUE = "total";

    public static final Set<Pair<String, String>> MMTB_PATIENT_TABLES = Sets.asSet(
        Pair.of(TABLE_1_KEY, TABLE_1_VALUE),
        Pair.of(TABLE_2_KEY, TABLE_2_VALUE),
        Pair.of(TABLE_3_KEY, TABLE_3_VALUE),
        Pair.of(TABLE_4_KEY, TABLE_4_VALUE),
        Pair.of(TABLE_5_KEY, TABLE_5_VALUE),
        Pair.of(TABLE_6_KEY, TABLE_6_VALUE)
    );
    public static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_1_COLUMNS = Sets.asSet(
        Pair.of(TABLE_1_COLUMN_1_KEY, TABLE_1_COLUMN_1_VALUE),
        Pair.of(TABLE_1_COLUMN_2_KEY, TABLE_1_COLUMN_2_VALUE),
        Pair.of(TABLE_1_COLUMN_3_KEY, TABLE_1_COLUMN_3_VALUE),
        Pair.of(TABLE_1_COLUMN_4_KEY, TABLE_1_COLUMN_4_VALUE),
        Pair.of(TABLE_1_COLUMN_5_KEY, TABLE_1_COLUMN_5_VALUE),
        Pair.of(TABLE_1_COLUMN_6_KEY, TABLE_1_COLUMN_6_VALUE),
        Pair.of(TABLE_1_COLUMN_7_KEY, TABLE_1_COLUMN_7_VALUE)
    );
    public static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_2_COLUMNS = Sets.asSet(
        Pair.of(TABLE_2_COLUMN_1_KEY, TABLE_2_COLUMN_1_VALUE),
        Pair.of(TABLE_2_COLUMN_2_KEY, TABLE_2_COLUMN_2_VALUE),
        Pair.of(TABLE_2_COLUMN_3_KEY, TABLE_2_COLUMN_3_VALUE),
        Pair.of(TABLE_2_COLUMN_4_KEY, TABLE_2_COLUMN_4_VALUE),
        Pair.of(TABLE_2_COLUMN_5_KEY, TABLE_2_COLUMN_5_VALUE),
        Pair.of(TABLE_2_COLUMN_6_KEY, TABLE_2_COLUMN_6_VALUE),
        Pair.of(TABLE_2_COLUMN_7_KEY, TABLE_2_COLUMN_7_VALUE)
    );
    public static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_3_COLUMNS = Sets.asSet(
        Pair.of(TABLE_3_COLUMN_1_KEY, TABLE_3_COLUMN_1_VALUE),
        Pair.of(TABLE_3_COLUMN_2_KEY, TABLE_3_COLUMN_2_VALUE),
        Pair.of(TABLE_3_COLUMN_3_KEY, TABLE_3_COLUMN_3_VALUE),
        Pair.of(TABLE_3_COLUMN_4_KEY, TABLE_3_COLUMN_4_VALUE),
        Pair.of(TABLE_3_COLUMN_5_KEY, TABLE_3_COLUMN_5_VALUE),
        Pair.of(TABLE_3_COLUMN_6_KEY, TABLE_3_COLUMN_6_VALUE),
        Pair.of(TABLE_3_COLUMN_7_KEY, TABLE_3_COLUMN_7_VALUE),
        Pair.of(TABLE_3_COLUMN_8_KEY, TABLE_3_COLUMN_8_VALUE)
    );
    public static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_4_COLUMNS = Sets.asSet(
        Pair.of(TABLE_4_COLUMN_1_KEY, TABLE_4_COLUMN_1_VALUE),
        Pair.of(TABLE_4_COLUMN_2_KEY, TABLE_4_COLUMN_2_VALUE),
        Pair.of(TABLE_4_COLUMN_3_KEY, TABLE_4_COLUMN_3_VALUE),
        Pair.of(TABLE_4_COLUMN_4_KEY, TABLE_4_COLUMN_4_VALUE),
        Pair.of(TABLE_4_COLUMN_5_KEY, TABLE_4_COLUMN_5_VALUE),
        Pair.of(TABLE_4_COLUMN_6_KEY, TABLE_4_COLUMN_6_VALUE),
        Pair.of(TABLE_4_COLUMN_7_KEY, TABLE_4_COLUMN_7_VALUE)
    );
    public static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_5_COLUMNS = Sets.asSet(
        Pair.of(TABLE_5_COLUMN_1_KEY, TABLE_5_COLUMN_1_VALUE),
        Pair.of(TABLE_5_COLUMN_2_KEY, TABLE_5_COLUMN_2_VALUE),
        Pair.of(TABLE_5_COLUMN_3_KEY, TABLE_5_COLUMN_3_VALUE),
        Pair.of(TABLE_5_COLUMN_4_KEY, TABLE_5_COLUMN_4_VALUE)
    );
    public static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_6_COLUMNS = Sets.asSet(
        Pair.of(TABLE_6_COLUMN_1_KEY, TABLE_6_COLUMN_1_VALUE),
        Pair.of(TABLE_6_COLUMN_2_KEY, TABLE_6_COLUMN_2_VALUE),
        Pair.of(TABLE_6_COLUMN_3_KEY, TABLE_6_COLUMN_3_VALUE)
    );
    public static final Set<Pair<String, Set<Pair<String, String>>>> MMTB_PATIENT_TABLE_TO_COLUMN = Sets.asSet(
        Pair.of(TABLE_1_VALUE, MMTB_PATIENT_TABLE_1_COLUMNS),
        Pair.of(TABLE_2_VALUE, MMTB_PATIENT_TABLE_2_COLUMNS),
        Pair.of(TABLE_3_VALUE, MMTB_PATIENT_TABLE_3_COLUMNS),
        Pair.of(TABLE_4_VALUE, MMTB_PATIENT_TABLE_4_COLUMNS),
        Pair.of(TABLE_5_VALUE, MMTB_PATIENT_TABLE_5_COLUMNS),
        Pair.of(TABLE_6_VALUE, MMTB_PATIENT_TABLE_6_COLUMNS)
    );
  }

  @NoArgsConstructor
  // MMTB Age group section: 2 tables
  public static class MmtbAgeGroupLineItems {
    // table 1: Faixas Etarias, "group"
    public static final String TABLE_1_KEY = "table_age_group_header_key";
    public static final String TABLE_1_VALUE = "group";
    public static final String TABLE_1_COLUMN_1_KEY = "table_age_group_header_key_treatment";
    public static final String TABLE_1_COLUMN_1_VALUE = "treatment";
    public static final String TABLE_1_COLUMN_2_KEY = "table_age_group_header_key_prophylaxis";
    public static final String TABLE_1_COLUMN_2_VALUE = "prophylaxis";
    // table 2: Service, "service"
    public static final String TABLE_2_KEY = "table_age_group_service_key";
    public static final String TABLE_2_VALUE = "service";
    public static final String TABLE_2_COLUMN_1_KEY = "table_age_group_service_key_adult";
    public static final String TABLE_2_COLUMN_1_VALUE = "adultos";
    public static final String TABLE_2_COLUMN_2_KEY = "table_age_group_service_key_child_less_than_25kg";
    public static final String TABLE_2_COLUMN_2_VALUE = "criança < 25Kg";
    public static final String TABLE_2_COLUMN_3_KEY = "table_age_group_service_key_child_more_than_25kg";
    public static final String TABLE_2_COLUMN_3_VALUE = "criança > 25Kg";
  }

}
