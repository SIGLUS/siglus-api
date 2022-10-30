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

package org.siglus.siglusapi.constant.android;

import java.util.Set;
import javax.persistence.EntityNotFoundException;
import lombok.NoArgsConstructor;
import org.javers.common.collections.Sets;
import org.springframework.data.util.Pair;

@NoArgsConstructor
public class MmtbRequisitionConstants {

  @NoArgsConstructor
  // MMTB Patient data section: 6 tables
  public static class MmtbPatientSection {

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

    public static String getTableValueByKey(String tableValue) {
      return MMTB_PATIENT_TABLES.stream()
          .filter(pair -> pair.getSecond().equals(tableValue))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getFirst();
    }

    public static String getTableKeyByValue(String tableKey) {
      return MMTB_PATIENT_TABLES.stream()
          .filter(pair -> pair.getFirst().equals(tableKey))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
    }

    public static String getColumnKeyByValue(String tableValue, String columnKey) {
      Set<Pair<String, String>> columnKeyToValueSet = MMTB_PATIENT_TABLE_TO_COLUMN.stream()
          .filter(pair -> pair.getFirst().equals(tableValue))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
      return columnKeyToValueSet.stream()
          .filter(pair -> pair.getFirst().equals(columnKey))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
    }

    public static String getColumnValueByKey(String tableValue, String columnValue) {
      Set<Pair<String, String>> columnKeyToValueSet = MMTB_PATIENT_TABLE_TO_COLUMN.stream()
          .filter(pair -> pair.getFirst().equals(tableValue))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
      return columnKeyToValueSet.stream()
          .filter(pair -> pair.getSecond().equals(columnValue))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getFirst();
    }
  }

  @NoArgsConstructor
  // MMTB Age group section: 2 tables
  public static class MmtbAgeGroupSection {

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

    public static final Set<Pair<String, String>> MMTB_AGE_GROUP_TABLE_1_COLUMNS = Sets.asSet(
        Pair.of(TABLE_1_COLUMN_1_KEY, TABLE_1_COLUMN_1_VALUE),
        Pair.of(TABLE_1_COLUMN_2_KEY, TABLE_1_COLUMN_2_VALUE)
    );
    public static final Set<Pair<String, String>> MMTB_AGE_GROUP_TABLE_2_COLUMNS = Sets.asSet(
        Pair.of(TABLE_2_COLUMN_1_KEY, TABLE_2_COLUMN_1_VALUE),
        Pair.of(TABLE_2_COLUMN_2_KEY, TABLE_2_COLUMN_2_VALUE),
        Pair.of(TABLE_2_COLUMN_3_KEY, TABLE_2_COLUMN_3_VALUE)
    );
    public static final Set<Pair<String, Set<Pair<String, String>>>> MMTB_AGE_GROUP_TABLE_TO_COLUMN = Sets.asSet(
        Pair.of(TABLE_1_VALUE, MMTB_AGE_GROUP_TABLE_1_COLUMNS),
        Pair.of(TABLE_2_VALUE, MMTB_AGE_GROUP_TABLE_2_COLUMNS)
    );

    public static String getGroupValueByKey(String groupKey) {
      return MMTB_AGE_GROUP_TABLE_1_COLUMNS.stream()
          .filter(pair -> pair.getFirst().equals(groupKey))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
    }

    public static String getServiceValueByKey(String groupKey) {
      return MMTB_AGE_GROUP_TABLE_2_COLUMNS.stream()
          .filter(pair -> pair.getFirst().equals(groupKey))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
    }
  }

}
