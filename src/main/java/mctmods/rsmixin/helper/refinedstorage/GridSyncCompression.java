package mctmods.rsmixin.helper.refinedstorage;

import mctmods.rsmixin.Config;
import mctmods.rsmixin.RSMixin;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class GridSyncCompression {
    private static final Logger LOGGER = LogManager.getLogger(RSMixin.MODID);
    private static final int MARKER = -1;
    private static final int THRESHOLD = 950 * 1024;
    private static final int PAYLOAD_LIMIT = 1024 * 1024;
    private static final int MAX_INFLATED = 32 * 1024 * 1024;

    private GridSyncCompression() {}

    public static void writePayload(ByteBuf out, Consumer<ByteBuf> payloadWriter) {
        ByteBuf temp = Unpooled.buffer();
        payloadWriter.accept(temp);
        if (temp.readableBytes() <= THRESHOLD) {
            out.writeBytes(temp);
            return;
        }

        byte[] raw = new byte[temp.readableBytes()];
        temp.readBytes(raw);

        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(raw);
        deflater.finish();
        ByteBuf compressed = Unpooled.buffer(raw.length / 4);
        byte[] chunk = new byte[8192];
        while (!deflater.finished()) { compressed.writeBytes(chunk, 0, deflater.deflate(chunk)); }
        deflater.end();

        out.writeInt(MARKER);
        out.writeInt(raw.length);
        out.writeInt(compressed.readableBytes());
        out.writeBytes(compressed);

        if (compressed.readableBytes() > PAYLOAD_LIMIT) { LOGGER.warn("RSMixin: Grid sync payload is {} bytes even after compression ({} bytes raw); the client will likely be disconnected", compressed.readableBytes(), raw.length); }
        else if (Config.enableDebugLogging) { LOGGER.debug("RSMixin: Compressed grid sync payload from {} to {} bytes", raw.length, compressed.readableBytes()); }
    }

    public static ByteBuf inflateMessage(ByteBuf buf) {
        if (buf.readableBytes() < 5 || buf.getInt(buf.readerIndex() + 1) != MARKER) { return buf; }

        boolean canCraft = buf.readBoolean();
        buf.skipBytes(4);
        int rawLength = buf.readInt();
        int compressedLength = buf.readInt();
        if (rawLength < 0 || rawLength > MAX_INFLATED || compressedLength < 0 || compressedLength > buf.readableBytes()) {
            LOGGER.warn("RSMixin: Ignoring a malformed compressed grid sync payload ({} raw, {} compressed, {} available)", rawLength, compressedLength, buf.readableBytes());
            ByteBuf empty = Unpooled.buffer(5);
            empty.writeBoolean(canCraft);
            empty.writeInt(0);
            return empty;
        }

        byte[] compressed = new byte[compressedLength];
        buf.readBytes(compressed);
        byte[] raw = new byte[rawLength];

        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        try {
            int written = 0;
            while (written < rawLength && !inflater.finished()) { written += inflater.inflate(raw, written, rawLength - written); }
        }
        catch (DataFormatException e) {
            LOGGER.warn("RSMixin: Failed to decompress a grid sync payload", e);
            ByteBuf empty = Unpooled.buffer(5);
            empty.writeBoolean(canCraft);
            empty.writeInt(0);
            return empty;
        }
        finally { inflater.end(); }

        ByteBuf out = Unpooled.buffer(rawLength + 1);
        out.writeBoolean(canCraft);
        out.writeBytes(raw);
        return out;
    }
}
