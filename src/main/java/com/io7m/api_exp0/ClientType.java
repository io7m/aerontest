package com.io7m.api_exp0;

import java.io.Closeable;
import java.io.IOException;

public interface ClientType extends Closeable
{
  void send(String message)
    throws IOException;
}
