package com.io7m.api_exp2_nat;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
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

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

final class Server extends Thread
{
  private final Logger log;
  private final MediaDriver.Context media_context;
  private final MediaDriver media;
  private final Aeron.Context aeron_context;
  private final Aeron aeron;
  private final UnsafeBuffer buffer;
  private final FragmentAssembler fragment_assembler;
  private final String local_address;
  private final int local_control_port;
  private final int local_data_port;
  private String publication_uri;
  private final ConcurrentHashMap<Integer, ExclusivePublication> clients;

  Server(
    final String address,
    final int control_port,
    final int data_port)
  {
    super("server");

    this.log = LoggerFactory.getLogger("Server");

    this.local_address = address;
    this.local_control_port = control_port;
    this.local_data_port = data_port;

    this.media_context =
      new MediaDriver.Context()
        .dirDeleteOnStart(true)
        .aeronDirectoryName("/tmp/aeron-server");
    this.media =
      MediaDriver.launch(this.media_context);

    this.aeron_context =
      new Aeron.Context()
        .aeronDirectoryName("/tmp/aeron-server");
    this.aeron =
      Aeron.connect(this.aeron_context);

    this.buffer =
      new UnsafeBuffer(BufferUtil.allocateDirectAligned(2048, 16));
    this.fragment_assembler =
      new FragmentAssembler(new Parser(this.log));

    this.clients = new ConcurrentHashMap<>(32);
  }

  private static String serverBroadcastMessage()
  {
    return new StringBuilder(128)
      .append("Server broadcast HELLO: ")
      .append(LocalDateTime.now().format(ISO_LOCAL_DATE_TIME))
      .toString();
  }

  private static String serverIndividualMessage(final int session)
  {
    return new StringBuilder(128)
      .append("Server individual HELLO: 0x")
      .append(Integer.toUnsignedString(session, 16))
      .append(" ")
      .append(LocalDateTime.now().format(ISO_LOCAL_DATE_TIME))
      .toString();
  }

  public static void main(final String[] args)
  {
    final Args jargs = new Args();
    JCommander.newBuilder()
      .addObject(jargs)
      .build()
      .parse(args);

    final Server s =
      new Server(
        jargs.address,
        jargs.control_port,
        jargs.data_port);
    s.start();
  }

  @Override
  public void run()
  {
    this.log.debug("start");

    /*
     * Create a publication that can be used to broadcast messages to all
     * clients. It specifies a "control" port to which clients will send
     * control messages. Clients add themselves as destinations automatically
     * and this allows messages sent from the server to clients to traverse
     * NAT.
     */

    this.publication_uri =
      new ChannelUriStringBuilder()
        .mtu(Shared.MTU)
        .reliable(Boolean.TRUE)
        .media("udp")
        .controlEndpoint(this.local_address + ":" + this.local_control_port)
        .build();

    this.log.debug(
      "opening control publication: {}",
      this.publication_uri);

    final ExclusivePublication pub =
      this.aeron.addExclusivePublication(
        this.publication_uri, Shared.STREAM_ID);

    /*
     * Create a subscription that will be used to receive messages from
     * clients. When a client connects, the #onImageAvailable handler is
     * called and the server then sets up a publication in the handler
     * used to send messages to that client specifically.
     */

    final String sub_uri =
      new ChannelUriStringBuilder()
        .mtu(Shared.MTU)
        .reliable(Boolean.TRUE)
        .media("udp")
        .endpoint(this.local_address + ":" + this.local_data_port)
        .build();

    this.log.debug("opening data subscription: {}", sub_uri);

    final Subscription sub =
      this.aeron.addSubscription(
        sub_uri,
        Shared.STREAM_ID,
        this::onImageAvailable,
        this::onImageUnavailable);

    /*
     * Go into a loop, reading from and sending messages to clients once
     * per second.
     */

    while (true) {
      this.log.trace("pub connected: {}", Boolean.valueOf(pub.isConnected()));
      if (pub.isConnected()) {
        Utilities.send(this.log, pub, this.buffer, serverBroadcastMessage());
      }

      this.log.trace("sub connected: {}", Boolean.valueOf(sub.isConnected()));
      if (sub.isConnected()) {
        sub.poll(this.fragment_assembler, 10);
      }

      /*
       * Send an individually-tailored message to each client.
       */

      this.clients.forEach(
        (session, client_pub) ->
          Utilities.send(
            this.log,
            client_pub,
            this.buffer,
            serverIndividualMessage(session.intValue())));

      try {
        Thread.sleep(1000L);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * A client has disconnected.
   */

  private void onImageUnavailable(final Image image)
  {
    final Integer session = Integer.valueOf(image.sessionId());

    this.log.debug(
      "onImageUnavailable: [0x{}] {}",
      String.format("%08x", session),
      image.sourceIdentity());

    try (ExclusivePublication pub = this.clients.remove(session)) {
      this.log.debug("closing publication: {}", pub);
    }
  }

  /**
   * A client has connected.
   */

  private void onImageAvailable(final Image image)
  {
    final Integer session = Integer.valueOf(image.sessionId());

    this.log.debug(
      "onImageAvailable: [0x{}] {}",
      String.format("%08x", session),
      image.sourceIdentity());

    /*
     * Create a new publication using a specific session ID. Messages sent
     * to this publication will only go to this specific client.
     */

    final String client_pub_uri =
      new ChannelUriStringBuilder()
        .mtu(Shared.MTU)
        .reliable(Boolean.TRUE)
        .media("udp")
        .sessionId(session)
        .controlEndpoint(this.local_address + ":" + this.local_control_port)
        .build();

    this.log.debug("creating client publication: {}", client_pub_uri);

    final ExclusivePublication client_pub =
      this.aeron.addExclusivePublication(client_pub_uri, Shared.STREAM_ID);

    assert !this.clients.containsKey(session);
    this.clients.put(session, client_pub);
  }

  private static final class Args
  {
    @Parameter(
      names = "--local-address",
      description = "Local address",
      required = true)
    private String address;

    @Parameter(
      names = "--local-control-port",
      description = "Local control port",
      required = false)
    private int control_port = 9000;

    @Parameter(
      names = "--local-data-port",
      description = "Local data port",
      required = false)
    private int data_port = 9001;
  }
}
