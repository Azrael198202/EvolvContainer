package org.acme.evolv.factory.db;

import org.acme.evolv.DTO.ChatConfig;
import org.acme.evolv.Enity.ChatConfigEntity;
import org.acme.evolv.interfaces.ChatConfigRepo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@ApplicationScoped
@Named("panacheRepo")
public class ChatConfigRepoPanache implements ChatConfigRepo {
  @Override
  public ChatConfig findByCompanyId(String companyId) {
    var e = ChatConfigEntity.findById(companyId);
    return e == null ? null : ((ChatConfigEntity)e).toDto();
  }
}