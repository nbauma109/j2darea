package j2darea;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class ExportableArea implements Externalizable {

    private ExportableImage backgroundImage;
    private List<PastedObject> pastedObjects;

    public ExportableArea() {
    }

    public ExportableArea(ExportableImage backgroundImage, List<PastedObject> pastedObjects) {
        this.backgroundImage = backgroundImage;
        this.pastedObjects = pastedObjects;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        backgroundImage.writeExternal(out);
        out.writeInt(pastedObjects.size());
        for (PastedObject pastedObject : pastedObjects) {
            pastedObject.writeExternal(out);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        backgroundImage = new ExportableImage();
        backgroundImage.readExternal(in);
        int objectCount = in.readInt();
        pastedObjects = new ArrayList<>(objectCount);
        for (int i = 0; i < objectCount; i++) {
            PastedObject pastedObject = new PastedObject();
            pastedObject.readExternal(in);
            pastedObjects.add(pastedObject);
        }
    }

    public ExportableImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(ExportableImage backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public List<PastedObject> getPastedObjects() {
        return pastedObjects;
    }

    public void setPastedObjects(List<PastedObject> pastedObjects) {
        this.pastedObjects = pastedObjects;
    }

}
