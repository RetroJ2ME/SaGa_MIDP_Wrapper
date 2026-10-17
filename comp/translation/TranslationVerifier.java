/*
 * Copyright (C) 2026 Magstic
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package translation;

import doja.tools.io.FileIO;
import doja.tools.translation.ClassText;
import doja.tools.translation.ResolvedText;
import doja.tools.translation.ResolvedTextTable;
import doja.tools.translation.RawTranslationResource;
import doja.tools.translation.TranslationTable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Verifies translated class constants and the optional Runtime translation dictionary. */
public final class TranslationVerifier {
    private TranslationVerifier() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: TranslationVerifier <raw.jar> <resolved.tsv> <config.properties> <generated-assets>");
        }
        File jarFile = new File(args[0]);
        List<ResolvedText> resolved = ResolvedTextTable.read(new File(args[1]));
        File config = new File(args[2]);
        File translationFile = new File(config.getParentFile(), "Translation.tsv");
        TranslationTable translations = TranslationTable.read(translationFile);
        List<TranslationSupport.DataText> dataText = TranslationSupport.extractDataText(new File(args[3]));
        Map<String,String> expectedRuntime = TranslationSupport.expectedRuntimeTranslations(translations, dataText);

        ZipFile jar = new ZipFile(jarFile);
        try {
            Map<String,byte[]> classes = new HashMap<String,byte[]>();
            for (int i = 0; i < resolved.size(); i++) {
                ResolvedText entry = resolved.get(i);
                if (!"class".equals(entry.kind)) throw new IOException("unknown resolved kind " + entry.kind);
                byte[] bytes = classes.get(entry.file);
                if (bytes == null) {
                    ZipEntry zipEntry = jar.getEntry(entry.file);
                    if (zipEntry == null) throw new IOException("final JAR has no " + entry.file);
                    bytes = readAll(jar, zipEntry);
                    classes.put(entry.file, bytes);
                }
                ClassText.verify(bytes, entry.location, entry.text, entry.file + ":" + entry.location);
            }

            ZipEntry dictionary = jar.getEntry("assets/translation.bin");
            if (expectedRuntime.isEmpty()) {
                if (dictionary != null) throw new IOException("unexpected assets/translation.bin with no translations");
            } else {
                if (dictionary == null) throw new IOException("final JAR has no assets/translation.bin");
                InputStream input = jar.getInputStream(dictionary);
                Map<String,String> actual;
                try {
                    actual = RawTranslationResource.read(input);
                } finally {
                    input.close();
                }
                if (!expectedRuntime.equals(actual)) throw new IOException("Runtime translation dictionary mismatch");
            }
            System.out.println("TranslationVerifier: verified " + resolved.size() + " class occurrence(s), "
                    + expectedRuntime.size() + " Runtime mapping(s)");
        } finally {
            jar.close();
        }
    }

    private static byte[] readAll(ZipFile zip, ZipEntry entry) throws IOException {
        InputStream input = zip.getInputStream(entry);
        try {
            return FileIO.read(input);
        } finally {
            input.close();
        }
    }
}
