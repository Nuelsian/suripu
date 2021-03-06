// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: filetransfer.proto

package com.hello.suripu.api.audio;

public final class FileTransfer {
  private FileTransfer() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface FileMessageOrBuilder
      extends com.google.protobuf.MessageOrBuilder {

    // optional string device_id = 1;
    /**
     * <code>optional string device_id = 1;</code>
     *
     * <pre>
     *device mac address (really device ID)
     * </pre>
     */
    boolean hasDeviceId();
    /**
     * <code>optional string device_id = 1;</code>
     *
     * <pre>
     *device mac address (really device ID)
     * </pre>
     */
    java.lang.String getDeviceId();
    /**
     * <code>optional string device_id = 1;</code>
     *
     * <pre>
     *device mac address (really device ID)
     * </pre>
     */
    com.google.protobuf.ByteString
        getDeviceIdBytes();

    // optional int32 unix_time = 2;
    /**
     * <code>optional int32 unix_time = 2;</code>
     *
     * <pre>
     *time in which transfer took place
     * </pre>
     */
    boolean hasUnixTime();
    /**
     * <code>optional int32 unix_time = 2;</code>
     *
     * <pre>
     *time in which transfer took place
     * </pre>
     */
    int getUnixTime();

    // optional bytes data = 3;
    /**
     * <code>optional bytes data = 3;</code>
     *
     * <pre>
     *file data
     * </pre>
     */
    boolean hasData();
    /**
     * <code>optional bytes data = 3;</code>
     *
     * <pre>
     *file data
     * </pre>
     */
    com.google.protobuf.ByteString getData();

    // optional int64 position = 4;
    /**
     * <code>optional int64 position = 4;</code>
     *
     * <pre>
     *position in which you wish to start in the file.  use this to resume a file transfer
     * </pre>
     */
    boolean hasPosition();
    /**
     * <code>optional int64 position = 4;</code>
     *
     * <pre>
     *position in which you wish to start in the file.  use this to resume a file transfer
     * </pre>
     */
    long getPosition();

    // optional string fileid = 5;
    /**
     * <code>optional string fileid = 5;</code>
     *
     * <pre>
     *some sort of unique identifier of the file (name + device + time or something)
     * </pre>
     */
    boolean hasFileid();
    /**
     * <code>optional string fileid = 5;</code>
     *
     * <pre>
     *some sort of unique identifier of the file (name + device + time or something)
     * </pre>
     */
    java.lang.String getFileid();
    /**
     * <code>optional string fileid = 5;</code>
     *
     * <pre>
     *some sort of unique identifier of the file (name + device + time or something)
     * </pre>
     */
    com.google.protobuf.ByteString
        getFileidBytes();
  }
  /**
   * Protobuf type {@code FileMessage}
   */
  public static final class FileMessage extends
      com.google.protobuf.GeneratedMessage
      implements FileMessageOrBuilder {
    // Use FileMessage.newBuilder() to construct.
    private FileMessage(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
      this.unknownFields = builder.getUnknownFields();
    }
    private FileMessage(boolean noInit) { this.unknownFields = com.google.protobuf.UnknownFieldSet.getDefaultInstance(); }

    private static final FileMessage defaultInstance;
    public static FileMessage getDefaultInstance() {
      return defaultInstance;
    }

    public FileMessage getDefaultInstanceForType() {
      return defaultInstance;
    }

    private final com.google.protobuf.UnknownFieldSet unknownFields;
    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
        getUnknownFields() {
      return this.unknownFields;
    }
    private FileMessage(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      initFields();
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {
              bitField0_ |= 0x00000001;
              deviceId_ = input.readBytes();
              break;
            }
            case 16: {
              bitField0_ |= 0x00000002;
              unixTime_ = input.readInt32();
              break;
            }
            case 26: {
              bitField0_ |= 0x00000004;
              data_ = input.readBytes();
              break;
            }
            case 32: {
              bitField0_ |= 0x00000008;
              position_ = input.readInt64();
              break;
            }
            case 42: {
              bitField0_ |= 0x00000010;
              fileid_ = input.readBytes();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e.getMessage()).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.hello.suripu.api.audio.FileTransfer.internal_static_FileMessage_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.hello.suripu.api.audio.FileTransfer.internal_static_FileMessage_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.hello.suripu.api.audio.FileTransfer.FileMessage.class, com.hello.suripu.api.audio.FileTransfer.FileMessage.Builder.class);
    }

    public static com.google.protobuf.Parser<FileMessage> PARSER =
        new com.google.protobuf.AbstractParser<FileMessage>() {
      public FileMessage parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new FileMessage(input, extensionRegistry);
      }
    };

    @java.lang.Override
    public com.google.protobuf.Parser<FileMessage> getParserForType() {
      return PARSER;
    }

    private int bitField0_;
    // optional string device_id = 1;
    public static final int DEVICE_ID_FIELD_NUMBER = 1;
    private java.lang.Object deviceId_;
    /**
     * <code>optional string device_id = 1;</code>
     *
     * <pre>
     *device mac address (really device ID)
     * </pre>
     */
    public boolean hasDeviceId() {
      return ((bitField0_ & 0x00000001) == 0x00000001);
    }
    /**
     * <code>optional string device_id = 1;</code>
     *
     * <pre>
     *device mac address (really device ID)
     * </pre>
     */
    public java.lang.String getDeviceId() {
      java.lang.Object ref = deviceId_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          deviceId_ = s;
        }
        return s;
      }
    }
    /**
     * <code>optional string device_id = 1;</code>
     *
     * <pre>
     *device mac address (really device ID)
     * </pre>
     */
    public com.google.protobuf.ByteString
        getDeviceIdBytes() {
      java.lang.Object ref = deviceId_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        deviceId_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    // optional int32 unix_time = 2;
    public static final int UNIX_TIME_FIELD_NUMBER = 2;
    private int unixTime_;
    /**
     * <code>optional int32 unix_time = 2;</code>
     *
     * <pre>
     *time in which transfer took place
     * </pre>
     */
    public boolean hasUnixTime() {
      return ((bitField0_ & 0x00000002) == 0x00000002);
    }
    /**
     * <code>optional int32 unix_time = 2;</code>
     *
     * <pre>
     *time in which transfer took place
     * </pre>
     */
    public int getUnixTime() {
      return unixTime_;
    }

    // optional bytes data = 3;
    public static final int DATA_FIELD_NUMBER = 3;
    private com.google.protobuf.ByteString data_;
    /**
     * <code>optional bytes data = 3;</code>
     *
     * <pre>
     *file data
     * </pre>
     */
    public boolean hasData() {
      return ((bitField0_ & 0x00000004) == 0x00000004);
    }
    /**
     * <code>optional bytes data = 3;</code>
     *
     * <pre>
     *file data
     * </pre>
     */
    public com.google.protobuf.ByteString getData() {
      return data_;
    }

    // optional int64 position = 4;
    public static final int POSITION_FIELD_NUMBER = 4;
    private long position_;
    /**
     * <code>optional int64 position = 4;</code>
     *
     * <pre>
     *position in which you wish to start in the file.  use this to resume a file transfer
     * </pre>
     */
    public boolean hasPosition() {
      return ((bitField0_ & 0x00000008) == 0x00000008);
    }
    /**
     * <code>optional int64 position = 4;</code>
     *
     * <pre>
     *position in which you wish to start in the file.  use this to resume a file transfer
     * </pre>
     */
    public long getPosition() {
      return position_;
    }

    // optional string fileid = 5;
    public static final int FILEID_FIELD_NUMBER = 5;
    private java.lang.Object fileid_;
    /**
     * <code>optional string fileid = 5;</code>
     *
     * <pre>
     *some sort of unique identifier of the file (name + device + time or something)
     * </pre>
     */
    public boolean hasFileid() {
      return ((bitField0_ & 0x00000010) == 0x00000010);
    }
    /**
     * <code>optional string fileid = 5;</code>
     *
     * <pre>
     *some sort of unique identifier of the file (name + device + time or something)
     * </pre>
     */
    public java.lang.String getFileid() {
      java.lang.Object ref = fileid_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs = 
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        if (bs.isValidUtf8()) {
          fileid_ = s;
        }
        return s;
      }
    }
    /**
     * <code>optional string fileid = 5;</code>
     *
     * <pre>
     *some sort of unique identifier of the file (name + device + time or something)
     * </pre>
     */
    public com.google.protobuf.ByteString
        getFileidBytes() {
      java.lang.Object ref = fileid_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        fileid_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private void initFields() {
      deviceId_ = "";
      unixTime_ = 0;
      data_ = com.google.protobuf.ByteString.EMPTY;
      position_ = 0L;
      fileid_ = "";
    }
    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized != -1) return isInitialized == 1;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        output.writeBytes(1, getDeviceIdBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        output.writeInt32(2, unixTime_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        output.writeBytes(3, data_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        output.writeInt64(4, position_);
      }
      if (((bitField0_ & 0x00000010) == 0x00000010)) {
        output.writeBytes(5, getFileidBytes());
      }
      getUnknownFields().writeTo(output);
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (((bitField0_ & 0x00000001) == 0x00000001)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, getDeviceIdBytes());
      }
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(2, unixTime_);
      }
      if (((bitField0_ & 0x00000004) == 0x00000004)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(3, data_);
      }
      if (((bitField0_ & 0x00000008) == 0x00000008)) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(4, position_);
      }
      if (((bitField0_ & 0x00000010) == 0x00000010)) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(5, getFileidBytes());
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    @java.lang.Override
    protected java.lang.Object writeReplace()
        throws java.io.ObjectStreamException {
      return super.writeReplace();
    }

    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseDelimitedFrom(input, extensionRegistry);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return PARSER.parseFrom(input);
    }
    public static com.hello.suripu.api.audio.FileTransfer.FileMessage parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return PARSER.parseFrom(input, extensionRegistry);
    }

    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(com.hello.suripu.api.audio.FileTransfer.FileMessage prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code FileMessage}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder>
       implements com.hello.suripu.api.audio.FileTransfer.FileMessageOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return com.hello.suripu.api.audio.FileTransfer.internal_static_FileMessage_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return com.hello.suripu.api.audio.FileTransfer.internal_static_FileMessage_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                com.hello.suripu.api.audio.FileTransfer.FileMessage.class, com.hello.suripu.api.audio.FileTransfer.FileMessage.Builder.class);
      }

      // Construct using com.hello.suripu.api.audio.FileTransfer.FileMessage.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      private static Builder create() {
        return new Builder();
      }

      public Builder clear() {
        super.clear();
        deviceId_ = "";
        bitField0_ = (bitField0_ & ~0x00000001);
        unixTime_ = 0;
        bitField0_ = (bitField0_ & ~0x00000002);
        data_ = com.google.protobuf.ByteString.EMPTY;
        bitField0_ = (bitField0_ & ~0x00000004);
        position_ = 0L;
        bitField0_ = (bitField0_ & ~0x00000008);
        fileid_ = "";
        bitField0_ = (bitField0_ & ~0x00000010);
        return this;
      }

      public Builder clone() {
        return create().mergeFrom(buildPartial());
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return com.hello.suripu.api.audio.FileTransfer.internal_static_FileMessage_descriptor;
      }

      public com.hello.suripu.api.audio.FileTransfer.FileMessage getDefaultInstanceForType() {
        return com.hello.suripu.api.audio.FileTransfer.FileMessage.getDefaultInstance();
      }

      public com.hello.suripu.api.audio.FileTransfer.FileMessage build() {
        com.hello.suripu.api.audio.FileTransfer.FileMessage result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public com.hello.suripu.api.audio.FileTransfer.FileMessage buildPartial() {
        com.hello.suripu.api.audio.FileTransfer.FileMessage result = new com.hello.suripu.api.audio.FileTransfer.FileMessage(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        if (((from_bitField0_ & 0x00000001) == 0x00000001)) {
          to_bitField0_ |= 0x00000001;
        }
        result.deviceId_ = deviceId_;
        if (((from_bitField0_ & 0x00000002) == 0x00000002)) {
          to_bitField0_ |= 0x00000002;
        }
        result.unixTime_ = unixTime_;
        if (((from_bitField0_ & 0x00000004) == 0x00000004)) {
          to_bitField0_ |= 0x00000004;
        }
        result.data_ = data_;
        if (((from_bitField0_ & 0x00000008) == 0x00000008)) {
          to_bitField0_ |= 0x00000008;
        }
        result.position_ = position_;
        if (((from_bitField0_ & 0x00000010) == 0x00000010)) {
          to_bitField0_ |= 0x00000010;
        }
        result.fileid_ = fileid_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.hello.suripu.api.audio.FileTransfer.FileMessage) {
          return mergeFrom((com.hello.suripu.api.audio.FileTransfer.FileMessage)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.hello.suripu.api.audio.FileTransfer.FileMessage other) {
        if (other == com.hello.suripu.api.audio.FileTransfer.FileMessage.getDefaultInstance()) return this;
        if (other.hasDeviceId()) {
          bitField0_ |= 0x00000001;
          deviceId_ = other.deviceId_;
          onChanged();
        }
        if (other.hasUnixTime()) {
          setUnixTime(other.getUnixTime());
        }
        if (other.hasData()) {
          setData(other.getData());
        }
        if (other.hasPosition()) {
          setPosition(other.getPosition());
        }
        if (other.hasFileid()) {
          bitField0_ |= 0x00000010;
          fileid_ = other.fileid_;
          onChanged();
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.hello.suripu.api.audio.FileTransfer.FileMessage parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.hello.suripu.api.audio.FileTransfer.FileMessage) e.getUnfinishedMessage();
          throw e;
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      // optional string device_id = 1;
      private java.lang.Object deviceId_ = "";
      /**
       * <code>optional string device_id = 1;</code>
       *
       * <pre>
       *device mac address (really device ID)
       * </pre>
       */
      public boolean hasDeviceId() {
        return ((bitField0_ & 0x00000001) == 0x00000001);
      }
      /**
       * <code>optional string device_id = 1;</code>
       *
       * <pre>
       *device mac address (really device ID)
       * </pre>
       */
      public java.lang.String getDeviceId() {
        java.lang.Object ref = deviceId_;
        if (!(ref instanceof java.lang.String)) {
          java.lang.String s = ((com.google.protobuf.ByteString) ref)
              .toStringUtf8();
          deviceId_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>optional string device_id = 1;</code>
       *
       * <pre>
       *device mac address (really device ID)
       * </pre>
       */
      public com.google.protobuf.ByteString
          getDeviceIdBytes() {
        java.lang.Object ref = deviceId_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          deviceId_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>optional string device_id = 1;</code>
       *
       * <pre>
       *device mac address (really device ID)
       * </pre>
       */
      public Builder setDeviceId(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        deviceId_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional string device_id = 1;</code>
       *
       * <pre>
       *device mac address (really device ID)
       * </pre>
       */
      public Builder clearDeviceId() {
        bitField0_ = (bitField0_ & ~0x00000001);
        deviceId_ = getDefaultInstance().getDeviceId();
        onChanged();
        return this;
      }
      /**
       * <code>optional string device_id = 1;</code>
       *
       * <pre>
       *device mac address (really device ID)
       * </pre>
       */
      public Builder setDeviceIdBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000001;
        deviceId_ = value;
        onChanged();
        return this;
      }

      // optional int32 unix_time = 2;
      private int unixTime_ ;
      /**
       * <code>optional int32 unix_time = 2;</code>
       *
       * <pre>
       *time in which transfer took place
       * </pre>
       */
      public boolean hasUnixTime() {
        return ((bitField0_ & 0x00000002) == 0x00000002);
      }
      /**
       * <code>optional int32 unix_time = 2;</code>
       *
       * <pre>
       *time in which transfer took place
       * </pre>
       */
      public int getUnixTime() {
        return unixTime_;
      }
      /**
       * <code>optional int32 unix_time = 2;</code>
       *
       * <pre>
       *time in which transfer took place
       * </pre>
       */
      public Builder setUnixTime(int value) {
        bitField0_ |= 0x00000002;
        unixTime_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional int32 unix_time = 2;</code>
       *
       * <pre>
       *time in which transfer took place
       * </pre>
       */
      public Builder clearUnixTime() {
        bitField0_ = (bitField0_ & ~0x00000002);
        unixTime_ = 0;
        onChanged();
        return this;
      }

      // optional bytes data = 3;
      private com.google.protobuf.ByteString data_ = com.google.protobuf.ByteString.EMPTY;
      /**
       * <code>optional bytes data = 3;</code>
       *
       * <pre>
       *file data
       * </pre>
       */
      public boolean hasData() {
        return ((bitField0_ & 0x00000004) == 0x00000004);
      }
      /**
       * <code>optional bytes data = 3;</code>
       *
       * <pre>
       *file data
       * </pre>
       */
      public com.google.protobuf.ByteString getData() {
        return data_;
      }
      /**
       * <code>optional bytes data = 3;</code>
       *
       * <pre>
       *file data
       * </pre>
       */
      public Builder setData(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000004;
        data_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional bytes data = 3;</code>
       *
       * <pre>
       *file data
       * </pre>
       */
      public Builder clearData() {
        bitField0_ = (bitField0_ & ~0x00000004);
        data_ = getDefaultInstance().getData();
        onChanged();
        return this;
      }

      // optional int64 position = 4;
      private long position_ ;
      /**
       * <code>optional int64 position = 4;</code>
       *
       * <pre>
       *position in which you wish to start in the file.  use this to resume a file transfer
       * </pre>
       */
      public boolean hasPosition() {
        return ((bitField0_ & 0x00000008) == 0x00000008);
      }
      /**
       * <code>optional int64 position = 4;</code>
       *
       * <pre>
       *position in which you wish to start in the file.  use this to resume a file transfer
       * </pre>
       */
      public long getPosition() {
        return position_;
      }
      /**
       * <code>optional int64 position = 4;</code>
       *
       * <pre>
       *position in which you wish to start in the file.  use this to resume a file transfer
       * </pre>
       */
      public Builder setPosition(long value) {
        bitField0_ |= 0x00000008;
        position_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional int64 position = 4;</code>
       *
       * <pre>
       *position in which you wish to start in the file.  use this to resume a file transfer
       * </pre>
       */
      public Builder clearPosition() {
        bitField0_ = (bitField0_ & ~0x00000008);
        position_ = 0L;
        onChanged();
        return this;
      }

      // optional string fileid = 5;
      private java.lang.Object fileid_ = "";
      /**
       * <code>optional string fileid = 5;</code>
       *
       * <pre>
       *some sort of unique identifier of the file (name + device + time or something)
       * </pre>
       */
      public boolean hasFileid() {
        return ((bitField0_ & 0x00000010) == 0x00000010);
      }
      /**
       * <code>optional string fileid = 5;</code>
       *
       * <pre>
       *some sort of unique identifier of the file (name + device + time or something)
       * </pre>
       */
      public java.lang.String getFileid() {
        java.lang.Object ref = fileid_;
        if (!(ref instanceof java.lang.String)) {
          java.lang.String s = ((com.google.protobuf.ByteString) ref)
              .toStringUtf8();
          fileid_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }
      /**
       * <code>optional string fileid = 5;</code>
       *
       * <pre>
       *some sort of unique identifier of the file (name + device + time or something)
       * </pre>
       */
      public com.google.protobuf.ByteString
          getFileidBytes() {
        java.lang.Object ref = fileid_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b = 
              com.google.protobuf.ByteString.copyFromUtf8(
                  (java.lang.String) ref);
          fileid_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }
      /**
       * <code>optional string fileid = 5;</code>
       *
       * <pre>
       *some sort of unique identifier of the file (name + device + time or something)
       * </pre>
       */
      public Builder setFileid(
          java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000010;
        fileid_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional string fileid = 5;</code>
       *
       * <pre>
       *some sort of unique identifier of the file (name + device + time or something)
       * </pre>
       */
      public Builder clearFileid() {
        bitField0_ = (bitField0_ & ~0x00000010);
        fileid_ = getDefaultInstance().getFileid();
        onChanged();
        return this;
      }
      /**
       * <code>optional string fileid = 5;</code>
       *
       * <pre>
       *some sort of unique identifier of the file (name + device + time or something)
       * </pre>
       */
      public Builder setFileidBytes(
          com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  bitField0_ |= 0x00000010;
        fileid_ = value;
        onChanged();
        return this;
      }

      // @@protoc_insertion_point(builder_scope:FileMessage)
    }

    static {
      defaultInstance = new FileMessage(true);
      defaultInstance.initFields();
    }

    // @@protoc_insertion_point(class_scope:FileMessage)
  }

  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_FileMessage_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_FileMessage_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\022filetransfer.proto\"c\n\013FileMessage\022\021\n\td" +
      "evice_id\030\001 \001(\t\022\021\n\tunix_time\030\002 \001(\005\022\014\n\004dat" +
      "a\030\003 \001(\014\022\020\n\010position\030\004 \001(\003\022\016\n\006fileid\030\005 \001(" +
      "\tB*\n\032com.hello.suripu.api.audioB\014FileTra" +
      "nsfer"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_FileMessage_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_FileMessage_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_FileMessage_descriptor,
              new java.lang.String[] { "DeviceId", "UnixTime", "Data", "Position", "Fileid", });
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
  }

  // @@protoc_insertion_point(outer_class_scope)
}
