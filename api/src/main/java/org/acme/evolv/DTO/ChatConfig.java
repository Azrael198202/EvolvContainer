package org.acme.evolv.DTO;

public record ChatConfig(
  String companyId,
  String apiUrl,
  String welcomeText,
  String messageIconUrl,
  String themePrimary,
  String contentBg,
  String footerBg,
  String textColor,
  String bubbleUser,
  String bubbleBot
) {}