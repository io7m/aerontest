package com.io7m.api_exp0;

import java.io.Closeable;
import java.io.IOException;

public interface ServerType extends Closeable
{
  int CLIENT_COMMAND_STREAM = 103067;

  int CLIENT_RESPONSE_STREAM = 102197;

  void sendToClient(
    int client,
    String message)
    throws IOException;

  void poll();
}
