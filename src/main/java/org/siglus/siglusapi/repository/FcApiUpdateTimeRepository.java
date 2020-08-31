package org.siglus.siglusapi.repository;

import java.util.UUID;
import org.siglus.siglusapi.domain.FcApiUpdateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcApiUpdateTimeRepository extends JpaRepository<FcApiUpdateTime, UUID> {

}
