package org.siglus.siglusapi.util;

import static org.siglus.siglusapi.i18n.MessageKeys.ERROR_NO_PERIOD_MATCH;

import java.time.LocalDate;
import java.util.List;
import org.openlmis.referencedata.domain.ProcessingPeriod;
import org.siglus.siglusapi.dto.Message;
import org.siglus.siglusapi.exception.BusinessDataException;

public class PeriodUtil {

  public static ProcessingPeriod getPeriodDateIn(List<ProcessingPeriod> processingPeriods, LocalDate localDate) {
    return processingPeriods.stream().filter(period -> isDateInPeriod(period, localDate)).findFirst()
        .orElseThrow(() -> new BusinessDataException(new Message(ERROR_NO_PERIOD_MATCH)));
  }

  public static ProcessingPeriod getPeriodDateInDefaultNull(List<ProcessingPeriod> processingPeriods,
      LocalDate localDate) {
    return processingPeriods.stream().filter(period -> isDateInPeriod(period, localDate)).findFirst().orElse(null);
  }

  public static boolean isDateInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return !isDateNotInPeriod(period, localDate);
  }

  private static boolean isDateNotInPeriod(ProcessingPeriod period, LocalDate localDate) {
    return localDate.isBefore(period.getStartDate()) || localDate.isAfter(period.getEndDate());
  }

}
