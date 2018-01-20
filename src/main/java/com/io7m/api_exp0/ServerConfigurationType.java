package com.io7m.api_exp0;

import org.immutables.value.Value;

@ImmutableStyleType
@Value.Immutable
public interface ServerConfigurationType
{
  @Value.Parameter
  String listenAddress();

  @Value.Parameter
  int listenPort();
}
