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

    int DecodeMask = 0x1;

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

        int payloadByteIndex = 0;
        int bitIndex = 0;
        System.out.println("payloads byte array: " + Arrays.toString(payloadBytesArray));
        int availableBytesInPic = imageBytesArray.length;

        // zapisvame duljinata v 1-vite 32 byte-a sled headera
        for (int i = msgLengthStartIndex; i < msgLengthEndIndex; i++) {
            imageBytesArray[i] = (byte) swapLsb(
                    getBit(payloadBytesArray.length, i - msgLengthStartIndex),
                    imageBytesArray[i]);

        }

        BitSet set = BitSet.valueOf(payloadBytesArray);
        System.out.println("set: " + Arrays.toString(set.toByteArray()));


        int setIndex = 0;
        int setLength = set.length();
        int estimatedLenght = payloadBytesArray.length * byteLength;
        while(setLength < estimatedLenght){
            setLength++;
        }
        
        for (int currByteInImage = msgLengthEndIndex; currByteInImage < availableBytesInPic && setIndex < setLength; currByteInImage++) {
            imageBytesArray[currByteInImage] = (byte) swapLsb(
                    set.get(setIndex) ? 1 : 0,
                    imageBytesArray[currByteInImage]);
            setIndex++;
        }
        System.out.println("setIndex: " + setIndex);

        for (int i = 0; i < set.length(); i++) {
            System.out.print(set.get(i) ? 1 : 0);
            if (i % 8 == 0 && i != 0) {
                System.out.println();
            }
        }

        try {
            FileOutputStream fos = new FileOutputStream(new File("outSI.bmp"));
            fos.write(imageBytesArray);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return "outSI.bmp";
    }

    public String extractString(String stego_image) {
        byte[] imageBytesArray = null;
        Path path = Paths.get(stego_image);
        System.out.println(path);
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
        System.out.println("outset.length: "+ outSet.length());
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
        return null;
    }

    public String extractFile(String stego_image) {

        return null;
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