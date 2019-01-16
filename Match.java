package rmidi;

import java.util.Arrays;

public class Match {
    /**
     * Finds byte array b in byte array a
     * @param a first array, ocean
     * @param b second array, perl
     * @return -1 if not match, else index at which b start in a
     */
    public static int match(byte[] a, byte[] b) {
        return match(a, b, 0, a.length);
    }

    /**
     *
     * @param a search array
     * @param b pattern to be search
     * @param x  start index  within array x
     * @param y  end index of array y
     * @return
     */
    public static int match(byte[] a, byte[] b, int x, int y) {
        int i = x, j;
        while((i < a.length) && (i < y)) {
            j = 0;
            while(j < b.length) {
                if(a[i+j] != b[j]) break;
                j++;
            }
            if(j == b.length) return i;
            i++;
        }return -1;
    }



    /**
     * Match the array b element in a, but array b is divided into two array. b[0-offset] and b[offset - MTrk_length]<br>
     * Function match b[0-offset] and then check for pattern b[offset - MTrk_length] further onwards.
     * @param a byte[] array
     * @param b byte[] array
     * @param offset integer to break array b
     * @return returns first index found for pattern b[offset - MTrk_length] from match index b[0 - offset]
     */
    public static int match(byte[] a, byte[] b, int offset) {
        byte[] ac = Arrays.copyOfRange(b,0, offset);
        int d = match(a, ac, a.length);
        return d + match(Arrays.copyOfRange(a, d, a.length), Arrays.copyOfRange(b, offset, b.length));
    }

}
