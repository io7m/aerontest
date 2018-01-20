package com.io7m.api_exp0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public final class ClientMain
{
  private static final Logger LOG = LoggerFactory.getLogger(ClientMain.class);

  private ClientMain()
  {

  }

  public static void main(final String[] args)
  {
    final ClientConfiguration config =
      ClientConfiguration.builder()
        .setServerAddress("127.0.0.1")
        .setServerPort(9999)
        .setLocalAddress("127.0.0.1")
        .setLocalPort(60000)
        .build();

    final ClientType client = Client.start(config, new ClientListenerType()
    {
      @Override
      public void onStart()
      {
        LOG.debug("onStart");
      }

      @Override
      public void onMessage(final String message)
      {
        LOG.debug("onMessage: {}", message);
      }
    });

    while (true) {
      try {
        try {
          client.send(ZonedDateTime.now().format(ISO_OFFSET_DATE_TIME));
        } catch (final IOException e) {
          LOG.error("i/o: ", e);
        }
        LOG.debug("sleeping");
        Thread.sleep(1000L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
