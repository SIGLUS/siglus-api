package org.siglus.siglusapi.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiglusRequisitionDateDto {
  private UUID requisitionId;
  private LocalDate initiatedDate;
}
