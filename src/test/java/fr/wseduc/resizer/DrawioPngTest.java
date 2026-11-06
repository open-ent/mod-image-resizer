package fr.wseduc.resizer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

/** Un schéma draw.io (PNG portant un chunk texte {@code mxfile}) ne doit pas être réencodé. */
public class DrawioPngTest {

	private static byte[] chunk(String type, byte[] data) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(data.length >>> 24);
		out.write(data.length >>> 16);
		out.write(data.length >>> 8);
		out.write(data.length);
		out.writeBytes(type.getBytes(StandardCharsets.ISO_8859_1));
		out.writeBytes(data);
		out.writeBytes(new byte[4]);
		return out.toByteArray();
	}

	private static byte[] text(String keyword, String value) {
		return (keyword + "\0" + value).getBytes(StandardCharsets.ISO_8859_1);
	}

	private static byte[] png(byte[]... chunks) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
		out.writeBytes(chunk("IHDR", new byte[13]));
		for (byte[] c : chunks) {
			out.writeBytes(c);
		}
		out.writeBytes(chunk("IDAT", new byte[] {1, 2, 3}));
		out.writeBytes(chunk("IEND", new byte[0]));
		return out.toByteArray();
	}

	@Test
	public void recognisesDrawioChunks() {
		assertTrue(DrawioPng.carriesDiagram(png(chunk("tEXt", text("mxfile", "%3Cmxfile%3E")))));
		assertTrue(DrawioPng.carriesDiagram(png(chunk("zTXt", text("mxGraphModel", "x")))));
	}

	@Test
	public void ignoresOrdinaryImages() {
		assertFalse(DrawioPng.carriesDiagram(png()));
		assertFalse(DrawioPng.carriesDiagram(png(chunk("tEXt", text("Software", "GIMP")))));
		assertFalse(DrawioPng.carriesDiagram(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff}));
		assertFalse(DrawioPng.carriesDiagram(null));
	}

	@Test
	public void survivesTruncatedFiles() {
		final byte[] full = png(chunk("tEXt", text("mxfile", "x")));
		assertFalse(DrawioPng.carriesDiagram(Arrays.copyOf(full, 30)));
	}
}
