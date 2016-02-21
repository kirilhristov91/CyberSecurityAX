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
     * @param payload        - the string which should be hidden in the cover image.
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

        // check if enough space
        if (!hasEnoughSpace(imageBytesArray, msgLengthEndIndex, payloadBytesArray)) {
            return "Fail";
        }

        // save the length of the message in the first 32 bytes after the headers
        saveLength(imageBytesArray, payloadBytesArray);

        // save the message
        importBitsetInBytes(BitSet.valueOf(payloadBytesArray), msgLengthEndIndex, msgLengthEndIndex + payloadBytesArray.length * byteLength, imageBytesArray);

        // generate a result image file using the altered image byte array
        String outFilename = "outImage.bmp";
        if (!saveBytesToFile(imageBytesArray, outFilename)) {
            return "Fail";
        }

        // return the name of the generated file
        return outFilename;
    }

    /**
     * The extractString method should extract a string which has been hidden in the stegoimage
     *
     * @param stego_image the name of the stego image
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
        int msgLength = getLengthOfPayload(imageBytesArray);

        // bitset to store the bits of the encoded message
        BitSet outSet = getData(imageBytesArray, msgLengthStartIndex, msgLengthEndIndex + msgLength * byteLength);

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

    private BitSet getData(byte[] source, int startIndex, int endIndex) {
        BitSet outSet = new BitSet();
        // for each byte (after the length bytes)
        for (int currByteInImage = startIndex, outSetIndex = 0; currByteInImage < endIndex; currByteInImage++, outSetIndex++) {
            //get the least significant bit and store it in the bitset
            outSet.set(outSetIndex, getBit(source[currByteInImage], 0) == 1);
        }
        return outSet;
    }

    /**
     * The hideFile method hides any file (so long as there's enough capacity in the image file) in a cover image
     *
     * @param file_payload - the name of the file to be hidden, you can assume it is in the same directory as the program
     * @param cover_image  - the name of the cover image file, you can assume it is in the same directory as the program
     * @return String - either 'Fail' to indicate an error in the hiding process, or the name of the stego image written out as a
     * result of the successful hiding process
     */
    public String hideFile(String file_payload, String cover_image) {

        // instantiating variable
        byte[] imageBytesArray, payloadBytesArray, extensionBytes = new byte[extBitsLength / 8];
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

        // check if enough space
        if (hasEnoughSpace(imageBytesArray, extensionEndIndex, payloadBytesArray)) {
            return "Fail";
        }

        // save the length of the file to be hidden in the first 32 bytes after the headers
        saveLength(imageBytesArray, payloadBytesArray);

        // save the extension in the 64 bytes after the length bytes
        importBitsetInBytes(BitSet.valueOf(extensionBytes), extensionStartIndex, extensionEndIndex, imageBytesArray);

        // save data in rest of image's bytes
        importBitsetInBytes(BitSet.valueOf(payloadBytesArray), extensionEndIndex, extensionEndIndex + payloadBytesArray.length * byteLength, imageBytesArray);

        // try to save it to a file
        String outFilename = "outFile.bmp";
        if (!saveBytesToFile(imageBytesArray, outFilename)) {
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
        int fileLength = getLengthOfPayload(imageBytesArray);

        // get extension bits
        BitSet outExtSet = getData(imageBytesArray, extensionStartIndex, extensionEndIndex);

        // transform the gathered extension bits to string
        String extension = "";
        try {
            extension = new String(outExtSet.toByteArray(), "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "Fail";
        }

        // get file bits
        BitSet outFileSet = getData(imageBytesArray, extensionEndIndex, extensionEndIndex + fileLength * byteLength);

        // generate the file
        String outFileName = "extractedFile" + "." + extension;
        if (!saveBytesToFile(outFileSet.toByteArray(), outFileName)) {
            return "Fail";
        }

        // return the name of the extracted file
        return outFileName;
    }

    /**
     * This method swaps the least significant bit with a bit from the filereader
     *
     * @param bitToHide - the bit which is to replace the lsb of the byte of the image
     * @param byt       - the current byte
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
     * @param b        base byte
     * @param position the position of the desired bit from the byte
     * @return value of the bit at the desired position in the byte.
     */
    public int getBit(byte b, int position) {
        return (b >> position) & 1;
    }

    /**
     * Get bit from Int
     *
     * @param i        base integer
     * @param position the position of the desired bit from the integer
     * @return value of the bit at the desired position in the integer.
     */
    public int getBit(int i, int position) {
        return (i >> position) & 1;
    }

    /**
     * Save bytes to file
     *
     * @param bytes    bytes to save
     * @param fileName the name of the file
     * @throws IOException
     */
    private boolean saveBytesToFile(byte[] bytes, String fileName) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            fos.write(bytes);
            fos.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * saves the length
     *
     * @param destination       destination
     * @param payloadBytesArray payload
     */
    private void saveLength(byte[] destination, byte[] payloadBytesArray) {
        ArrayList<Integer> savedBits = new ArrayList<>();
        for (int i = msgLengthStartIndex; i < msgLengthEndIndex; i++) {
            int bitToSave = getBit(payloadBytesArray.length, i - msgLengthStartIndex);
            savedBits.add(bitToSave);
            destination[i] = (byte) swapLsb(bitToSave, destination[i]);
        }
    }

    private void importBitsetInBytes(BitSet set, int startIndex, int endIndex, byte[] destination) {
        for (int currByteInImage = startIndex, setIndex = 0; currByteInImage < endIndex; currByteInImage++, setIndex++) {
            destination[currByteInImage] = (byte) swapLsb(set.get(setIndex) ? 1 : 0, destination[currByteInImage]);
        }
    }

    private boolean hasEnoughSpace(byte[] destination, int offset, byte[] payloadBytesArray) {
        // calculate the available bytes to encode the message in
        int availableBytesInPic = destination.length - offset;

        // calculate required space
        int requiredSpaceInBits = payloadBytesArray.length * byteLength;

        // check if message is too big to be encoded in the given image
        // we write 1bit of payload data per 1byte ot image data
        if (requiredSpaceInBits > availableBytesInPic) {
            return false;
        }
        return true;
    }

    private int getLengthOfPayload(byte[] imageBytesArray) {
        int msgLength = 0;
        for (int currByteInImage = msgLengthEndIndex; currByteInImage >= msgLengthStartIndex; currByteInImage--) {
            msgLength = (msgLength << 1) | getBit(imageBytesArray[currByteInImage], 0);
        }
        return msgLength;
    }

}
