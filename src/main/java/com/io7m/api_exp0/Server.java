package com.io7m.api_exp0;

import io.aeron.Aeron;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ExclusivePublication;
import io.aeron.FragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import org.agrona.BufferUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static io.aeron.logbuffer.FrameDescriptor.FRAME_ALIGNMENT;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class Server implements ServerType
{
  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private final ServerConfiguration config;
  private final ServerListenerType listener;
  private final MediaDriver.Context media_context;
  private final MediaDriver media;
  private final Aeron.Context aeron_context;
  private final Aeron aeron;
  private final Subscription command_receive;
  private final ConcurrentHashMap<Integer, ServerClient> clients;
  private final FragmentAssembler fragment_assembler;

  private Server(
    final ServerConfiguration in_config,
    final ServerListenerType in_listener)
  {
    this.config =
      Objects.requireNonNull(in_config, "config");
    this.listener =
      Objects.requireNonNull(in_listener, "listener");

    this.media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .aeronDirectoryName("/dev/shm/aeron-server");
    this.media =
      MediaDriver.launch(this.media_context);

    this.aeron_context =
      new Aeron.Context()
        .aeronDirectoryName("/dev/shm/aeron-server");
    this.aeron =
      Aeron.connect(this.aeron_context);

    this.clients = new ConcurrentHashMap<>(32);

    final String uri =
      new ChannelUriStringBuilder()
        .mtu(Integer.valueOf(FRAME_ALIGNMENT * 38))
        .reliable(Boolean.TRUE)
        .media("udp")
        .endpoint(in_config.listenAddress() + ":" + in_config.listenPort())
        .build();

    LOG.debug("opening subscription: {}", uri);

    this.command_receive =
      this.aeron.addSubscription(
        uri,
        CLIENT_COMMAND_STREAM,
        this::onAvailableImage,
        this::onUnavailableImage);

    this.fragment_assembler =
      new FragmentAssembler((buffer, offset, length, header) -> {
        final byte[] buf = new byte[length];
        buffer.getBytes(offset, buf);
        this.listener.onClientMessage(
          header.sessionId(),
          new String(buf, UTF_8));
      });
  }

  public static ServerType start(
    final ServerConfiguration config,
    final ServerListenerType listener)
  {
    final ServerType server = new Server(config, listener);
    listener.onStart();
    return server;
  }

  private void onUnavailableImage(final Image image)
  {
    LOG.debug(
      "onUnavailableImage: {} {}",
      image.sourceIdentity(),
      Integer.toUnsignedString(image.sessionId(), 16));

    this.clients.remove(
      Integer.valueOf(image.sessionId()));

    this.listener.onClientDisconnected();
  }

  private void onAvailableImage(final Image image)
  {
    LOG.debug(
      "onAvailableImage: {} {}",
      image.sourceIdentity(),
      Integer.toUnsignedString(image.sessionId(), 16));

    final String uri =
      new ChannelUriStringBuilder()
        .mtu(Integer.valueOf(FRAME_ALIGNMENT * 38))
        .reliable(Boolean.TRUE)
        .media("udp")
        .endpoint(image.sourceIdentity())
        .build();

    LOG.debug("creating publication: {}", uri);

    final ExclusivePublication client_send =
      this.aeron.addExclusivePublication(uri, CLIENT_RESPONSE_STREAM);

    final ServerClient server_client =
      new ServerClient(image.sessionId(), client_send);

    this.clients.put(
      Integer.valueOf(image.sessionId()),
      server_client);

    this.listener.onClientConnected();
  }

  @Override
  public void sendToClient(
    final int client,
    final String message)
    throws IOException
  {
    final ServerClient server_client = this.clients.get(Integer.valueOf(client));
    if (server_client != null) {
      server_client.send(message);
    } else {
      throw new IllegalArgumentException("No such client: " + client);
    }
  }

  @Override
  public void poll()
  {
    this.command_receive.poll(this.fragment_assembler, Integer.MAX_VALUE);
  }

  @Override
  public void close()
  {
    this.aeron.close();
    this.media.close();
  }

  private static final class ServerClient
  {
    private final int session;
    private final ExclusivePublication client_send;
    private final UnsafeBuffer buffer;

    ServerClient(
      final int session,
      final ExclusivePublication client_send)
    {
      this.session = session;
      this.client_send = client_send;
      this.buffer =
        new UnsafeBuffer(BufferUtil.allocateDirectAligned(2048, 16));
    }

    public void send(final String message)
      throws IOException
    {
      final byte[] message_bytes = message.getBytes(UTF_8);
      this.buffer.putBytes(0, message_bytes);
      final long result =
        this.client_send.offer(this.buffer, 0, message_bytes.length);

      if (result < 0L) {
        throw new IOException("Could not send: " + result);
      }
    }
  }
}
