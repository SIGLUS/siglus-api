package org.siglus.siglusapi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.siglus.siglusapi.dto.SimpleRequisitionDto;

public class RequisitionUtil {

  public static Map<String, Object> getRequisitionExtraData(SimpleRequisitionDto simpleRequisitionDto) {
    if (Objects.isNull(simpleRequisitionDto) || StringUtils.isBlank(simpleRequisitionDto.getExtraData())) {
      return null;
    }
    return jsonStringToMap(simpleRequisitionDto.getExtraData());
  }

  @SneakyThrows
  private static Map<String, Object> jsonStringToMap(String jsonString) {
    return new ObjectMapper().readValue(jsonString, Map.class);
  }

}
