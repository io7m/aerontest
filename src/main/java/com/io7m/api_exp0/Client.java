package com.io7m.api_exp0;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ExclusivePublication;
import io.aeron.driver.MediaDriver;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.io7m.api_exp0.ServerType.CLIENT_COMMAND_STREAM;
import static io.aeron.logbuffer.FrameDescriptor.FRAME_ALIGNMENT;
import static java.lang.Boolean.TRUE;

public final class Client implements ClientType
{
  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  private final ClientConfiguration config;
  private final ClientListenerType listener;
  private final MediaDriver.Context media_context;
  private final MediaDriver media;
  private final Aeron.Context aeron_context;
  private final Aeron aeron;
  private final ExclusivePublication command_send;
  private final UnsafeBuffer buffer;

  private Client(
    final ClientConfiguration in_config,
    final ClientListenerType in_listener)
  {
    this.config =
      Objects.requireNonNull(in_config, "config");
    this.listener =
      Objects.requireNonNull(in_listener, "listener");

    this.media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .aeronDirectoryName("/dev/shm/aeron-client");
    this.media =
      MediaDriver.launch(this.media_context);

    this.aeron_context =
      new Aeron.Context()
        .aeronDirectoryName("/dev/shm/aeron-client");
    this.aeron =
      Aeron.connect(this.aeron_context);

    final String send_uri =
      new ChannelUriStringBuilder()
        .mtu(Integer.valueOf(FRAME_ALIGNMENT * 38))
        .reliable(TRUE)
        .media("udp")
        .networkInterface(in_config.localAddress() + ":" + in_config.localPort())
        .endpoint(in_config.serverAddress() + ":" + in_config.serverPort())
        .build();

    LOG.debug("opening publication: {}", send_uri);

    this.command_send =
      this.aeron.addExclusivePublication(send_uri, CLIENT_COMMAND_STREAM);

    this.buffer =
      new UnsafeBuffer(BufferUtil.allocateDirectAligned(2048, 16));
  }

  public static ClientType start(
    final ClientConfiguration config,
    final ClientListenerType listener)
  {
    final ClientType client = new Client(config, listener);
    listener.onStart();
    return client;
  }

  @Override
  public void close()
  {
    this.aeron.close();
    this.media.close();
  }

  @Override
  public void send(final String message)
    throws IOException
  {
    final byte[] message_bytes = message.getBytes(StandardCharsets.UTF_8);
    this.buffer.putBytes(0, message_bytes);
    final long result =
      this.command_send.offer(this.buffer, 0, message_bytes.length);

    if (result < 0L) {
      throw new IOException("Could not send: " + result);
    }
  }
}
