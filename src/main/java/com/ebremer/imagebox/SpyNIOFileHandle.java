package com.ebremer.imagebox;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import loci.common.AbstractNIOHandle;
import loci.common.Constants;
import loci.common.NIOByteBufferProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpyNIOFileHandle extends AbstractNIOHandle {

  // -- Constants --

  /** Logger for this class. */
  private static final Logger LOGGER =
    LoggerFactory.getLogger(SpyNIOFileHandle.class);

  //-- Static fields --

  /** Default NIO buffer size to facilitate buffered I/O. */
  protected static int defaultBufferSize = 1048576;

  /**
   * Default NIO buffer size to facilitate buffered I/O for read/write streams.
   */
  protected static int defaultRWBufferSize = 8192;

  // -- Fields --

  /** The random access file object backing this FileHandle. */
  protected RandomAccessFile raf;

  /** The file channel backed by the random access file. */
  protected FileChannel channel;

  /** The absolute position within the file. */
  protected long pos = 0;

  /** The absolute position of the start of the buffer. */
  protected long bufferStartPosition = 0;

  /** The buffer size. */
  protected int bufferSize;

  /** The buffer itself. */
  protected ByteBuffer buffer;

  /** Whether or not the file is opened read/write. */
  protected boolean isReadWrite = false;

  /** The default map mode for the file. */
  protected FileChannel.MapMode mapMode = FileChannel.MapMode.READ_ONLY;

  /** The buffer's byte ordering. */
  protected ByteOrder order;

  /** Provider class for NIO byte buffers, allocated or memory mapped. */
  protected NIOByteBufferProvider byteBufferProvider;

  /** The original length of the file. */
  private Long defaultLength;

  // -- Constructors --

  /**
   * Creates a random access file stream to read from, and
   * optionally to write to, the file specified by the File argument.
   */
  public SpyNIOFileHandle(File file, String mode, int bufferSize)
    throws IOException
  {
    this.bufferSize = bufferSize;
    validateMode(mode);
    if (mode.equals("rw")) {
      isReadWrite = true;
      mapMode = FileChannel.MapMode.READ_WRITE;
    }
    raf = new RandomAccessFile(file, mode);
    channel = raf.getChannel();
    byteBufferProvider = new NIOByteBufferProvider(channel, mapMode);
    buffer(pos, 0);

    // if we know the length won't change, cache the original length
    if (mode.equals("r")) {
      defaultLength = raf.length();
    }
  }

  /**
   * Creates a random access file stream to read from, and
   * optionally to write to, the file specified by the File argument.
   */
  public SpyNIOFileHandle(File file, String mode) throws IOException {
    this(file, mode,
      mode.equals("rw") ? defaultRWBufferSize : defaultBufferSize);
  }

  /**
   * Creates a random access file stream to read from, and
   * optionally to write to, a file with the specified name.
   */
  public SpyNIOFileHandle(String name, String mode) throws IOException {
    this(new File(name), mode);
  }

  public static void setDefaultBufferSize(int size) {
      System.out.println("setDefaultBufferSize");
    defaultBufferSize = size;
  }

  /**
   * Set the default buffer size for read/write files.
   *
   * Subsequent uses of the NIOFileHandle(String, String) and
   * NIOFileHandle(File, String) constructors will use this buffer size.
   */
  public static void setDefaultReadWriteBufferSize(int size) {
      System.out.println("setDefaultReadWriteBufferSize");
    defaultRWBufferSize = size;
  }

  // -- FileHandle and Channel API methods --

  /** Gets the random access file object backing this FileHandle. */
  public RandomAccessFile getRandomAccessFile() {
      System.out.println("getRandomAccessFile");
    return raf;
  }

  /** Gets the FileChannel from this FileHandle. */
  public FileChannel getFileChannel() {
    try {
      channel.position(pos);
    }
    catch (IOException e) {
      LOGGER.warn("FileChannel.position failed", e);
    }
    return channel;
  }

  /** Gets the current buffer size. */
  public int getBufferSize() {
    return bufferSize;
  }

  // -- AbstractNIOHandle API methods --

  /* @see AbstractNIOHandle.setLength(long) */
  @Override
  public void setLength(long length) throws IOException {
    if (raf.length() < length) {
      raf.setLength(length);
    }
    raf.seek(length - 1);
    buffer = null;
  }

  // -- IRandomAccess API methods --

  /* @see IRandomAccess.close() */
  @Override
  public void close() throws IOException {
    raf.close();
  }

  /* @see IRandomAccess.getFilePointer() */
  @Override
  public long getFilePointer() {
      System.out.println("getFilePointer("+pos+")");
    return pos;
  }

  /* @see IRandomAccess.length() */
  @Override
  public long length() throws IOException {
      
    if (defaultLength != null) {
        //System.out.println("length()="+defaultLength);
      return defaultLength;
    }
    //System.out.println("length()="+raf.length());
    return raf.length();
  }

  /* @see IRandomAccess.getOrder() */
  @Override
  public ByteOrder getOrder() {
      //System.out.println("getOrder "+order.toString());
      ByteOrder bo = (buffer == null ? order : buffer.order());
    return bo;
  }

  /* @see IRandomAccess.setOrder(ByteOrder) */
  @Override
  public void setOrder(ByteOrder order) {
      //System.out.println("setOrder "+order.toString());
    this.order = order;
    if (buffer != null) {
      buffer.order(order);
    }
  }

  /* @see IRandomAccess.read(byte[]) */
  @Override
  public int read(byte[] b) throws IOException {
      System.out.println("read(byte[] b) "+b.length);
      int len = read(ByteBuffer.wrap(b));
      //byte[] x = b.array();
           //if (b.length == 3082) {
            //System.out.print("buffer : ");
            //for (int p=0;p<b.length;p++) {
              //  System.out.print(" "+Integer.toHexString(b[p]&0xFF));
            //}
            //System.out.println("");
       // }
    //return read(ByteBuffer.wrap(b));
        System.out.println(b.length);
        for (int y=0;y<b.length;y++){
            System.out.print(b[y]+" ");
        }
        System.out.println();
    return len;
  }

  /* @see IRandomAccess.read(byte[], int, int) */
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
      System.out.println("read("+off+"  "+len);
    return read(ByteBuffer.wrap(b), off, len);
  }

  /* @see IRandomAccess.read(ByteBuffer) */
  @Override
  public int read(ByteBuffer buf) throws IOException {
      System.out.println("read(ByteBuffer buf) "+buf.capacity());
    return read(buf, 0, buf.capacity());
  }

  /* @see IRandomAccess.read(ByteBuffer, int, int) */
  @Override
  public int read(ByteBuffer buf, int off, int len) throws IOException {
      System.out.println("read(ByteBuffer buf, int off, int len) "+off+" "+len);
    buf.position(off);
    int realLength = (int) Math.min(len, length() - pos);
    if (realLength < 0) {
      return -1;
    }
    buf.limit(off + realLength);
    buffer(pos, realLength);
    pos += realLength;
    while (buf.hasRemaining()) {
      try {
        buf.put(buffer.get());
      } catch (BufferUnderflowException e) {
        EOFException eof = new EOFException(EOF_ERROR_MSG);
        eof.initCause(e);
        throw eof;
      }
    }
    return realLength;
  }

  /* @see IRandomAccess.seek(long) */
  @Override
  public void seek(long pos) throws IOException {
    System.out.println("seek("+Long.toHexString(pos)+")");
    if (mapMode == FileChannel.MapMode.READ_WRITE && pos > length()) {
      setLength(pos);
    }
    buffer(pos, 0);
  }

  /* @see java.io.DataInput.readBoolean() */
  @Override
  public boolean readBoolean() throws IOException {
      System.out.println("CRAP A4");
    return readByte() == 1;
  }

  /* @see java.io.DataInput.readByte() */
  @Override
  public byte readByte() throws IOException {
      System.out.print("readByte() = ");
    buffer(pos, 1);
    pos += 1;
    Byte b = buffer.get();
    System.out.println(Integer.toHexString(b));
    try {
      return b;  
      //return buffer.get();
    } catch (BufferUnderflowException e) {
      EOFException eof = new EOFException(EOF_ERROR_MSG);
      eof.initCause(e);
      throw eof;
    }
  }

  /* @see java.io.DataInput.readChar() */
  @Override
  public char readChar() throws IOException {
      System.out.println("CRAP A5");
    buffer(pos, 2);
    pos += 2;
    try {
      return buffer.getChar();
    } catch (BufferUnderflowException e) {
      EOFException eof = new EOFException(EOF_ERROR_MSG);
      eof.initCause(e);
      throw eof;
    }
  }

  /* @see java.io.DataInput.readDouble() */
  @Override
  public double readDouble() throws IOException {
      System.out.println("CRAP A6");
    buffer(pos, 8);
    pos += 8;
    try {
      return buffer.getDouble();
    } catch (BufferUnderflowException e) {
      EOFException eof = new EOFException(EOF_ERROR_MSG);
      eof.initCause(e);
      throw eof;
    }
  }

  /* @see java.io.DataInput.readFloat() */
  @Override
  public float readFloat() throws IOException {
      System.out.println("CRAP A7");
    buffer(pos, 4);
    pos += 4;
    try {
      return buffer.getFloat();
    } catch (BufferUnderflowException e) {
      EOFException eof = new EOFException(EOF_ERROR_MSG);
      eof.initCause(e);
      throw eof;
    }
  }

  /* @see java.io.DataInput.readFully(byte[]) */
  @Override
  public void readFully(byte[] b) throws IOException {
      System.out.println("readFully : "+b.length);
    readFully(b, 0, b.length);
  }

  /* @see java.io.DataInput.readFully(byte[], int, int) */
  @Override
  public void readFully(byte[] b, int off, int len) throws IOException {
      System.out.println("readFully "+off+" "+len);
    read(b, off, len);
  }

  /* @see java.io.DataInput.readInt() */
  @Override
  public int readInt() throws IOException {
      System.out.print("readInt() = ");
    buffer(pos, 4);
    pos += 4;
    try {
      int b = buffer.getInt();
      System.out.println(Integer.toHexString(b));
      return b;
    } catch (BufferUnderflowException e) {
      EOFException eof = new EOFException(EOF_ERROR_MSG);
      eof.initCause(e);
      throw eof;
    }
  }

  /* @see java.io.DataInput.readLine() */
  @Override
  public String readLine() throws IOException {
      System.out.println("CRAP A10");
    raf.seek(pos);
    String line = raf.readLine();
    buffer(raf.getFilePointer(), 0);
    return line;
  }

  /* @see java.io.DataInput.readLong() */
  @Override
  public long readLong() throws IOException {
      System.out.println("CRAP A11");
    buffer(pos, 8);
    pos += 8;
    try {
      return buffer.getLong();
    } catch (BufferUnderflowException e) {
      EOFException eof = new EOFException(EOF_ERROR_MSG);
      eof.initCause(e);
      throw eof;
    }
  }

  /* @see java.io.DataInput.readShort() */
  @Override
  public short readShort() throws IOException {
    System.out.print(pos+"  readShort() = ");
    buffer(pos, 2);
    pos += 2;
    try {
      short s = buffer.getShort();
      System.out.println(Integer.toHexString(s));
      return s;
    } catch (BufferUnderflowException e) {
      EOFException eof = new EOFException(EOF_ERROR_MSG);
      eof.initCause(e);
      throw eof;
    }
  }

  /* @see java.io.DataInput.readUnsignedByte() */
  @Override
  public int readUnsignedByte() throws IOException {
      System.out.println("CRAP A12");
    return readByte() & 0xFF;
  }

  /* @see java.io.DataInput.readUnsignedShort() */
  @Override
  public int readUnsignedShort() throws IOException {
      System.out.print("readUnsignedShort() = ");
    int s = readShort()& 0xFFFF;
    //System.out.println(Integer.toHexString(s));
    return s;
  }

  /* @see java.io.DataInput.readUTF() */
  @Override
  public String readUTF() throws IOException {
      System.out.println("CRAP A13");
    raf.seek(pos);
    String utf8 = raf.readUTF();
    buffer(raf.getFilePointer(), 0);
    return utf8;
  }

  /* @see java.io.DataInput.skipBytes(int) */
  @Override
  public int skipBytes(int n) throws IOException {
      System.out.println("skipBytes("+n+")");
    if (n < 1) {
      return 0;
    }
    long oldPosition = pos;
    long newPosition = oldPosition + Math.min(n, length());

    buffer(newPosition, 0);
    return (int) (pos - oldPosition);
  }

  // -- DataOutput API methods --

  /* @see java.io.DataOutput.write(byte[]) */
  @Override
  public void write(byte[] b) throws IOException {
      System.out.println("CRAP W1");
    write(ByteBuffer.wrap(b));
  }

  /* @see java.io.DataOutput.write(byte[], int, int) */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
      System.out.println("CRAP W2");
    write(ByteBuffer.wrap(b), off, len);
  }

  /* @see IRandomAccess.write(ByteBuffer) */
  @Override
  public void write(ByteBuffer buf) throws IOException {
      System.out.println("CRAP W3");
    write(buf, 0, buf.capacity());
  }

  /* @see IRandomAccess.write(ByteBuffer, int, int) */
  @Override
  public void write(ByteBuffer buf, int off, int len) throws IOException {
      System.out.println("CRAP W4");
    writeSetup(len);
    buf.limit(off + len);
    buf.position(off);
    pos += channel.write(buf, pos);
    buffer = null;
  }

  /* @see java.io.DataOutput.write(int b) */
  @Override
  public void write(int b) throws IOException {
      System.out.println("CRAP W5");
    writeByte(b);
  }

  /* @see java.io.DataOutput.writeBoolean(boolean) */
  @Override
  public void writeBoolean(boolean v) throws IOException {
      System.out.println("CRAP W6");
    writeByte(v ? 1 : 0);
  }

  /* @see java.io.DataOutput.writeByte(int) */
  @Override
  public void writeByte(int v) throws IOException {
      System.out.println("CRAP W7");
    writeSetup(1);
    buffer.put((byte) v);
    doWrite(1);
  }

  /* @see java.io.DataOutput.writeBytes(String) */
  @Override
  public void writeBytes(String s) throws IOException {
      System.out.println("CRAP W8");
    write(s.getBytes(Constants.ENCODING));
  }

  /* @see java.io.DataOutput.writeChar(int) */
  @Override
  public void writeChar(int v) throws IOException {
      System.out.println("CRAP W9");
    writeSetup(2);
    buffer.putChar((char) v);
    doWrite(2);
  }

  /* @see java.io.DataOutput.writeChars(String) */
  @Override
  public void writeChars(String s) throws IOException {
      System.out.println("CRAP W10");
    write(s.getBytes("UTF-16BE"));
  }

  /* @see java.io.DataOutput.writeDouble(double) */
  @Override
  public void writeDouble(double v) throws IOException {
      System.out.println("CRAP W11");
    writeSetup(8);
    buffer.putDouble(v);
    doWrite(8);
  }

  /* @see java.io.DataOutput.writeFloat(float) */
  @Override
  public void writeFloat(float v) throws IOException {
      System.out.println("CRAP W12");
    writeSetup(4);
    buffer.putFloat(v);
    doWrite(4);
  }

  /* @see java.io.DataOutput.writeInt(int) */
  @Override
  public void writeInt(int v) throws IOException {
      System.out.println("CRAP W13");
    writeSetup(4);
    buffer.putInt(v);
    doWrite(4);
  }

  /* @see java.io.DataOutput.writeLong(long) */
  @Override
  public void writeLong(long v) throws IOException {
      System.out.println("CRAP W14");
    writeSetup(8);
    buffer.putLong(v);
    doWrite(8);
  }

  /* @see java.io.DataOutput.writeShort(int) */
  @Override
  public void writeShort(int v) throws IOException {
      System.out.println("CRAP W15");
    writeSetup(2);
    buffer.putShort((short) v);
    doWrite(2);
  }

  /* @see java.io.DataOutput.writeUTF(String)  */
  @Override
  public void writeUTF(String str) throws IOException {
      System.out.println("CRAP W16");
    // NB: number of bytes written is greater than the length of the string
    int strlen = str.getBytes(Constants.ENCODING).length + 2;
    writeSetup(strlen);
    raf.seek(pos);
    raf.writeUTF(str);
    pos += strlen;
    buffer = null;
  }

  /**
   * Aligns the NIO buffer, maps it if it is not currently and sets all
   * relevant positions and offsets.
   * @param offset The location within the file to read from.
   * @param size The requested read length.
   * @throws IOException If there is an issue mapping, aligning or allocating
   * the buffer.
   */
  private void buffer(long offset, int size) throws IOException {
      //System.out.println("SUPERBUFFER");
    pos = offset;
    long newPosition = offset + size;
    if (newPosition < bufferStartPosition ||
      newPosition > bufferStartPosition + bufferSize || buffer == null)
    {
      bufferStartPosition = offset;
      if (length() > 0 && length() - 1 < bufferStartPosition) {
        bufferStartPosition = length() - 1;
      }
      long newSize = Math.min(length() - bufferStartPosition, bufferSize);
      if (newSize < size && newSize == bufferSize) newSize = size;
      if (newSize + bufferStartPosition > length()) {
        newSize = length() - bufferStartPosition;
      }
      offset = bufferStartPosition;
      ByteOrder byteOrder = buffer == null ? order : getOrder();
      buffer = byteBufferProvider.allocate(bufferStartPosition, (int) newSize);
      if (byteOrder != null) setOrder(byteOrder);
    }
    buffer.position((int) (offset - bufferStartPosition));
    if (buffer.position() + size > buffer.limit() &&
      mapMode == FileChannel.MapMode.READ_WRITE)
    {
      buffer.limit(buffer.position() + size);
    }
  }

  private void writeSetup(int length) throws IOException {
      System.out.println("CRAP W");
    validateLength(length);
    buffer(pos, length);
  }

  private void doWrite(int length) throws IOException {
      System.out.println("CRAP W");
    buffer.position(buffer.position() - length);
    channel.write(buffer, pos);
    pos += length;
  }

    @Override
    public long skipBytes(long l) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
