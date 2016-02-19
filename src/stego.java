import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class Steg {

    /**
     * A constant to hold the number of bits per byte
     */
    private final int byteLength = 8;

    /**
     * A constant to hold the number of bits used to store the size of the file extracted
     */
    protected final int sizeBitsLength = 32;

    /**
     * A constant to hold the number of bits used to store the extension of the file extracted
     */
    protected final int extBitsLength = 64;

    /**
     * A constant to hold the number of bits used for image header
     */
    protected final int headerLenght = 54;

    /**
     * Constants to use when storing and extracting the length of message/file
     */
    private final int msgLengthStartIndex = headerLenght;
    private final int msgLengthEndIndex = sizeBitsLength + headerLenght;

    /**
     * Constants to use when storing and extracting the file extension
     */
    private final int extensionStartIndex = msgLengthEndIndex;
    private final int extensionEndIndex = extensionStartIndex + extBitsLength;

    public Steg() {
      // undeeded but provided constructor
    }

    /**
     * A method for hiding a string in an uncompressed image file such as a .bmp or .png
     * You can assume a .bmp will be used
     *
     * @param cover_filename - the filename of the cover image as a string
     * @param payload - the string which should be hidden in the cover image.
     * @return a string which either contains 'Fail' or the name of the stego image which has been
     * written out as a result of the successful hiding operation.
     * You can assume that the images are all in the same directory as the java files
     */
    public String hideString(String payload, String cover_filename) {
        byte[] imageBytesArray = null;
        byte[] payloadBytesArray = new byte[0];
        Path path = Paths.get(cover_filename);

        // store the bytes of the image and the message in byte arrays
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

        // calculate the available bytes to encode the message in
        int availableBytesInPic = imageBytesArray.length - msgLengthEndIndex;

        // check if message is too big to be encoded in the given image
        if(payloadBytesArray.length * byteLength > availableBytesInPic){
            return "Fail";
        }

        // save the length of the message in the first 32 bytes after the headers
        for (int i = msgLengthStartIndex; i < msgLengthEndIndex; i++) {
            // swap the least significant bit of the current byte with bit from the length
            imageBytesArray[i] = (byte) swapLsb(
                    getBit(payloadBytesArray.length, i - msgLengthStartIndex),
                    imageBytesArray[i]);

        }

        // save the message
        // get the message in bits
        BitSet set = BitSet.valueOf(payloadBytesArray);
        int setIndex = 0;
        int setLength = payloadBytesArray.length * byteLength;

        // for each byte in the image (after those that store the length)
        for (int currByteInImage = msgLengthEndIndex; currByteInImage < availableBytesInPic && setIndex < setLength; currByteInImage++) {
            // swap the least significant bit of the current byte with bit from the message
            imageBytesArray[currByteInImage] = (byte) swapLsb(
                    set.get(setIndex) ? 1 : 0,
                    imageBytesArray[currByteInImage]);
            setIndex++;
        }

        // generate a result image file using the altered image byte array
        String outFilename = "outSI.bmp";
        try {
            FileOutputStream fos = new FileOutputStream(new File(outFilename));
            fos.write(imageBytesArray);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        // return the name of the generated file
        return outFilename;
    }

    /**
     * The extractString method should extract a string which has been hidden in the stegoimage
     *
     * @param the name of the stego image
     * @return a string which contains either the message which has been extracted or 'Fail' which indicates the extraction
     * was unsuccessful
     */
    public String extractString(String stego_image) {
        byte[] imageBytesArray = null;
        Path path = Paths.get(stego_image);

        // store the image bytes in byte array
        try {
            imageBytesArray = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        // get the length of the encoded message
        int msgLength = 0;
        for (int currByteInImage = msgLengthEndIndex; currByteInImage >= msgLengthStartIndex; currByteInImage--) {
            msgLength = (msgLength << 1) | getBit(imageBytesArray[currByteInImage], 0);
        }

        // bitset to store the bits of the encoded message
        BitSet outSet = new BitSet();
        int outSetIndex = 0;
        // for each byte (after the length bytes)
        for (int currByteInImage = msgLengthEndIndex; currByteInImage < msgLengthEndIndex + msgLength * byteLength; currByteInImage++) {
            //get the least significant bit and store it in the bitset
            outSet.set(outSetIndex++,getBit(imageBytesArray[currByteInImage], 0) == 1);
        }

        // transform the bitset into string
        String result = "";
        try {
            result = new String(outSet.toByteArray(), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "Fail";
        }

        // return the encoded message
        return result;
    }

    /**
     * The hideFile method hides any file (so long as there's enough capacity in the image file) in a cover image
     *
     * @param file_payload - the name of the file to be hidden, you can assume it is in the same directory as the program
     * @param cover_image - the name of the cover image file, you can assume it is in the same directory as the program
     * @return String - either 'Fail' to indicate an error in the hiding process, or the name of the stego image written out as a
     * result of the successful hiding process
     */
    public String hideFile(String file_payload, String cover_image) {
        byte[] imageBytesArray = null;
        byte[] payloadBytesArray = null;
        byte[] extensionBytes = new byte[extBitsLength / 8];
        int n = file_payload.lastIndexOf('.');
        Path imagePath = Paths.get(cover_image);
        Path payloadPath = Paths.get(file_payload);

        // store the bytes of the image and the file to be hidden in byte arrays
        try {
            imageBytesArray = Files.readAllBytes(imagePath);
            payloadBytesArray = Files.readAllBytes(payloadPath);
            if (n > 0) {
                // get extension
                String extension = file_payload.substring(n + 1);

                // prepare extension string for storage
                System.arraycopy(extension.getBytes("US-ASCII"), 0, extensionBytes, 0, extension.getBytes().length);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        // calculate the available bytes to encode the file bits
        int availableBytesInPic = imageBytesArray.length - extensionEndIndex;

        // check if message is too big to be encoded in the given image
        if(payloadBytesArray.length * byteLength > availableBytesInPic){
            return "Fail";
        }
        //

        // save the length of the file to be hidden in the first 32 bytes after the headers
        for (int i = msgLengthStartIndex; i < msgLengthEndIndex; i++) {
            imageBytesArray[i] = (byte) swapLsb(getBit(payloadBytesArray.length, i - msgLengthStartIndex), imageBytesArray[i]);

        }

        // save the extension in the 64 bytes after the length bytes
        BitSet extSet = BitSet.valueOf(extensionBytes);
        int extSetIndex = 0;
        for (int currByteInImage = extensionStartIndex; currByteInImage < extensionEndIndex; currByteInImage++) {
            imageBytesArray[currByteInImage] = (byte) swapLsb(extSet.get(extSetIndex++) ? 1 : 0, imageBytesArray[currByteInImage]);
        }

        // save the file bits
        BitSet payloadSet = BitSet.valueOf(payloadBytesArray);
        // for each byte in the image (after the extension bytes)
        for (int currByteInImage = extensionEndIndex, setIndex = 0; setIndex < payloadBytesArray.length * byteLength; currByteInImage++, setIndex ++) {
            // swap the least significant bit with such from the file bits
            imageBytesArray[currByteInImage] = (byte) swapLsb(payloadSet.get(setIndex) ? 1 : 0, imageBytesArray[currByteInImage]);
        }

        // generate an image file using the altered imageBytesArray
        String outFilename = "outFile.bmp";
        try {
            FileOutputStream fos = new FileOutputStream(new File(outFilename));
            fos.write(imageBytesArray);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        // return the name of the generated image
        return outFilename;
    }

    /**
     * The extractFile method hides any file (so long as there's enough capacity in the image file) in a cover image
     *
     * @param stego_image - the name of the file to be hidden, you can assume it is in the same directory as the program
     * @return String - either 'Fail' to indicate an error in the extraction process, or the name of the file written out as a
     * result of the successful extraction process
     */
    public String extractFile(String stego_image) {
        byte[] imageBytesArray = null;
        Path path = Paths.get(stego_image);
        try {
            imageBytesArray = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        // get the length of the file
        int fileLength = 0;
        for (int currByteInImage = msgLengthEndIndex; currByteInImage >= msgLengthStartIndex; currByteInImage--) {
            fileLength = (fileLength << 1) | getBit(imageBytesArray[currByteInImage], 0);
        }

        // get extension bits
        BitSet outExtSet = new BitSet();
        int outExtSetIndex = 0;
        for (int currByteInImage = extensionStartIndex; currByteInImage < extensionEndIndex; currByteInImage++) {
            outExtSet.set(outExtSetIndex++, getBit(imageBytesArray[currByteInImage], 0) == 1);
        }
        // transform the gathered extension bits to string
        String extension = "";
        try {
            extension = new String(outExtSet.toByteArray(), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "Fail";
        }

        // get file bits
        BitSet outFileSet = new BitSet();
        int outFileSetIndex = 0;
        // get the least significant bit of each byte between the extension bytes and the byte containing the last bit of the file
        for (int currByteInImage = extensionEndIndex; currByteInImage < extensionEndIndex + fileLength * byteLength; currByteInImage++) {
            outFileSet.set(outFileSetIndex++, getBit(imageBytesArray[currByteInImage], 0) == 1);
        }

        // generate the file
        String outFileName = "extractedFile" + "." + extension;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(outFileName));
            fos.write(outFileSet.toByteArray());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "Fail";
        }

        // return the name of the extracted file
        return outFileName;
    }

    /**
     * This method swaps the least significant bit with a bit from the filereader
     *
     * @param bitToHide - the bit which is to replace the lsb of the byte of the image
     * @param byt - the current byte
     * @return the altered byte
     */
    public int swapLsb(int bitToHide, int byt) {
        byt = ((byt >> 1) << 1);
        byt |= bitToHide;
        return byt;
    }

    /**
    * Get bit from Byte
    *
    * @param b base byte
    * @param position the position of the desired bit from the byte
    * @return value of the bit at the desired position in the byte.
    */
    public int getBit(byte b, int position) {
        return (b >> position) & 1;
    }

    /**
    * Get bit from Int
    *
    * @param i base integer
    * @param position the position of the desired bit from the integer
    * @return value of the bit at the desired position in the integer.
    */
    public int getBit(int i, int position) {
        return (i >> position) & 1;
    }
}
