package org.siglus.siglusapi.dto.fc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageInfoDto {

  private int totalObjects;

  private int totalPages = 1;

  private int pageNumber;

  private int pSize;

}
