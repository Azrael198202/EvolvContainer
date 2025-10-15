package org.acme.evolv.interfaces;

import org.acme.evolv.dto.ScenarioBasicDTO;

public interface ScenarioBase {
  ScenarioBasicDTO findByCompanyId(String companyId);
}