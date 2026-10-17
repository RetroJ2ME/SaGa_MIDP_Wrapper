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

import doja.tools.translation.TextIndex;
import doja.tools.translation.TextOccurrence;
import doja.tools.translation.TranslationTable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Exports Makai Toushi SaGa translatable class, TableData and script text. */
public final class TranslationPrepare {
    private TranslationPrepare() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                    "Usage: TranslationPrepare <game.jar> <generated-assets> <config.properties> <Index.tsv> <Translation.tsv>");
        }
        File jar = new File(args[0]);
        File assets = new File(args[1]);
        File config = new File(args[2]);
        File index = new File(args[3]);
        File translations = new File(args[4]);
        if (!config.isFile()) throw new IllegalArgumentException("missing " + config);

        List<TextOccurrence> classText = TranslationSupport.extractClassText(jar);
        List<TranslationSupport.DataText> dataText = TranslationSupport.extractDataText(assets);
        List<TextIndex.Entry> indexEntries = new ArrayList<TextIndex.Entry>();
        List<String> occurrenceSources = new ArrayList<String>();

        int classId = 0;
        int tableId = 0;
        int scriptId = 0;
        for (int i = 0; i < classText.size(); i++) {
            TextOccurrence entry = classText.get(i);
            indexEntries.add(new TextIndex.Entry(format('C', ++classId), "JAR",
                    entry.file + ":#" + entry.location, entry.source));
            occurrenceSources.add(entry.source);
        }
        for (int i = 0; i < dataText.size(); i++) {
            TranslationSupport.DataText entry = dataText.get(i);
            char prefix = "TABLE".equals(entry.sourceKind) ? 'T' : 'S';
            int id = prefix == 'T' ? ++tableId : ++scriptId;
            indexEntries.add(new TextIndex.Entry(format(prefix, id), entry.sourceKind,
                    entry.location, entry.text));
            occurrenceSources.add(entry.text);
        }
        TextIndex.write(index, indexEntries);

        Set<String> seen = new LinkedHashSet<String>();
        List<String> unique = new ArrayList<String>();
        for (int i = 0; i < occurrenceSources.size(); i++) {
            String source = occurrenceSources.get(i);
            if (seen.add(source)) unique.add(source);
        }
        TranslationTable table = TranslationTable.read(translations);
        table.write(translations, unique);
        System.out.println("TranslationPrepare: class=" + classText.size() + ", data=" + dataText.size()
                + ", unique=" + unique.size() + "; blank translations fall back to original text");
    }

    private static String format(char prefix, int value) {
        String text = String.valueOf(value);
        while (text.length() < 4) text = "0" + text;
        return prefix + text;
    }
}
