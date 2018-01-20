package com.io7m.api_exp0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerMain
{
  private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);

  private ServerMain()
  {

  }

  public static void main(final String[] args)
    throws Exception
  {
    final ServerConfiguration config =
      ServerConfiguration.builder()
        .setListenAddress("127.0.0.1")
        .setListenPort(9999)
        .build();

    final ServerType server =
      Server.start(config, new ServerListenerType()
      {
        @Override
        public void onStart()
        {
          LOG.debug("onStart");
        }

        @Override
        public void onClientConnected()
        {
          LOG.debug("onClientConnected");
        }

        @Override
        public void onClientMessage(
          final int client,
          final String message)
        {
          LOG.debug(
            "onClientMessage [{}]: {}",
            Integer.toUnsignedString(client, 16),
            message);
        }

        @Override
        public void onClientDisconnected()
        {
          LOG.debug("onClientDisconnected");
        }
      });

    while (true) {
      try {
        server.poll();
        LOG.debug("sleeping");
        Thread.sleep(1000L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
