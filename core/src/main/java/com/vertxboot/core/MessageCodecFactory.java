package com.vertxboot.core;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class MessageCodecFactory {

    private MessageCodecFactory() {
    }

    public static <Message> MessageCodec<Message, Message> messageCodec(Class<Message> messageClass) {
        return new MessageCodecImpl<>(messageClass);
    }

    // https://github.com/vert-x3/vertx-examples/blob/master/core-examples/src/main/java/io/vertx/example/core/eventbus/messagecodec/util/CustomMessageCodec.java
    private static class MessageCodecImpl<Message> implements MessageCodec<Message, Message> {

        private Class<Message> messageClass;

        private MessageCodecImpl(Class<Message> messageClass) {
            this.messageClass = messageClass;
        }

        @Override
        public void encodeToWire(Buffer buffer, Message message) {
            // Easiest ways is using JSON object
            JsonObject jsonToEncode = JsonObject.mapFrom(message);

            // Encode object to string
            String jsonToStr = jsonToEncode.encode();

            // Length of JSON: is NOT characters count
            int length = jsonToStr.getBytes().length;

            // Write data into given buffer
            buffer.appendInt(length);
            buffer.appendString(jsonToStr);
        }

        @Override
        public Message decodeFromWire(int position, Buffer buffer) {
            // My custom message starting from this *position* of buffer

            // Length of JSON
            int length = buffer.getInt(position);

            // Get JSON string by it`s length, Jump 4 because getInt() == 4 bytes
            String jsonStr = buffer.getString(position + Integer.BYTES, position + Integer.BYTES + length);
            JsonObject contentJson = new JsonObject(jsonStr);
            return contentJson.mapTo(messageClass);
        }

        @Override
        public Message transform(Message message) {
            // If a message is sent *locally* across the event bus.
            // This example sends message just as is
            return message;
        }

        @Override
        public String name() {
            // Each codec must have a unique name.
            // This is used to identify a codec when sending a message and for unregistering codecs.
            return this.getClass().getSimpleName() + messageClass.getName();
        }

        @Override
        public byte systemCodecID() {
            // Always -1
            return -1;
        }
    }
}
