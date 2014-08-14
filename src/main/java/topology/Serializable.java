package topology;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.opencv_core;

import java.nio.ByteBuffer;

/**
 * Created by Intern04 on 4/8/2014.
 */
public class Serializable {

    /* Fields of serializable class cannot be final. Use carefully. */
    public static class Mat implements KryoSerializable {
        private byte[] data;
        private int rows, cols, type;

        public byte[] getData() {
            return data;
        }

        public int getRows() {
            return rows;
        }

        public int getCols() {
            return cols;
        }

        public int getType() {
            return type;
        }

        public Mat(int rows, int cols, int type, byte [] data) {
            this.rows = rows;
            this.cols = cols;
            this.type = type;
            this.data = data;
        }
        public Mat(opencv_core.Mat mat) {
            if (!mat.isContinuous())
                mat = mat.clone();
            if (!mat.isContinuous())
                throw new IllegalArgumentException("Matrix is not continuous");

            rows = mat.rows();
            cols = mat.cols();
            type = mat.type();
            int size = mat.arraySize();

            ByteBuffer bb = mat.getByteBuffer();
            bb.rewind();
            data = new byte[size];
            while (bb.hasRemaining())  // should happen only once
                bb.get(data);
        }

        public opencv_core.Mat toJavaCVMat() {
            return new opencv_core.Mat(rows, cols, type, new BytePointer(data));
        }

     @Override
     public void write(Kryo kryo, Output output) {
         output.write(rows);
         output.write(cols);
         output.write(type);
         output.write(data);
     }

     @Override
     public void read(Kryo kryo, Input input) {
         rows = input.readInt();
         cols = input.readInt();
         type = input.readInt();
         input.read(data);
     }
 }


    public static class Rect implements KryoSerializable {
        public int x, y, width, height;
        public Rect(opencv_core.Rect rect) {
            x = rect.x();
            y = rect.y();
            width = rect.width();
            height = rect.height();
        }
        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.height = height;
            this.width = width;
        }
        public opencv_core.Rect toJavaCVRect() {
            return new opencv_core.Rect(x, y, width, height);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Rect rect = (Rect) o;

            if (height != rect.height) return false;
            if (width != rect.width) return false;
            if (x != rect.x) return false;
            if (y != rect.y) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + width;
            result = 31 * result + height;
            return result;
        }

        @Override
        public void write(Kryo kryo, Output output) {
            output.write(x);
            output.write(y);
            output.write(width);
            output.write(height);
        }

        @Override
        public void read(Kryo kryo, Input input) {
            x = input.readInt();
            y = input.readInt();
            width = input.readInt();
            height = input.readInt();
        }
    }

    /* This is a class which identifies each patch*/
    public static class PatchIdentifier implements  KryoSerializable {
        public int frameId;
        public Rect roi;
        public PatchIdentifier(int frameId, Rect roi) {
            this.roi = roi;
            this.frameId = frameId;
        }

        @Override
        public void write(Kryo kryo, Output output) {
            output.writeInt(frameId);
            output.writeInt(roi.x);
            output.writeInt(roi.y);
            output.writeInt(roi.width);
            output.writeInt(roi.height);
        }

        @Override
        public void read(Kryo kryo, Input input) {
            frameId = input.readInt();
            int     x = input.readInt(),
                    y = input.readInt(),
                    width = input.readInt(),
                    height = input.readInt();
            roi = new Rect(x, y, width, height);
        }
        public String toString() {
            if (roi != null)
                return String.format("N%04d@%04d@%04d@%04d@%04d", frameId, roi.x, roi.y, roi.x + roi.width, roi.y + roi.height);
            return String.format("N%04d@null", frameId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PatchIdentifier that = (PatchIdentifier) o;

            if (frameId != that.frameId) return false;
            if (roi != null ? !roi.equals(that.roi) : that.roi != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = frameId;
            result = 31 * result + (roi != null ? roi.hashCode() : 0);
            return result;
        }
    }
}
