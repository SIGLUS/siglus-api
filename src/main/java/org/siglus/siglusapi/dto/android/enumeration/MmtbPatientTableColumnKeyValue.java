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

package org.siglus.siglusapi.dto.android.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MmtbPatientTableColumnKeyValue {

  NEWSECTION3() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable1.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable1.findValueByKey(key);
    }
  },
  NEWSECTION4() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable2.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable2.findValueByKey(key);
    }
  },
  NEWSECTION2() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable3.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable3.findValueByKey(key);
    }
  },
  PATIENTTYPE() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable4.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable4.findValueByKey(key);
    }
  },
  NEWSECTION0() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable5.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable5.findValueByKey(key);
    }
  },
  NEWSECTION1() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable6.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable6.findValueByKey(key);
    }
  },
  TABLE_TREATMENT_ADULT_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable1.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable1.findValueByKey(key);
    }
  },
  TABLE_TREATMENT_PEDIATRIC_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable2.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable2.findValueByKey(key);
    }
  },
  TABLE_CONSUMPTION_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable3.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable3.findValueByKey(key);
    }
  },
  TABLE_NEW_PATIENT_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable4.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable4.findValueByKey(key);
    }
  },
  TABLE_PROPHYLAXIS_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable5.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable5.findValueByKey(key);
    }
  },
  TABLE_PROPHYLACTICS_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return MmtbPatientTable6.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return MmtbPatientTable6.findValueByKey(key);
    }
  };

  @SuppressWarnings("unused")
  public String findKeyByValue(String columnName) {
    return null;
  }

  @SuppressWarnings("unused")
  public String findValueByKey(String columnName) {
    return null;
  }
}
