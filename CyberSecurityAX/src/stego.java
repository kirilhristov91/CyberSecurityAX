import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.util.*;

import javax.imageio.ImageIO;
import java.io.FileOutputStream;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.awt.image.DataBufferByte;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.color.ColorSpace;
import java.awt.Color;


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

        // get dimenstions of photo
        int w = imageBI.getWidth();
        int h = imageBI.getHeight();

        // calculate num of pixels
        int numOfPixels = w * h;

        byte[] imagesPixelsBytesArray = null;
        byte[] msgByteArray = null;

        String binary = "";
        try {

                // get pixels
                imagesPixelsBytesArray = ((DataBufferByte) imageBI.getRaster().getDataBuffer()).getData();

                msgByteArray = payload.getBytes("US-ASCII");

                for (int i = 0; i < msgByteArray.length; i++) {
                        byte p = msgByteArray[i];
                        System.out.println(i + " : " + p);
                        // String output = Integer.toBinaryString(p & 0xFF);
                        String output = String.format("%8s", Integer.toBinaryString(p & 0xFF)).replace(' ', '0');
                        System.out.println(output);
                        binary += output;
                }
                System.out.println("msgByteArray: \n" + Arrays.toString(msgByteArray));
                System.out.println("binary: \n" + binary);
                System.out.println("imagesPixelsBytesArray.length: " + imagesPixelsBytesArray.length + ", pixels: " + numOfPixels);
        } catch (Exception e) {
                e.printStackTrace();
                return "Fail";
        }
        char[] bitsArray = binary.toCharArray();
        System.out.println("bitsArray.length: " + bitsArray.length);
        System.out.println("imagesPixelsBytesArray[0]: " + imagesPixelsBytesArray[0]);
        imagesPixelsBytesArray[0] = (byte)bitsArray.length;
        System.out.println("imagesPixelsBytesArray[0]: " + imagesPixelsBytesArray[0]);

        for (int imageBytesIndex = 1, msgBytesIndex = 0; msgBytesIndex < bitsArray.length && imageBytesIndex < imagesPixelsBytesArray.length; imageBytesIndex++) {

              // System.out.println("---------- msgBytesIndex: "+msgBytesIndex+", imageBytesIndex: "+imageBytesIndex+" -----------------\n");
              int bit = bitsArray[msgBytesIndex] == '1' ? 1 : 0;
              // System.out.println("replace:" + imagesPixelsBytesArray[imageBytesIndex] + ", with: " + bit);
              int newValue = swapLsb(bit,(int)imagesPixelsBytesArray[imageBytesIndex]);
              // System.out.println("newValue: " + newValue + ", (byte)newValue: " + ((byte)newValue));
              imagesPixelsBytesArray[imageBytesIndex] = (byte)newValue;
              msgBytesIndex++;
              // System.out.println("\n------------------------------------------");
        }
        // System.out.println("imagesPixelsBytesArray: " + Arrays.toString(imagesPixelsBytesArray));

        for (int row = 0, ipbaIndex =0 ; row < h && ipbaIndex < bitsArray.length ; row ++){
          for (int col = 0; col < w && ipbaIndex < bitsArray.length; col ++){
            int r = imagesPixelsBytesArray[ipbaIndex++] & 0xFF;
            int g = imagesPixelsBytesArray[ipbaIndex++] & 0xFF;
            int b = imagesPixelsBytesArray[ipbaIndex++] & 0xFF;
            System.out.println("r,b,g: " + r +","+ b + ","+g);
            int c = new Color(r,g,b).getRGB();
            imageBI.setRGB(col,row,c);
          }
        }

        String stegoImageToReturn = "outSI2.bmp";
        // ByteArrayInputStream bais = new ByteArrayInputStream(imagesPixelsBytesArray);
        try {
            ImageIO.write(imageBI, "bmp", new File(stegoImageToReturn));


            // FileOutputStream fos = new FileOutputStream(new File(stegoImageToReturn));
            // fos.write(imagesPixelsBytesArray);
            // fos.flush();
            // fos.close();

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
        byte[] imagesPixelsBytesArray = null;
        try {
                imageBI = ImageIO.read(decodeImage);

                // image to bytes array
                imagesPixelsBytesArray = ((DataBufferByte) imageBI.getRaster().getDataBuffer()).getData();
        } catch (IOException e) {
                e.printStackTrace();
                return "Fail";
        }

        int w = imageBI.getWidth();
        int h = imageBI.getHeight();

        // get number of bits to extract
        int numOfBits = imagesPixelsBytesArray[0];
        System.out.println("numOfBits: " + numOfBits);

        ArrayList<Integer> msgChars = new ArrayList<Integer>();
        int decodedBit;
        for (int imageBytesIndex = 1 ; imageBytesIndex < numOfBits ; imageBytesIndex ++){
          byte value = imagesPixelsBytesArray[imageBytesIndex];
          decodedBit = value & DE_MASK_1;
          msgChars.add(decodedBit);
        }
        System.out.println("msgChars: " + Arrays.toString(msgChars.toArray()) + ", msgChars.length: " + msgChars.size());

        StringBuilder decodedMessage = new StringBuilder();
//         for (int i = 0; i < msgChars.size(); ) {
//                 StringBuilder b = new StringBuilder();
//                 int p = i + 8;
//                 for (int y = i; y < p; y++, i++) {
//                         b.append(String.valueOf(msgChars.get(y)));
//                 }
//                 String integerBinaryValueString = b.toString();
//                 //System.out.print(integerBinaryValueString + "\t");
//                 int value = Integer.parseInt(integerBinaryValueString, 2);
//                 char c = (char) (value);
//                 decodedMessage.append(c);
// //            byte[] bval = new BigInteger(integerBinaryValueString, 2).toByteArray();
// //            String output = new String(bval);
// //            int anInt = wrapped.getInt();
// //            Integer result = anInt / 2;
// //            System.out.println(result.toString());
//         }

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
        byt = ((byt >> 1) << 1);
        byt |= bitToHide;
        return byt;
}

/// print methods
public static void printInt(String o, int x) {
        System.out.println(o + " : " + x + ", bits: " + Integer.toBinaryString(x));
}

public static void printByte(String i, byte x) {
        System.out.println(i + " : byte -> " + x);
}

}
