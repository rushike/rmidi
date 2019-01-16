package rmidi;


import java.util.ArrayList;
import java.util.StringTokenizer;

public class Convert{

    //Wrappers
    protected static byte WRAP_DATA = 0x7f;

    protected static byte WRAP_DATA_BITS = 0x7;
    /**
     * returns file name from path string
     * @param path
     * @return
     */
    public static String NAME(String path) {
        String separator = System.getProperty("file.separator");
        StringTokenizer str = new StringTokenizer(path, separator);
        String file;
        int i = str.countTokens();
        while(--i > 0 )str.nextToken();
        return str.nextToken(".");
    }

    /**
     * type = 0 big-endian
     * type = 1 little endian
     */
    public static byte[] BYTE_ARR(int sz, int type) {
        int itr = 3;
        long yt = (long) (sz & 0xffffffff);
        byte[] ft = new byte[4];
        while(yt >= 0 && itr > -1) {
            ft[itr--] = (byte)(yt & 0xff);
            yt = yt >> 8;
        }
        if(type == 0) return ft;
        else {
            return rev(ft);
        }
    }


    /**
     * Converts hexadiimal number / hex byte array to int
     */
    public static int INT(byte[] sz, int type) {
        int num = 0, i = 0, s = 1 , itr = 0;
        if(type == 1) {
            s = -1;
            i = sz.length;
        }//set loop from end for little indian        
        while(itr < 4 && itr < sz.length) {
            i = i + s * 1;
            //System.out.println("INT "+itr+" : IN " + HEX(num));
           // System.out.println("BYTE  : IN " + HEX(sz[i]));
            num = num << 8;
            num = num + (sz[i] & 0xff);
            //System.out.println("INT  : IN " + HEX(num));
            itr++;
        }return  num;
    }

    public static int SIZE(int len){
        return 8192;
    }

    public static  String CHAR(byte[] num, int type) {
        if(type == 1) num = rev(num);
        StringBuilder sbm = new StringBuilder();
        int itr = 0;
        while(itr < num.length) {
            sbm.append(Character.toChars(num[itr++] & 0xff));
        }return sbm.toString();
    }

    final static char[] digit = new char[]{'0','1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String HEX(byte num) {
        int st = num & 0xff;
        return "" + digit[st >> 4] + digit[st & 0xf];
    }

    public static String HEX(int num) {
        int itr = 0;
        long numt = num & 0xffffffff;
        StringBuilder sb = new StringBuilder();
        while(itr < 4) {
            sb.insert(0, HEX((byte)(numt & 0xff)));
            numt = numt >>> 4;
            itr++;
        }return sb.toString();
    }

    /**
     *
     * @param num byte array to be converted
     * @param type type : big-endian or small-endian
     * @param group no. of bytes in group
     * @param length length of line(in bytes)
     * @return
     */
    public static String HEX(byte[] num, int type, int group, int length) {
        if(type == 1) num = rev(num);
        if(group == 0) group = num.length;
        StringBuilder sbm = new StringBuilder();
        int itr = 0;
        while(itr < num.length) {
            sbm.append(HEX(num[itr++]) + " ");
            if(itr % group == 0 ) sbm.append(" ");
            if(itr % length == 0) sbm.append("\n");
        }return sbm.toString();
    }


    public static byte[] bytelist_to_bytearray(ArrayList list){
        byte[] li = new byte[list.size()];
        for(int i = 0; i < li.length; i++){
            li[i] = (byte)list.get(i);
        }
        return li;        
    }

    public static long to_hex(byte[] num, int bits) {
        int i = -1;
        long hex = 0x0;
        while(++i < num.length) {
            hex = hex << bits;
            hex = hex + (num[i] & ((1 << bits) - 1 ));
        }return  hex;
    }

    public static long var_to_hex(byte[] var) {
	    int i = -1;
	    long hex = 0x0;
	    while(++i < var.length) {
            hex = hex << 0x7;
	        hex = hex + (var[i] & 0x7f);
        }return  hex;
    }

    public static byte[] to_var_length(long k) {
	    byte[] var;
	    if(k > 127) {
	        int len = length(k)/7 + 1;
	        var = new byte[len];
	        var[len - 1] = (byte)(k & WRAP_DATA);
            k = k >> WRAP_DATA_BITS;
	        for(int i = len - 2; i > -1 && k > -1; i--) {
                var[i] = (byte)((k & WRAP_DATA) + WRAP_DATA + 1);
                k = k >> WRAP_DATA_BITS;
            }
        }else{
	        var = new byte[1];
	        var[0] = (byte)k;
        }return var;
    }

    
    /**
     * @param k - int length
     * @param len - byte array length
     * @param bits - encoding bits
     */
    public static byte[] to_fix_length(long k, int len, int bits) {
	    byte[] fix = new byte[len];
	    int wrapper = (1 << bits) - 1;
        if(k > 255) {
            fix[len - 1] = (byte)(k & wrapper);
            k = k >>> bits;
            for(int i = len - 2; i > -1 && k > 0; i--) {
                fix[i] = (byte)((k & wrapper) + wrapper + 1);
                k = k >>> bits;
            }
        }else{
            fix = new byte[len];
            fix[len - 1] = (byte)k;
        }return fix;

    }

    public static int length(long k) {
	    return (int)(Math.log(k) / Math.log(2) + 1);
    }

    public static byte[] rev(byte[] num) {
        byte[] rv = new byte[num.length];
        for(int i = 0; i < num.length; i++) {
            rv[num.length - 1 - i] = num[i];
        }return rv;
    }
}