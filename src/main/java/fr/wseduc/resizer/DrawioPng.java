package fr.wseduc.resizer;

import java.nio.charset.StandardCharsets;

/**
 * Open ENT : reconnaît un schéma draw.io enregistré par l'éditeur riche — un PNG dont un chunk
 * texte (tEXt, zTXt, iTXt) de mot-clé {@code mxfile} contient le schéma source. Le réencoder
 * (compression à l'envoi dans le workspace) supprimerait ce chunk : le schéma ne serait plus
 * modifiable.
 */
public final class DrawioPng {

	private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
	/** {@code mxGraphModel} : anciennes versions de draw.io. */
	private static final String[] KEYWORDS = {"mxfile", "mxGraphModel"};

	private DrawioPng() {
	}

	public static boolean carriesDiagram(byte[] data) {
		if (data == null || data.length < SIGNATURE.length) {
			return false;
		}
		for (int i = 0; i < SIGNATURE.length; i++) {
			if (data[i] != SIGNATURE[i]) {
				return false;
			}
		}
		int offset = SIGNATURE.length;
		while (offset + 8 <= data.length) {
			final long length = ((data[offset] & 0xffL) << 24) | ((data[offset + 1] & 0xff) << 16)
					| ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff);
			final String type = new String(data, offset + 4, 4, StandardCharsets.ISO_8859_1);
			final int start = offset + 8;
			if ("tEXt".equals(type) || "zTXt".equals(type) || "iTXt".equals(type)) {
				final int end = (int) Math.min(data.length, start + length);
				int zero = start;
				while (zero < end && data[zero] != 0) {
					zero++;
				}
				if (zero < end) {
					final String keyword = new String(data, start, zero - start, StandardCharsets.ISO_8859_1);
					for (String expected : KEYWORDS) {
						if (expected.equals(keyword)) {
							return true;
						}
					}
				}
			}
			if ("IEND".equals(type) || length > Integer.MAX_VALUE) {
				break;
			}
			offset = (int) (start + length + 4);
		}
		return false;
	}
}
