package com.io7m.aerontest0;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.FrameDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public final class Client
{
  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  private Client()
  {

  }

  public static void main(final String[] args)
  {
    final FragmentHandler handler = new FragmentAssembler((buffer, offset, length, header) -> {
      final byte[] data = new byte[length];
      buffer.getBytes(offset, data);
      LOG.debug("message: {}", new String(data, StandardCharsets.UTF_8));
    });

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
            LOG.debug("adding subscription {}", uri_reliable);

            final String uri_unreliable =
              new ChannelUriStringBuilder()
                .endpoint("127.0.0.1:9999")
                .media("udp")
                .reliable(Boolean.FALSE)
                .mtu(Integer.valueOf(FrameDescriptor.FRAME_ALIGNMENT * 38))
                .build();
            LOG.debug("adding subscription {}", uri_unreliable);

            try (Subscription sub_reliable =
                   aeron.addSubscription(uri_reliable, 104729)) {
              LOG.debug("subscribed {}", uri_reliable);
              try (Subscription sub_unreliable =
                     aeron.addSubscription(uri_unreliable, 104723)) {
                LOG.debug("subscribed {}", uri_unreliable);

                int wait = 10;
                while (!sub_reliable.isConnected() && wait >= 0) {
                  try {
                    LOG.debug("waiting for connection...");
                    Thread.sleep(1000L);
                    --wait;
                  } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }

                if (!sub_reliable.isConnected()) {
                  LOG.error("timed out waiting for connection");
                  return;
                }

                while (true) {
                  try {
                    sub_unreliable.poll(handler, Integer.MAX_VALUE);
                    sub_reliable.poll(handler, Integer.MAX_VALUE);
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
}
