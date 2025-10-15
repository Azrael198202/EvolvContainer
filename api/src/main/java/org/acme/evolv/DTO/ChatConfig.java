package org.acme.evolv.dto;

public record ChatConfig(
  String companyId,
  String apiUrl,
  String welcomeText,
  String messageIconUrl,
  String headerIconUrl,
  String themePrimary,
  String contentBg,
  String footerBg,
  String textColor,
  String bubbleUser,
  String bubbleBot
) {}