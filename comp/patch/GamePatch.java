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
