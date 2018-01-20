package com.io7m.aerontest0;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ConcurrentPublication;
import io.aeron.Publication;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FrameDescriptor;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class Server
{
  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private Server()
  {

  }

  public static void main(final String[] args)
  {
    final UnsafeBuffer buffer =
      new UnsafeBuffer(BufferUtil.allocateDirectAligned(1024, 16));

    LOG.debug("creating media context");
    try (MediaDriver.Context media_context = new MediaDriver.Context()) {

      LOG.debug("creating media driver: {}", media_context.aeronDirectory());
      try (MediaDriver media_driver = MediaDriver.launchEmbedded(media_context)) {

        LOG.debug("creating aeron context");
        try (Aeron.Context aeron_context = new Aeron.Context()) {
          aeron_context.aeronDirectoryName(media_driver.aeronDirectoryName());

          LOG.debug("creating aeron");
          try (Aeron aeron = Aeron.connect(aeron_context)) {

            final String uri_reliable =
              new ChannelUriStringBuilder()
                .endpoint("127.0.0.1:9999")
                .media("udp")
                .reliable(Boolean.TRUE)
                .mtu(Integer.valueOf(FrameDescriptor.FRAME_ALIGNMENT * 38))
                .build();
            LOG.debug("adding publication {}", uri_reliable);

            final String uri_unreliable =
              new ChannelUriStringBuilder()
                .endpoint("127.0.0.1:9999")
                .media("udp")
                .reliable(Boolean.FALSE)
                .mtu(Integer.valueOf(FrameDescriptor.FRAME_ALIGNMENT * 38))
                .build();
            LOG.debug("adding publication {}", uri_unreliable);

            try (ConcurrentPublication pub_reliable =
                   aeron.addPublication(uri_reliable, 104729)) {
              LOG.debug("added publication {}", uri_reliable);

              try (ConcurrentPublication pub_unreliable =
                     aeron.addPublication(uri_unreliable, 104723)) {
                LOG.debug("added publication {}", uri_unreliable);

                int index = 0;
                while (true) {
                  try {
                    {
                      final byte[] value = ("U " + index).getBytes(UTF_8);
                      buffer.putBytes(0, value);
                      final long result = pub_unreliable.offer(
                        buffer,
                        0,
                        value.length);
                      LOG.debug("result: {}", resultText(result));
                      ++index;
                    }

                    {
                      final byte[] value = ("R " + index).getBytes(UTF_8);
                      buffer.putBytes(0, value);
                      final long result = pub_reliable.offer(
                        buffer,
                        0,
                        value.length);
                      LOG.debug("result: {}", resultText(result));
                      ++index;
                    }

                    Thread.sleep(1000L);
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              }
            }
          }
        }
      }
    } finally {
      LOG.debug("exiting");
    }
  }

  private static String resultText(final long result)
  {
    if (result < 0L) {
      if (result == Publication.BACK_PRESSURED) {
        return "BACK_PRESSURED";
      }
      if (result == Publication.NOT_CONNECTED) {
        return "NOT_CONNECTED";
      }
      if (result == Publication.ADMIN_ACTION) {
        return "ADMIN_ACTION";
      }
      if (result == Publication.CLOSED) {
        return "CLOSED";
      }
      if (result == Publication.MAX_POSITION_EXCEEDED) {
        return "MAX_POSITION_EXCEEDED";
      }
      return "UNKNOWN";
    }
    return "OK";
  }
}
