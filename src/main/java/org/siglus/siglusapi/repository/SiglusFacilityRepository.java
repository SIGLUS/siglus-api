package org.siglus.siglusapi.repository;

import java.util.UUID;

import org.siglus.common.domain.referencedata.Facility;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiglusFacilityRepository extends JpaRepository<Facility, UUID> {

  Facility findFirstByTypeId(UUID typeId);
}
