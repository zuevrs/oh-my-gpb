package ru.gazprombank.automation.akitagpb.modules.api.rest;

/** Параметры для формирования http запроса */
public enum RequestParamType {
  PARAMETER,
  HEADER,
  BODY,
  BODY_BYTE,
  COOKIE,
  API_SPEC,
  NO_SSL_CHECK,
  SSL_CERTIFICATE_FILE,
  SSL_CERTIFICATE_PASSWORD,
  SSL_CERTIFICATE_TYPE,
  VAR,
  MULTIPART,
  MULTIPART_FILE,
  MULTIPART_FILE_CONTENT,
  MULTIPART_UTF8,
  FORM_PARAM
}
