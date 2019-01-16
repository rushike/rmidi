package rmidi;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class RIFF{
    
    byte[] stream;

    InputStream isf;

    String file_name;

    final File file;

    HEAD head;

    INFO info;

    DATA data;

    public RIFF(String path) {
        file = new File(path);
        byte[] buf = new byte[4];
        int rt, len, itr = 0;
        try {
            //reading head
            isf = new FileInputStream(file);
            rt = isf.read(buf, 0, 4);
            itr = itr + 4;
            if(Match.match(buf, Constant.HEAD_ID) != 0) throw new FileFormatException("FILE with no head");
            rt = isf.read(buf, 0, 4);
            itr = itr + 4;
            len = Convert.INT(buf, 1);
            buf = new byte[4];
            rt = isf.read(buf, 0, 4);
            itr = itr + 4;
            head = new HEAD(buf, len);

            //reading info
            rt = isf.read(buf, 0, 4);
            itr = itr + 4;
            if(Match.match(buf, Constant.HEAD_ID) == 0) throw new FileFormatException("FILE with no info");
            rt = isf.read(buf, 0, 4);
            itr = itr + 4;
            len = Convert.INT(buf, 1);
            System.out.println("rt : " + rt +" "+ len +" "+Convert.HEX(buf, 1, 0, 100));
           // System.out.println(Arrays.toString(buf));
            buf = new byte[len];
            rt = isf.read(buf, 0, len);
            itr = itr + len;
            info = new INFO(buf, len);

            //setting data index
            rt = isf.read(buf, 0, 4);
            itr = itr + 4;
            if(Match.match(buf, Constant.HEAD_ID) == 0) throw new FileFormatException("FILE with no info");
            rt = isf.read(buf, 0, 4);
            itr = itr + 4;
            len = Convert.INT(buf, 1);
            data = new DATA(itr, len);



        }catch (Exception fst) {
            fst.printStackTrace();
        }
    }

    public RIFF(byte[] stream, String name) {
        file = new File("wav.data" + System.getProperty("file.seperate") + name);
        file_name = Convert.NAME(name);
        this.stream = stream;
        head = new HEAD(Arrays.copyOfRange(stream, 0, 12));
        info = new INFO(Arrays.copyOfRange(stream, 12, 36));
        data = new DATA(Arrays.copyOfRange(stream, 36, stream.length));
    }

    public File FILE() {
        return file;
    }

    public InputStream isf() throws IOException {
        return new FileInputStream(file);
    }
    class HEAD {
        final byte[] ID = new byte[]{0x52, 0x49, 0x46, 0x46};
        
        int size;

        byte[] sz;

        byte[] format;

        HEAD(byte[] hd, int len) {
            size = len;

            sz = Convert.BYTE_ARR(size, 1);

            format = hd;
        }

        HEAD(byte[] hd) {
            sz = Arrays.copyOfRange(hd, 4, 8);

            size = Convert.INT(sz, 1);

            format = Arrays.copyOfRange(hd, 8, 12);
        }

        @Override
        public String toString() {
            return "ID : " + Convert.CHAR(ID, 0) + "  FILE_SIZE : " +
                    (size * 1.0 / 1000) + "KB { " + Convert.HEX(sz, 1, 0, 16) +
                    "}   FORMAT : " + Convert.CHAR(format, 0);
        }
    }

    class INFO {
        final byte[] ID  = new byte[]{0x66, 0x6d, 0x74, 0x20};

        int length;

        byte[] len;

        byte[] info;

        INFO(byte[] inf, int lengt) {
            length = lengt;

            len = Convert.BYTE_ARR(lengt, 1);

            System.out.println("3435 "+Arrays.toString(len));

            info = inf;
        }

        INFO(byte[] inf) {
            len = Arrays.copyOfRange(inf, 4, 8);

            length = Convert.INT(len, 1);

            info = Arrays.copyOfRange(inf, 8, 8 + length);
        }
        @Override
        public String toString(){
            return "ID : {" + Convert.CHAR(ID, 0) + "}  FILE_SIZE : " +
                    (length * 1.0 / 1000) + "KB { " + Convert.HEX(len, 1, 0, 32) +
                    "}   INFO : " + Convert.HEX(info, 0, 2, 32);
        }
    }

    class DATA {
        final byte[] ID  = new byte[]{0x64, 0x61, 0x74, 0x61};

        int length;

        byte[] len;

        byte[] data;

        final int index;

        DATA(int index, int lengt) {
            this.index = index;

            length = lengt;

            len = Convert.BYTE_ARR(lengt, 1);

            data = new byte[0];
        }

        DATA(byte[] dta) {
            len = Arrays.copyOfRange(dta, 4, 8);

            length = Convert.INT(len, 1);

            data = Arrays.copyOfRange(dta, 8, 8 + length);
            index = 0;
        }
        public boolean create_file(){
            String separate = System.getProperty("file.separate");
            String path = separate + "wav.data" + separate + file_name ;
            boolean res = false;
            try {
               File ft = new File(path);
               if(!ft.exists()){
                    res = ft.mkdir();
                    path = path +separate +"data.dat";
                    ft = new File(path);
                    if(!ft.exists())res = ft.createNewFile();
                    
               }
            }catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        @Override
        public String toString(){
            return "ID : {" + Convert.CHAR(ID, 0) + "}  FILE_SIZE : " +
                    (length * 1.0 / 1000) + "KB { "+ Convert.HEX(len, 1, 0, 16) +
                    "}\nSTREAM : " + index;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HEAD : [ " + head + "]\n\n");
        sb.append("INFO : [ " + info + "]\n\n");
        sb.append("DATA : [ " + data + "]\n\n");
        return sb.toString();
    }
}