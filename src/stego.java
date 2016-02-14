import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class Steg {

    private final int byteLength = 8;

    protected final int sizeBitsLength = 32;

    protected final int extBitsLength = 64;

    protected final int headerLenght = 54;

    int msgLengthStartIndex = headerLenght;
    int msgLengthEndIndex = sizeBitsLength + headerLenght;

    int extensionStartIndex = msgLengthEndIndex;
    int extensionEndIndex = extensionStartIndex + extBitsLength;

    public Steg() {

    }

    public String hideString(String payload, String cover_filename) {
        byte[] imageBytesArray = null;
        byte[] payloadBytesArray = new byte[0];
        Path path = Paths.get(cover_filename);
        System.out.println(path);
        try {
            payloadBytesArray = payload.getBytes("US-ASCII");
            imageBytesArray = Files.readAllBytes(path);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "Fail";
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        int availableBytesInPic = imageBytesArray.length;

        // zapisvame duljinata v 1-vite 32 byte-a sled headera
        for (int i = msgLengthStartIndex; i < msgLengthEndIndex; i++) {
            imageBytesArray[i] = (byte) swapLsb(
                    getBit(payloadBytesArray.length, i - msgLengthStartIndex),
                    imageBytesArray[i]);

        }

        // zapisvame bitovete
        BitSet set = BitSet.valueOf(payloadBytesArray);
        int setIndex = 0;
        int setLength = payloadBytesArray.length * byteLength;

        for (int currByteInImage = msgLengthEndIndex; currByteInImage < availableBytesInPic && setIndex < setLength; currByteInImage++) {
            imageBytesArray[currByteInImage] = (byte) swapLsb(
                    set.get(setIndex) ? 1 : 0,
                    imageBytesArray[currByteInImage]);
            setIndex++;
        }

        try {
            FileOutputStream fos = new FileOutputStream(new File("outSI.bmp"));
            fos.write(imageBytesArray);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "outSI.bmp";
    }

    public String extractString(String stego_image) {
        byte[] imageBytesArray = null;
        Path path = Paths.get(stego_image);
        try {
            imageBytesArray = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        int msgLength = 0;
        for (int currByteInImage = msgLengthEndIndex; currByteInImage >= msgLengthStartIndex; currByteInImage--) {
            msgLength = (msgLength << 1) | getBit(imageBytesArray[currByteInImage], 0);
        }

        BitSet outSet = new BitSet();
        int outSetIndex = 0;
        for (int currByteInImage = msgLengthEndIndex; currByteInImage < msgLengthEndIndex + msgLength * byteLength; currByteInImage++) {
            boolean bit = getBit(imageBytesArray[currByteInImage], 0) == 1;
            outSet.set(
                    outSetIndex,
                    bit
            );
            outSetIndex++;
        }
        System.out.println("outset.length: " + outSet.length());
        System.out.println(Arrays.toString(outSet.toByteArray()));

        String result = "";
        try {
            result = new String(outSet.toByteArray(), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String hideFile(String file_payload, String cover_image) {
        byte[] imageBytesArray = null;
        byte[] payloadBytesArray = null;
        Path imagePath = Paths.get(cover_image);
        Path payloadPath = Paths.get(file_payload);
        try {
            imageBytesArray = Files.readAllBytes(imagePath);
            payloadBytesArray = Files.readAllBytes(payloadPath);
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }
        byte[] extensionBytes = new byte[extBitsLength / 8];
        int n = file_payload.lastIndexOf('.');
        if (n > 0) {
            try {
                System.arraycopy(file_payload.substring(n + 1).getBytes("US-ASCII"), 0, extensionBytes, 0, file_payload.substring(n + 1).getBytes().length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        int availableBytesInPic = imageBytesArray.length;

        // save length
        for (int i = msgLengthStartIndex; i < msgLengthEndIndex; i++) {
            imageBytesArray[i] = (byte) swapLsb(
                    getBit(payloadBytesArray.length, i - msgLengthStartIndex),
                    imageBytesArray[i]);

        }

        // zapisvame extension
        BitSet extSet = BitSet.valueOf(extensionBytes);
        int extSetIndex = 0;
        int extSetLength = extBitsLength;

        for (int currByteInImage = extensionStartIndex; currByteInImage < extSetLength; currByteInImage++) {
            imageBytesArray[currByteInImage] = (byte) swapLsb(
                    extSet.get(extSetIndex) ? 1 : 0,
                    imageBytesArray[currByteInImage]
            );
        }

        // zapisvame payload-a
        BitSet payloadSet = BitSet.valueOf(payloadBytesArray);
        int setIndex = 0;
        int setLength = payloadBytesArray.length * byteLength;

        for (int currByteInImage = extensionEndIndex; currByteInImage < availableBytesInPic && setIndex < setLength; currByteInImage++) {
            imageBytesArray[currByteInImage] = (byte) swapLsb(
                    payloadSet.get(setIndex) ? 1 : 0,
                    imageBytesArray[currByteInImage]);
            setIndex++;
        }

        try {
            FileOutputStream fos = new FileOutputStream(new File("outFile.bmp"));
            fos.write(imageBytesArray);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "outFile.bmp";
    }

    public String extractFile(String stego_image) {
        byte[] imageBytesArray = null;
        Path path = Paths.get(stego_image);
        try {
            imageBytesArray = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        // get length
        int fileLength = 0;
        for (int currByteInImage = msgLengthEndIndex; currByteInImage >= msgLengthStartIndex; currByteInImage--) {
            fileLength = (fileLength << 1) | getBit(imageBytesArray[currByteInImage], 0);
        }

        // get extension bits
        BitSet outExtSet = new BitSet();
        int outExtSetIndex = 0;

        for (int currByteInImage = extensionStartIndex; currByteInImage < extensionEndIndex; currByteInImage++) {
            boolean bit = getBit(imageBytesArray[currByteInImage], 0) == 1;
            outExtSet.set(outExtSetIndex, bit);
            outExtSetIndex++;
        }

        String extension = "";
        try {
            extension = new String(outExtSet.toByteArray(), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // get file bits
        BitSet outFileSet = new BitSet();
        int outFileSetIndex = 0;
        for (int currByteInImage = extensionEndIndex; currByteInImage < extensionEndIndex + fileLength * byteLength; currByteInImage++) {
            boolean bit = getBit(imageBytesArray[currByteInImage], 0) == 1;
            outFileSet.set(outFileSetIndex, bit);
            outFileSetIndex++;
        }


        // saving file
        String outFileName = "mazniqFail" + "." + extension;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(outFileName));
            fos.write(outFileSet.toByteArray());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outFileName;
    }

    public int swapLsb(int bitToHide, int byt) {
        byt = ((byt >> 1) << 1);
        byt |= bitToHide;
        return byt;
    }

    public int getBit(byte b, int position) {
        return (b >> position) & 1;
    }

    int getBit(int b, int position) {
        return (b >> position) & 1;
    }


}