import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.util.*;

import javax.imageio.ImageIO;

class Steg {

	private static final int MASK_0 = 0xFFFFFFFE;
    private static final int MASK_1 = 0x1;
    private static final int DE_MASK_1 = 0x1;
	

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
     * Default constructor to create a steg object, doesn't do anything - so we actually don't need to declare it explicitly. Oh well.
     */

    public Steg() {

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

    //TODO you must write this method
    public String hideString(String payload, String cover_filename) {
    	File imageFile = new File(cover_filename);
    	BufferedImage imageBI = null;
		try {
			imageBI = ImageIO.read(imageFile);
		} catch (IOException e) {
			e.printStackTrace();
			return "Fail";	
		}
		
        int w = imageBI.getWidth();
        int h = imageBI.getHeight();

        int msgLength = payload.length();
        //System.out.println("message=" + payload + " length=" + msgLength);

        int msglendecode = imageBI.getRGB(0, 0);
        //System.out.println("msglenecode: " + msglendecode + ", bytes: " + Integer.toBinaryString(msglendecode));
        msglendecode = msglendecode >> 8;
        //System.out.println("msglenecode >> 8: " + msglendecode + ", bytes: " + Integer.toBinaryString(msglendecode));
        msglendecode = msglendecode << 8;
        //System.out.println("msglenecode << 8: " + msglendecode + ", bytes: " + Integer.toBinaryString(msglendecode));
        //System.out.println("msgLength: " + msgLength + ", bytes: " + Integer.toBinaryString(msgLength));
        msglendecode |= msgLength;
        //System.out.println("msglenecode |= msgLength: " + msglendecode + ", bytes: " + Integer.toBinaryString(msglendecode));

        int argb3 = imageBI.getRGB(0, 0);
        //printInt("0,0 before set: ", argb3);
        imageBI.setRGB(0, 0, msglendecode); // save length
        int argb2 = imageBI.getRGB(0, 0);
        //printInt("0,0 after set: ", argb2);

        String binary = new BigInteger(payload.getBytes()).toString(2);
        //System.out.println("binary: " + binary);
        char[] bitsArray = binary.toCharArray();

        for (int row = 0, charsIndex = 0; row < h && charsIndex < bitsArray.length; row++) {
            for (int col = 0; col < w && charsIndex < bitsArray.length; col++) {
                if (row == 0 && col == 0) {
                    continue;
                }

                char c = bitsArray[charsIndex];
                int argb = imageBI.getRGB(col, row);
                if (c == '1') {
                    argb |= MASK_1;
                } else if (c == '0') {
                    argb &= MASK_0;
                } else {
                    System.out.println("WTF");
                }
                //System.out.println("bit to insert = " + c + "\tSetting RGB (" + col + "," + row + ") = " + Integer.toBinaryString(argb));
                imageBI.setRGB(col, row, argb);
                charsIndex++;
            }
        }

        String stegoImageToReturn = "outputStegoImage.bmp";
        try {
            ImageIO.write(imageBI, "bmp", new File(stegoImageToReturn));
        } catch (Exception e) {
            e.printStackTrace();
            return "Fail";
        }
       
        return stegoImageToReturn;
    }

    //TODO you must write this method
    /**
     * The extractString method should extract a string which has been hidden in the stegoimage
     *
     * @param the name of the stego image
     * @return a string which contains either the message which has been extracted or 'Fail' which indicates the extraction
     * was unsuccessful
     */
    public String extractString(String stego_image) {
    	
        File decodeImage = new File(stego_image);
        BufferedImage imageBI = null;
		try {
			imageBI = ImageIO.read(decodeImage);
		} catch (IOException e) {
			e.printStackTrace();
			return "Fail";
		}

        int w = imageBI.getWidth();
        int h = imageBI.getHeight();

        int MASK_LENGTH = 0xFF;

        int argb = imageBI.getRGB(0, 0);
        argb &= MASK_LENGTH;

        int lengthInLetters = argb;
        //System.out.println("legnthInLetters: " + lengthInLetters);

        ArrayList<Integer> msgChars = new ArrayList<Integer>();

        for (int row = 0, numOfExtractedBits = 0; row < h && numOfExtractedBits < lengthInLetters * 8; row++) {
            for (int col = 0; col < w && numOfExtractedBits < lengthInLetters * 8; col++) {

                // ostavame 1-viq pixel na mira
                if (row == 0 && col == 0) {
                    continue;
                }

                int argbCurrPX = imageBI.getRGB(col, row);
                int decodedBit = argbCurrPX & DE_MASK_1;
                //System.out.println("decodedBit = " + decodedBit + "\tRGB(" + col + "," + row + ") = " + Integer.toBinaryString(argbCurrPX));

                msgChars.add(decodedBit);
                numOfExtractedBits++;
            }
        }
        /*
        System.out.println(msgChars.size());
        System.out.println(Arrays.toString(msgChars.toArray()));

        for (int i = 0; i < msgChars.size(); i++) {
            if (i % 8 == 0) {
                System.out.print(" ");
            }
            System.out.print(msgChars.get(i).toString());
        }*/

        //System.out.println("------------------------------------------------------------------------");
        //System.out.println("Output:");
        StringBuilder decodedMessage  = new StringBuilder();
        for (int i = 0; i < msgChars.size(); ) {
            StringBuilder b = new StringBuilder();
            int p = i+8;
            for (int y = i; y < p; y++, i++) {
                b.append(String.valueOf(msgChars.get(y)));
            }
            String integerBinaryValueString = b.toString();
            //System.out.print(integerBinaryValueString + "\t");
            int value = Integer.parseInt(integerBinaryValueString, 2);
            char c = (char) (value / 2);
            decodedMessage.append(c);
//            byte[] bval = new BigInteger(integerBinaryValueString, 2).toByteArray();
//            String output = new String(bval);
//            int anInt = wrapped.getInt();
//            Integer result = anInt / 2;
//            System.out.println(result.toString());
        }
		
        return decodedMessage.toString();
    }

    //TODO you must write this method

    /**
     * The hideFile method hides any file (so long as there's enough capacity in the image file) in a cover image
     *
     * @param file_payload - the name of the file to be hidden, you can assume it is in the same directory as the program
     * @param cover_image  - the name of the cover image file, you can assume it is in the same directory as the program
     * @return String - either 'Fail' to indicate an error in the hiding process, or the name of the stego image written out as a
     * result of the successful hiding process
     */
    public String hideFile(String file_payload, String cover_image) {
        return null;
    }

    //TODO you must write this method

    /**
     * The extractFile method hides any file (so long as there's enough capacity in the image file) in a cover image
     *
     * @param stego_image - the name of the file to be hidden, you can assume it is in the same directory as the program
     * @return String - either 'Fail' to indicate an error in the extraction process, or the name of the file written out as a
     * result of the successful extraction process
     */
    public String extractFile(String stego_image) {

        return null;
    }

    //TODO you must write this method

    /**
     * This method swaps the least significant bit with a bit from the filereader
     *
     * @param bitToHide - the bit which is to replace the lsb of the byte of the image
     * @param byt       - the current byte
     * @return the altered byte
     */
    public int swapLsb(int bitToHide, int byt) {
        return 0;
    }

    /// print methods
    public static void printInt(String o, int x) {
        System.out.println(o + " : " + x + ", bits: " + Integer.toBinaryString(x));
    }

    public static void printByte(String i, byte x) {
        System.out.println(i + " : byte -> " + x);
    }

}
