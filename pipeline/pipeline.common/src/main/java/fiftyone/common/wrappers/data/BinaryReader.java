/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.common.wrappers.data;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/** Reads little-endian values from a byte buffer. */
public class BinaryReader implements Closeable {

    private final FileChannel channel;
    private ByteBuffer byteBuffer;

    /**
     * Create a reader over the whole file, mapped into memory.
     *
     * @param fileInputStream the file to read
     * @throws IOException if the file could not be mapped
     */
    public BinaryReader(FileInputStream fileInputStream) throws IOException {
        channel = fileInputStream.getChannel();
        byteBuffer = channel.map(
            MapMode.READ_ONLY,
            0,
            channel.size());
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Create a reader over the buffer given.
     *
     * @param byteBuffer the bytes to read
     */
    public BinaryReader(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
        this.byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        channel = null;
    }

    /**
     * The number of bytes the reader holds.
     *
     * @return the size in bytes
     */
    public long getSize() {
        return byteBuffer.limit();
    }

    /**
     * The buffer the reader reads from.
     *
     * @return the buffer
     */
    public ByteBuffer getBuffer() {
        return byteBuffer;
    }

    /**
     * The position of the next byte to read.
     *
     * @return the position
     */
    public int getPosition() {
        return byteBuffer.position();
    }

    /**
     * Move to a new position.
     *
     * @param position the position of the next byte to read
     */
    public void setPosition(int position) {
        byteBuffer.position(position);
    }

    /**
     * Read a 4 byte float.
     *
     * @return the value
     */
    public float readFloat() {
        return byteBuffer.getFloat();
    }

    /**
     * Read a 2 byte integer.
     *
     * @return the value
     */
    public short readInt16() {
        return byteBuffer.getShort();
    }

    /**
     * Read a 4 byte integer.
     *
     * @return the value
     */
    public int readInt32() {
        return byteBuffer.getInt();
    }

    /**
     * Read an 8 byte integer.
     *
     * @return the value
     */
    public long readInt64() {
        return byteBuffer.getLong();
    }

    /**
     * Read bytes as characters up to the next line feed, dropping any carriage
     * return.
     *
     * @return the line, or null at the end of the buffer
     */
    public String readLine() {
        char c;
        boolean read = false;
        StringBuilder result = new StringBuilder();
        while (getPosition() < getSize() && (c = (char)byteBuffer.get()) != '\n') {
            if (read == false) {
                read = true;
            }
            if (c != '\r') {
                result.append(c);
            }
        }
        return read == true ? result.toString() : null;
    }

    /**
     * Read the number of bytes given.
     *
     * @param length the number of bytes
     * @return the bytes
     */
    public byte[] readBytes(final int length) {
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        return bytes;
    }

    /**
     * Copy every byte the reader holds, from the start, into a new array.
     *
     * @return the bytes
     */
    public byte[] toByteArray() {
        ByteBuffer result = ByteBuffer.allocate((int) getSize());
        // https://www.morling.dev/blog/bytebuffer-and-the-dreaded-nosuchmethoderror/
        ((java.nio.Buffer)byteBuffer).position(0);
        result.put(byteBuffer);
        return result.array();
    }

    /**
     * Write every byte the reader holds to a writer.
     *
     * @param writer where the bytes are written
     * @throws IOException if the writer fails
     */
    public void copyTo(BinaryWriter writer) throws IOException {
        writer.writeBytes(toByteArray());
    }

    /**
     * Read one byte.
     *
     * @return the value
     */
    public byte readByte() {
        return byteBuffer.get();
    }

    /**
     * Set the byte buffer to null to prevent any further access to the under
     * lying data. This should be done before the channel is closed as the
     * byte buffer could be tied to the channel. Any subsequent access to the
     * methods will fail with a null object exception.
     */
    @Override
    public void close() {
        byteBuffer = null;
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ex) {
                // Do nothing.
            }
        }
    }

}
