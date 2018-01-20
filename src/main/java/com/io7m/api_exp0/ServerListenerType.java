package com.io7m.api_exp0;

public interface ServerListenerType
{
  void onStart();

  void onClientConnected();

  void onClientMessage(
    int client,
    String message);

  void onClientDisconnected();
}
