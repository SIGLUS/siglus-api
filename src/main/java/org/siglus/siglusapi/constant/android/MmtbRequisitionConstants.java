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
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class MmtbRequisitionConstants {

  @NoArgsConstructor
  public static class MmtbPatientSection {

    // table 1: Fases de Tratamento (Adulto)
    private static final Pair<String, String> PAIR_TABLE_1 = Pair.of("table_treatment_adult_key", "newSection3");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_1 = Pair.of(
        "table_treatment_adult_key_sensitive_intensive", "new");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_2 = Pair.of(
        "table_treatment_adult_key_sensitive_maintenance", "newColumn6");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_3 = Pair.of(
        "table_treatment_adult_key_mr_induction", "newColumn0");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_4 = Pair.of(
        "table_treatment_adult_key_mr_intensive", "newColumn1");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_5 = Pair.of(
        "table_treatment_adult_key_mr_maintenance", "newColumn2");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_6 = Pair.of(
        "table_treatment_adult_key_xr_induction", "newColumn3");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_7 = Pair.of(
        "table_treatment_adult_key_xr_maintenance", "newColumn4");
    // table 2: Fases de Tratamento (Pediatrico)
    private static final Pair<String, String> PAIR_TABLE_2 = Pair.of("table_treatment_pediatric_key", "newSection4");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_1 = Pair.of(
        "table_treatment_pediatric_key_sensitive_intensive", "new");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_2 = Pair.of(
        "table_treatment_pediatric_key_sensitive_maintenance", "newColumn6");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_3 = Pair.of(
        "table_treatment_pediatric_key_mr_induction", "newColumn0");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_4 = Pair.of(
        "table_treatment_pediatric_key_mr_intensive", "newColumn1");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_5 = Pair.of(
        "table_treatment_pediatric_key_mr_maintenance", "newColumn2");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_6 = Pair.of(
        "table_treatment_pediatric_key_xr_maintenance", "newColumn3");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_7 = Pair.of(
        "table_treatment_pediatric_key_xr_intensive", "newColumn4");
    // table 3: PUs e Farmácia Ambulatório
    private static final Pair<String, String> PAIR_TABLE_3 = Pair.of("table_pharmacy_product_key", "newSection2");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_1 = Pair.of(
        "table_pharmacy_product_key_isoniazida_100_mg", "new");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_2 = Pair.of(
        "table_pharmacy_product_key_isoniazida_300_mg", "newColumn0");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_3 = Pair.of(
        "table_pharmacy_product_key_levofloxacina_100_mg", "newColumn1");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_4 = Pair.of(
        "table_pharmacy_product_key_levofloxacina_250_mg", "newColumn2");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_5 = Pair.of(
        "table_pharmacy_product_key_rifapentina_300_mg_isoniazida_300_mg", "newColumn3");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_6 = Pair.of(
        "table_pharmacy_product_key_rifapentina_150_mg", "newColumn4");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_7 = Pair.of(
        "table_pharmacy_product_key_piridoxina_25_mg", "newColumn5");
    private static final Pair<String, String> PAIR_TABLE_3_COLUMN_8 = Pair.of(
        "table_pharmacy_product_key_piridoxina_50_mg", "newColumn6");
    // table 4: Pacientes Novos no Sector da TB
    private static final Pair<String, String> PAIR_TABLE_4 = Pair.of("table_new_patients_key", "patientType");
    private static final Pair<String, String> PAIR_TABLE_4_COLUMN_1 = Pair.of(
        "table_new_patients_key_new_adult_sensitive", "new");
    private static final Pair<String, String> PAIR_TABLE_4_COLUMN_2 = Pair.of(
        "table_new_patients_key_new_adult_mr", "newColumn0");
    private static final Pair<String, String> PAIR_TABLE_4_COLUMN_3 = Pair.of(
        "table_new_patients_key_new_adult_xr", "newColumn1");
    private static final Pair<String, String> PAIR_TABLE_4_COLUMN_4 = Pair.of(
        "table_new_patients_key_new_child_sensitive", "newColumn2");
    private static final Pair<String, String> PAIR_TABLE_4_COLUMN_5 = Pair.of(
        "table_new_patients_key_new_child_mr", "newColumn3");
    private static final Pair<String, String> PAIR_TABLE_4_COLUMN_6 = Pair.of(
        "table_new_patients_key_new_child_xr", "newColumn4");
    private static final Pair<String, String> PAIR_TABLE_4_COLUMN_7 = Pair.of(
        "table_new_patients_key_total", "total");
    // table 5: Seguimento Profilaxias (Pus e Farmácia Pública)
    private static final Pair<String, String> PAIR_TABLE_5 = Pair.of("table_prophylaxis_key", "newSection0");
    private static final Pair<String, String> PAIR_TABLE_5_COLUMN_1 = Pair.of(
        "table_prophylaxis_key_initial", "new");
    private static final Pair<String, String> PAIR_TABLE_5_COLUMN_2 = Pair.of(
        "table_prophylaxis_key_continuous_maintenance", "newColumn0");
    private static final Pair<String, String> PAIR_TABLE_5_COLUMN_3 = Pair.of(
        "table_prophylaxis_key_final_last_dismissal", "newColumn1");
    private static final Pair<String, String> PAIR_TABLE_5_COLUMN_4 = Pair.of(
        "table_prophylaxis_key_total", "total");
    // table 6: Tipo de Dispensa dos Profilacticos
    private static final Pair<String, String> PAIR_TABLE_6 = Pair.of("table_prophylactics_key", "newSection1");
    private static final Pair<String, String> PAIR_TABLE_6_COLUMN_1 = Pair.of(
        "table_prophylactics_key_monthly", "new");
    private static final Pair<String, String> PAIR_TABLE_6_COLUMN_2 = Pair.of(
        "table_prophylactics_key_trimenstral", "newColumn0");
    private static final Pair<String, String> PAIR_TABLE_6_COLUMN_3 = Pair.of(
        "table_prophylactics_key_total", "total");
    private static final Set<Pair<String, String>> MMTB_PATIENT_TABLES = Sets.asSet(
        PAIR_TABLE_1, PAIR_TABLE_2, PAIR_TABLE_3, PAIR_TABLE_4, PAIR_TABLE_5, PAIR_TABLE_6
    );
    private static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_1_COLUMNS = Sets.asSet(
        PAIR_TABLE_1_COLUMN_1, PAIR_TABLE_1_COLUMN_2, PAIR_TABLE_1_COLUMN_3, PAIR_TABLE_1_COLUMN_4,
        PAIR_TABLE_1_COLUMN_5, PAIR_TABLE_1_COLUMN_6, PAIR_TABLE_1_COLUMN_7
    );
    private static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_2_COLUMNS = Sets.asSet(
        PAIR_TABLE_2_COLUMN_1, PAIR_TABLE_2_COLUMN_2, PAIR_TABLE_2_COLUMN_3, PAIR_TABLE_2_COLUMN_4,
        PAIR_TABLE_2_COLUMN_5, PAIR_TABLE_2_COLUMN_6, PAIR_TABLE_2_COLUMN_7
    );
    private static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_3_COLUMNS = Sets.asSet(
        PAIR_TABLE_3_COLUMN_1, PAIR_TABLE_3_COLUMN_2, PAIR_TABLE_3_COLUMN_3, PAIR_TABLE_3_COLUMN_4,
        PAIR_TABLE_3_COLUMN_5, PAIR_TABLE_3_COLUMN_6, PAIR_TABLE_3_COLUMN_7, PAIR_TABLE_3_COLUMN_8
    );
    private static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_4_COLUMNS = Sets.asSet(
        PAIR_TABLE_4_COLUMN_1, PAIR_TABLE_4_COLUMN_2, PAIR_TABLE_4_COLUMN_3, PAIR_TABLE_4_COLUMN_4,
        PAIR_TABLE_4_COLUMN_5, PAIR_TABLE_4_COLUMN_6, PAIR_TABLE_4_COLUMN_7
    );
    private static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_5_COLUMNS = Sets.asSet(
        PAIR_TABLE_5_COLUMN_1, PAIR_TABLE_5_COLUMN_2, PAIR_TABLE_5_COLUMN_3, PAIR_TABLE_5_COLUMN_4
    );
    private static final Set<Pair<String, String>> MMTB_PATIENT_TABLE_6_COLUMNS = Sets.asSet(
        PAIR_TABLE_6_COLUMN_1, PAIR_TABLE_6_COLUMN_2, PAIR_TABLE_6_COLUMN_3
    );
    private static final Set<Pair<String, Set<Pair<String, String>>>> MMTB_PATIENT_TABLE_TO_COLUMN = Sets.asSet(
        Pair.of(PAIR_TABLE_1.getSecond(), MMTB_PATIENT_TABLE_1_COLUMNS),
        Pair.of(PAIR_TABLE_2.getSecond(), MMTB_PATIENT_TABLE_2_COLUMNS),
        Pair.of(PAIR_TABLE_3.getSecond(), MMTB_PATIENT_TABLE_3_COLUMNS),
        Pair.of(PAIR_TABLE_4.getSecond(), MMTB_PATIENT_TABLE_4_COLUMNS),
        Pair.of(PAIR_TABLE_5.getSecond(), MMTB_PATIENT_TABLE_5_COLUMNS),
        Pair.of(PAIR_TABLE_6.getSecond(), MMTB_PATIENT_TABLE_6_COLUMNS)
    );

    public static String getTableKeyByValue(String tableValue) {
      return MMTB_PATIENT_TABLES.stream()
          .filter(pair -> pair.getSecond().equals(tableValue))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getFirst();
    }

    public static String getTableValueByKey(String tableKey) {
      return MMTB_PATIENT_TABLES.stream()
          .filter(pair -> pair.getFirst().equals(tableKey))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
    }

    public static String getColumnKeyByValue(String tableValue, String columnValue) {
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

    public static String getColumnValueByKey(String tableValue, String columnKey) {
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
  }

  @NoArgsConstructor
  public static class MmtbAgeGroupSection {

    // table 1: Faixas Etarias
    private static final Pair<String, String> PAIR_TABLE_1 = Pair.of("table_age_group_header_key", "group");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_1 = Pair.of(
        "table_age_group_header_key_treatment", "treatment");
    private static final Pair<String, String> PAIR_TABLE_1_COLUMN_2 = Pair.of(
        "table_age_group_header_key_prophylaxis", "prophylaxis");
    // table 2: Service
    private static final Pair<String, String> PAIR_TABLE_2 = Pair.of("table_age_group_service_key", "service");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_1 = Pair.of(
        "table_age_group_service_key_adult", "adultos");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_2 = Pair.of(
        "table_age_group_service_key_child_less_than_25kg", "criança < 25Kg");
    private static final Pair<String, String> PAIR_TABLE_2_COLUMN_3 = Pair.of(
        "table_age_group_service_key_child_more_than_25kg", "criança > 25Kg");
    private static final Set<Pair<String, String>> MMTB_AGE_GROUP_TABLE_1_COLUMNS = Sets.asSet(
        PAIR_TABLE_1_COLUMN_1, PAIR_TABLE_1_COLUMN_2
    );
    private static final Set<Pair<String, String>> MMTB_AGE_GROUP_TABLE_2_COLUMNS = Sets.asSet(
        PAIR_TABLE_2_COLUMN_1, PAIR_TABLE_2_COLUMN_2, PAIR_TABLE_2_COLUMN_3
    );

    public static String getGroupKeyByValue(String groupValue) {
      return MMTB_AGE_GROUP_TABLE_1_COLUMNS.stream()
          .filter(pair -> pair.getSecond().equals(groupValue))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getFirst();
    }

    public static String getGroupValueByKey(String groupKey) {
      return MMTB_AGE_GROUP_TABLE_1_COLUMNS.stream()
          .filter(pair -> pair.getFirst().equals(groupKey))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getSecond();
    }

    public static String getServiceKeyByValue(String serviceValue) {
      return MMTB_AGE_GROUP_TABLE_2_COLUMNS.stream()
          .filter(pair -> pair.getSecond().equals(serviceValue))
          .findFirst()
          .orElseThrow(EntityNotFoundException::new)
          .getFirst();
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
