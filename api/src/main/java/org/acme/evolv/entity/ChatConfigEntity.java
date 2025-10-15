package org.acme.evolv.entity;

import java.util.UUID;

import org.acme.evolv.dto.ChatConfig;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_config")
public class ChatConfigEntity extends PanacheEntityBase {
  @Id
  @Column(name = "company_id")
  public UUID companyId;

  @Column(name = "api_url")          public String apiUrl;
  @Column(name = "header_title")     public String headerTitle;
  @Column(name = "message_text")     public String welcomeText;
  @Column(name = "message_lang")     public String messageLang;
  @Column(name = "header_icon_url")  public String headerIconUrl;
  @Column(name = "message_icon_url") public String messageIconUrl;
  @Column(name = "theme_primary")    public String themePrimary;
  @Column(name = "content_bg")       public String contentBg;
  @Column(name = "footer_bg")        public String footerBg;
  @Column(name = "text_color")       public String textColor;
  @Column(name = "bubble_user")      public String bubbleUser;
  @Column(name = "bubble_bot")       public String bubbleBot;

  public ChatConfig toDto() {
    return new ChatConfig(companyId.toString(), apiUrl, welcomeText, messageIconUrl, headerIconUrl,
        themePrimary, contentBg, footerBg, textColor, bubbleUser, bubbleBot);
  }
}