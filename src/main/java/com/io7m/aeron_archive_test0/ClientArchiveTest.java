package com.io7m.aeron_archive_test0;

import io.aeron.Aeron;
import io.aeron.archive.client.AeronArchive;
import io.aeron.driver.MediaDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientArchiveTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ClientArchiveTest.class);

  private ClientArchiveTest()
  {

  }

  public static void main(
    final String[] args)
  {
    LOG.debug("creating media context");
    try (MediaDriver.Context media_context =
           new MediaDriver.Context()
             .dirDeleteOnStart(true)
             .errorHandler(ClientArchiveTest::onError)
             .aeronDirectoryName("/dev/shm/someone-aeron-0")) {

      LOG.debug("creating media driver");
      try (MediaDriver media_driver = MediaDriver.launch(media_context)) {

        LOG.debug("creating aeron context");
        try (Aeron.Context aeron_context = new Aeron.Context()) {
          aeron_context.aeronDirectoryName(media_driver.aeronDirectoryName());

          LOG.debug("creating aeron");
          try (Aeron aeron = Aeron.connect(aeron_context)) {
            runAeronArchiveClient(aeron);
          }
        }
      }
    }
  }

  private static void onError(final Throwable e)
  {
    LOG.error("onError: ", e);
  }

  private static void runAeronArchiveClient(final Aeron aeron)
  {
    aeron.nextCorrelationId();

    LOG.debug("connecting");
    final AeronArchive.Context context =
      new AeronArchive.Context()
        .controlResponseChannel("aeron:udp?endpoint=127.0.0.1:9998")
        .controlResponseStreamId(104729)
        .aeron(aeron);

    try (AeronArchive archive = AeronArchive.connect(context)) {
      LOG.debug("connected");
    }
  }
}
