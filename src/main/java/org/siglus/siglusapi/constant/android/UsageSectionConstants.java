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

import lombok.NoArgsConstructor;

@NoArgsConstructor
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class UsageSectionConstants {

  public static final String QUERY_REQUISITIONS_UNDER_HIGH_LEVEL =
      "  (select r.id from requisition.requisitions r "
          + "    left join referencedata.supervisory_nodes sn on sn.id = r.supervisorynodeid "
          + "    where sn.facilityid = :facilityId and sn.parentid is null"
          + "    and r.programid = :programId "
          + "    and r.status in ('APPROVED','RELEASED','RELEASED_WITHOUT_ORDER') "
          + "    and r.processingperiodid in "
          + "    ("
          + "      select pp.id from referencedata.processing_periods pp "
          + "      left join referencedata.processing_periods hpp on hpp.name = pp.name "
          + "      left join referencedata.processing_schedules ps on ps.id = pp.processingscheduleid "
          + "      where hpp.id = :periodId and ps.code = 'M1' "
          + "     )"
          + "  ) ";

  public static final String QUERY_MAX_VALUE_IN_LAST_PERIODS =
      "  ( select r.id "
          + "from requisition.requisitions r "
          + "where r.programid = :programId "
          + "  and r.facilityid in "
          + "      ( "
          + "          select rgm.facilityid "
          + "          from referencedata.requisition_group_members rgm "
          + "                   left join "
          + "               ( "
          + "                   select r.* "
          + "                   from requisition.requisitions r "
          + "                   where r.programid = :programId "
          + "                     and r.processingperiodid in "
          + "                         ( "
          + "                             select pp.id "
          + "                             from referencedata.processing_periods pp "
          + "                                    left join referencedata.processing_periods hpp on hpp.name = pp.name "
          + "                                    left join referencedata.processing_schedules ps "
          + "                                              on ps.id = pp.processingscheduleid "
          + "                             where hpp.id = :periodId "
          + "                               and ps.code = 'M1' "
          + "                         ) "
          + "               ) tmp on tmp.facilityid = rgm.facilityid "
          + "                   left join referencedata.requisition_groups rg on rg.id = rgm.requisitiongroupid "
          + "                   left join referencedata.requisition_group_program_schedules rgps "
          + "                             on rgps.requisitiongroupid = rg.id "
          + "                   left join referencedata.supervisory_nodes sn on sn.id = rg.supervisorynodeid "
          + "          where sn.facilityid = :facilityId "
          + "            and sn.parentid is not null "
          + "            and rgps.programid = :programId "
          + "            and (tmp.status is null or "
          + "                 tmp.status in ('INITIATED', 'SUBMITTED', 'AUTHORIZED', 'IN_APPROVAL', 'REJECTED')) "
          + "      ) "
          + "  and r.status in ('APPROVED','RELEASED','RELEASED_WITHOUT_ORDER') "
          + "  and r.processingperiodid in "
          + "      ( "
          + "          select pp.id "
          + "          from referencedata.processing_periods pp "
          + "                   left join referencedata.processing_schedules ps on ps.id = pp.processingscheduleid "
          + "          where ps.code = 'M1' "
          + "            and pp.startdate < "
          + "                (select pp.startdate "
          + "                 from referencedata.processing_periods pp "
          + "                       left join referencedata.processing_periods hpp on hpp.name = pp.name "
          + "                       left join referencedata.processing_schedules ps on ps.id = pp.processingscheduleid "
          + "                 where hpp.id = :periodId "
          + "                   and ps.code = 'M1') "
          + "          order by pp.startdate desc "
          + "          limit 3 "
          + "      )"
          + " ) ";

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

  public static class MmiaPatientLineItems {
    private MmiaPatientLineItems() {
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

}
