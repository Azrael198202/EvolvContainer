package org.acme.evolv.interfaces;

import org.acme.evolv.dto.ChatConfig;

public interface ChatConfigRepo {
  ChatConfig findByCompanyId(String companyId);
}