/*
 * Copyright 2020 Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.api.gax.httpjson;

import com.google.api.core.BetaApi;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * This class serializes/deserializes protobuf {@link Message} for REST interactions. It serializes
 * requests protobuf messages into REST messages, splitting the message into the JSON request body,
 * URL path parameters, and query parameters. It deserializes JSON responses into response protobuf
 * message.
 */
@BetaApi
public class ProtoRestSerializer<RequestT extends Message> {
  private ProtoRestSerializer() {}

  /** Creates a new instance of ProtoRestSerializer. */
  public static <RequestT extends Message> ProtoRestSerializer<RequestT> create() {
    return new ProtoRestSerializer<>();
  }

  /**
   * Serializes the data from {@code message} to a JSON string. The implementation relies on
   * protobuf native JSON formatter.
   *
   * @param message a message to serialize
   */
  String toJson(RequestT message) {
    try {
      return JsonFormat.printer().print(message);
    } catch (InvalidProtocolBufferException e) {
      throw new ProtoRestSerializationException("Failed to serialize message to JSON", e);
    }
  }

  /**
   * Deserializes a {@code message} from an input stream to a protobuf message.
   *
   * @param message the input stream with a JSON-encoded message in it
   * @param builder an empty builder for the specific {@code RequestT} message to serialize
   */
  @SuppressWarnings("unchecked")
  RequestT fromJson(InputStream message, Message.Builder builder) {
    try (Reader json = new InputStreamReader(message)) {
      JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
      return (RequestT) builder.build();
    } catch (IOException e) {
      throw new ProtoRestSerializationException("Failed to parse response message", e);
    }
  }

  /**
   * Puts a message field in {@code fields} map which will be used to populate URL path of a
   * request.
   *
   * @param fields a map with serialized fields
   * @param fieldName a field name
   * @param fieldValue a field value
   */
  public void putPathParam(Map<String, String> fields, String fieldName, Object fieldValue) {
    if (isDefaultValue(fieldName, fieldValue)) {
      return;
    }
    fields.put(fieldName, String.valueOf(fieldValue));
  }

  /**
   * Puts a message field in {@code fields} map which will be used to populate query parameters of a
   * request.
   *
   * @param fields a map with serialized fields
   * @param fieldName a field name
   * @param fieldValue a field value
   */
  public void putQueryParam(Map<String, List<String>> fields, String fieldName, Object fieldValue) {
    // Avoids empty query parameter
    if (isDefaultValue(fieldName, fieldValue)) {
      return;
    }

    ImmutableList.Builder<String> paramValueList = ImmutableList.builder();
    if (fieldValue instanceof List<?>) {
      for (Object fieldValueItem : (List<?>) fieldValue) {
        paramValueList.add(String.valueOf(fieldValueItem));
      }
    } else {
      paramValueList.add(String.valueOf(fieldValue));
    }

    fields.put(fieldName, paramValueList.build());
  }

  /**
   * Serializes a message to a request body in a form of JSON-encoded string.
   *
   * @param fieldName a name of a request message field this message belongs to
   * @param fieldValue a field value to serialize
   */
  public String toBody(String fieldName, RequestT fieldValue) {
    return toJson(fieldValue);
  }

  private boolean isDefaultValue(String fieldName, Object fieldValue) {
    // TODO: Revisit this approach to ensure proper default-value handling as per design.
    if (fieldValue instanceof Number) {
      return ((Number) fieldValue).longValue() == 0L;
    } else if (fieldValue instanceof String) {
      return ((String) fieldValue).isEmpty();
    }

    return false;
  }
}