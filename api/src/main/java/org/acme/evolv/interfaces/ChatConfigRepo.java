package org.acme.evolv.interfaces;

import org.acme.evolv.DTO.ChatConfig;

public interface ChatConfigRepo {
  ChatConfig findByCompanyId(String companyId);
}