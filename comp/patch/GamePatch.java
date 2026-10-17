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

import java.io.File;
import java.io.IOException;
import doja.tools.classfile.ClassFile;
import doja.tools.classfile.MethodRefPatch;

/** Makai Toushi SaGa sound-control patches. */
public final class GamePatch {
    private GamePatch() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IOException("Usage: GamePatch <class-dir>");
        File classes = new File(args[0]);
        int next = MethodRefPatch.redirect(classes, "j", "e", "()V",
                "SagaSoundControl", "next");
        int refresh = MethodRefPatch.redirect(classes, "j", "f", "()V",
                "SagaSoundControl", "refresh");
        if (next != 1 || refresh != 1) {
            throw new IOException("SaGa sound-control references changed: j.e=" + next
                    + ", j.f=" + refresh);
        }
        File soundClass = new File(classes, "j.class");
        ClassFile cls = ClassFile.read(soundClass);
        ClassFile.Member mediaAction = cls.findMethod("mediaAction",
                "(Lcom/nttdocomo/ui/MediaPresenter;II)V");
        if (mediaAction == null) throw new IOException("SaGa mediaAction method not found in j.class");
        mediaAction.setAccessFlags(mediaAction.accessFlags() & ~0x0020); // ACC_SYNCHRONIZED
        cls.write(soundClass);
        System.out.println("GamePatch: SaGa sound-control references redirected and mediaAction unlocked");
    }
}
