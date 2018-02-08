package com.io7m.api_exp2_nat;

import io.aeron.logbuffer.FrameDescriptor;

public final class Shared
{
  /*
   * A sensible MTU value.
   */

  public static final Integer MTU =
    Integer.valueOf(FrameDescriptor.FRAME_ALIGNMENT * 38);

  /**
   * The stream ID that the server and client use for messages.
   */

  public static final int STREAM_ID = 0xcafe0000;

  private Shared()
  {

  }
}
