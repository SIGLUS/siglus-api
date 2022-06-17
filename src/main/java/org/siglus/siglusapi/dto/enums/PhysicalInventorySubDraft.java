package org.siglus.siglusapi.dto.enums;

public enum PhysicalInventorySubDraft {
  NOT_YET_STARTED(1),
  DRAFT(2),
  SUBMITTED(3);

  private final int value;

  PhysicalInventorySubDraft(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
