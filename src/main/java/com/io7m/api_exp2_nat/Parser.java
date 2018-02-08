package com.io7m.api_exp2_nat;

import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

final class Parser implements FragmentHandler
{
  private final Logger log;

  Parser(final Logger log)
  {
    this.log = log;
  }

  @Override
  public void onFragment(
    final DirectBuffer buffer,
    final int offset,
    final int length,
    final Header header)
  {
    final byte[] buf = new byte[length];
    buffer.getBytes(offset, buf);

    this.log.debug("received: [session 0x{}] [stream 0x{}] [term 0x{}] {}",
                   String.format("%8x", Integer.valueOf(header.sessionId())),
                   String.format("%8x", Integer.valueOf(header.streamId())),
                   String.format("%8x", Integer.valueOf(header.termId())),
                   new String(buf, UTF_8));
  }
}
