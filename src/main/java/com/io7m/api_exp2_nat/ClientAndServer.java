package com.io7m.api_exp2_nat;

public final class ClientAndServer
{
  private ClientAndServer()
  {

  }

  public static void main(final String[] args)
  {
    final Server s =
      new Server("127.0.0.1", 9000, 9001);
    final Client c0 =
      new Client("127.0.0.1", 9000, 9001, "127.0.0.1",8000);
    final Client c1 =
      new Client("127.0.0.1", 9000, 9001, "127.0.0.1", 8001);

    s.start();
    c0.start();
    c1.start();
  }
}
