package com.io7m.api_exp0;

import org.immutables.value.Value;

@ImmutableStyleType
@Value.Immutable
public interface ClientConfigurationType
{
  @Value.Parameter
  String serverAddress();

  @Value.Parameter
  int serverPort();

  @Value.Parameter
  String localAddress();

  @Value.Parameter
  int localPort();
}
