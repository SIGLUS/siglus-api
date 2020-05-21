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

package org.openlmis.requisition.utils;

import static net.sf.jasperreports.engine.JRParameter.REPORT_LOCALE;
import static net.sf.jasperreports.engine.JRParameter.REPORT_RESOURCE_BUNDLE;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.APPROVED_QUANTITY;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.REMARKS_COLUMN;
import static org.openlmis.requisition.domain.requisition.RequisitionLineItem.SKIPPED_COLUMN;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.JRBand;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import org.openlmis.requisition.domain.RequisitionTemplateColumn;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.springframework.context.i18n.LocaleContextHolder;

public final class ReportUtils {
  private ReportUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Set parameters of rendered pdf report.
   */
  public static Map<String, Object> createParametersMap() {
    Map<String, Object> params = new HashMap<>();
    params.put("format", "pdf");

    Locale currentLocale = LocaleContextHolder.getLocale();
    params.put(REPORT_LOCALE, currentLocale);

    ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", currentLocale);
    params.put(REPORT_RESOURCE_BUNDLE, resourceBundle);

    return params;
  }

  /**
   * Sorts the map of requisition template columns by their display order, without 'skipped' column.
   * It also filters out 'approvedQuantity' column if the requisition status is either
   * INITIATED, REJECTED or SUBMITTED.
   *
   * @param map map of column keys to columns.
   * @return sorted map.
   */
  public static Map<String, RequisitionTemplateColumn> getSortedTemplateColumnsForPrint(
      Map<String, RequisitionTemplateColumn> map, RequisitionStatus requisitionStatus) {
    List<Map.Entry<String, RequisitionTemplateColumn>> sorted = map
        .entrySet()
        .stream()
        .filter(ent -> !SKIPPED_COLUMN.equals(ent.getKey()))
        .filter(ent -> ent.getValue().getIsDisplayed())
        .filter(ent -> !APPROVED_QUANTITY.equals(ent.getKey())
            && !REMARKS_COLUMN.equals(ent.getKey())
            || requisitionStatus.isAuthorized())
        .sorted(Comparator.comparingInt(ent -> ent.getValue().getDisplayOrder()))
        .collect(Collectors.toList());

    LinkedHashMap<String, RequisitionTemplateColumn> result = new LinkedHashMap<>();
    for (Map.Entry<String, RequisitionTemplateColumn> entry : sorted) {
      result.put(entry.getKey(), entry.getValue());
    }

    return result;
  }

  /**
   * Customizes template band to adjust columns order.
   * @param band Jasper Report band to edit.
   * @param columns map of requisition template columns.
   * @param margin page margin, to adjust initial column positions.
   */
  public static void customizeBandWithTemplateFields(
      JRBand band, Map<String, RequisitionTemplateColumn> columns, int width, int margin) {
    List<String> foundTemplateKeys = columns.keySet().stream()
        .filter(key -> band.getElementByKey(key) != null)
        .collect(Collectors.toList());
    List<JRDesignTextField> foundColumns = band.getChildren().stream()
        .filter(child -> child instanceof JRDesignTextField)
        .map(child -> (JRDesignTextField)child)
        .collect(Collectors.toList());

    double widthMultipier = getWidthMultipier(width, margin, foundTemplateKeys, foundColumns);

    JRDesignTextField prevField = null;
    for (String key : foundTemplateKeys) {
      JRDesignTextField field = (JRDesignTextField)band.getElementByKey(key);

      field.setWidth((int) (field.getWidth() * widthMultipier));
      setPositionAfterPreviousField(field, prevField, margin);
      prevField = field;
    }

    fillWidthGap(prevField, width, margin);
    removeSpareColumns(band, foundColumns, foundTemplateKeys);
  }

  private static double getWidthMultipier(int width, int margin, List<String> foundTemplateKeys,
                                          List<JRDesignTextField> foundColumns) {
    int toFill = 0;
    for (JRDesignTextField field : foundColumns) {
      if (!foundTemplateKeys.contains(field.getKey())) {
        toFill += field.getWidth();
      }
    }

    if (toFill != 0) {
      int lineWidth = width - 2 * margin;
      return (double)lineWidth / (lineWidth - toFill);
    }
    return 1;
  }

  private static void removeSpareColumns(
      JRBand band, List<JRDesignTextField> children, List<String> foundKeys) {
    for (JRDesignTextField child : children) {
      if (!foundKeys.contains(child.getKey())) {
        band.getChildren().remove(child);
      }
    }
  }

  private static void fillWidthGap(JRDesignTextField lastField, int width, int margin) {
    if (lastField != null) {
      int widthGap = (width - margin) - (lastField.getX() + lastField.getWidth());
      if (widthGap > 0) {
        lastField.setWidth(lastField.getWidth() + widthGap);
      }
    }
  }

  private static void setPositionAfterPreviousField(
      JRDesignTextField field, JRDesignTextField prev, int margin) {
    if (prev == null) {
      field.setX(margin);
    } else {
      field.setX(prev.getX() + prev.getWidth());
    }
  }
}
