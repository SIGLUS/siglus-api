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

package org.siglus.siglusapi.dto.android.androidenum;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PatientTableName {

  PATIENTTYPE() {
    @Override
    public String findKeyByValue(String columnName) {
      return PatientType.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return PatientType.findValueByKey(key);
    }
  },
  NEWSECTION0() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection0.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection0.findValueByKey(key);
    }
  },
  NEWSECTION1() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection1.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection1.findValueByKey(key);
    }
  },
  NEWSECTION2() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection2.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection2.findValueByKey(key);
    }
  },
  NEWSECTION3() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection3.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection3.findValueByKey(key);
    }
  },
  NEWSECTION4() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection4.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection4.findValueByKey(key);
    }
  },
  TABLE_ARVT_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return PatientType.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return PatientType.findValueByKey(key);
    }
  },
  TABLE_PATIENTS_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection0.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection0.findValueByKey(key);
    }
  },
  TABLE_PROPHYLAXY_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection1.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection1.findValueByKey(key);
    }
  },
  TABLE_DISPENSED_DS_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection2.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection2.findValueByKey(key);
    }
  },
  TABLE_DISPENSED_DT_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection3.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection3.findValueByKey(key);
    }
  },
  TABLE_DISPENSED_DM_KEY() {
    @Override
    public String findKeyByValue(String columnName) {
      return NewSection4.findKeyByValue(columnName);
    }

    @Override
    public String findValueByKey(String key) {
      return NewSection4.findValueByKey(key);
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
