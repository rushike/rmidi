package com.midi;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;


public class Midi{
    private int file_length = 0;
    public byte[] file = new byte[file_length];

	int index = 0;

	int MTdh_length;

    private int tracks;

	private int  format;

    private int time_div;

	public Track[] track;

	//Wrappers
    protected byte WRAP_DATA = 0x7f;
    protected byte WRAP_DATA_BITS = 0x7;

	public Midi() {
	    build_head_chunk();
	    track = new Track[tracks];
	    for(int i = 0; i < tracks; i++) {
	        track[i] = new Track();
        }
    }
    public Midi(String path) {
	    try {
        file = Files.readAllBytes(Paths.get(path));
        file_length = file.length;
	    }catch (IOException e){
	        e.printStackTrace();
        }
        try {
	        allocate_midi();
        }catch (FileFormatException e) {
	        e.printStackTrace();
        }
    }

    public void head_chunk(byte[] hdck) {
	    MTdh_length = hdck.length;
	    int i = 0;
	    //format MTrk_length
        format = (int)to_hex(Arrays.copyOfRange(hdck, 0, 2), 8);

        //tracks
        tracks = (int)to_hex(Arrays.copyOfRange(hdck, 2, 4), 8);

        //time_div
        time_div = (int)to_hex(Arrays.copyOfRange(hdck, 4, 6), 8);
    }

	private byte[] build_head_chunk() {
		byte[] head_chunk = new byte[14];

		//defining ID 4 bytes, same always , start of every midi file
		head_chunk[0] = 0x4d;
		head_chunk[1] = 0x54;
		head_chunk[2] = 0x68;
		head_chunk[3] = 0x64;

		//header chunk MTrk_length : len  4 bytes: | range 0 to 16**4
		head_chunk[4] = 0x00;
		head_chunk[5] = 0x00;
		head_chunk[6] = 0x00;
		head_chunk[7] = 0x06; // may not be constant 6 always
        MTdh_length = (int)to_hex(Arrays.copyOfRange(head_chunk, 4, 8),8);

		//format len : 2 bytes
		head_chunk[8] = 0x00;
		head_chunk[9] = 0x01; // can be user given
        format = (int)to_hex(Arrays.copyOfRange(head_chunk, 8, 10),8);

		//tacks len : 2 bytes
		head_chunk[10] = 0x00;
		head_chunk[11] = 0x01; //can be user given
        tracks = (int)to_hex(Arrays.copyOfRange(head_chunk, 10, 12),8);

		//time division... // not fully understand yet
		head_chunk[12] = 0x01;
		head_chunk[13] = (byte)0xe0; // may user given, current values from song complicated rushi scorpion cover
        time_div = (int)to_hex(Arrays.copyOfRange(head_chunk, 12, 14),8);

		return head_chunk;
	}

	private byte[] build_ini_track_chunk() {
		byte tc[] = new byte[60];

		//defining ID 4 bytes, same always , every track start with id
		tc[0] = 0x4d;
		tc[1] = 0x54;
		tc[2] = 0x72;
		tc[3] = 0x6b;

		//track chunk MTrk_length : 4 bytes: | range 0 - 16**4
		tc[7] = 0x32; //(size & 0xff);
		tc[6] = 0x0; //((size = size >>> 8) & 0xff);
		tc[5] = 0x0; //((size = size >>> 8) & 0xff);
		tc[4] = 0x0; //((size = size >>> 8) & 0xff);

		//Now need to write events
		//General format <ID> <len> <midi - event> ... <midi - event>
		//Building on track setting of complicated, cover rushi scorpion
		
		tc[8] = 0x0; // delta time+


		//Setting meta-events
		//always start with 0xff
		//format <0xff> <type> <len> <data>

		//Time Signiture
		tc[9] = (byte)0xff;  //indicating meta event
		tc[10] = 0x58; //indicating time signiture
		tc[11] = 0x04; //indiacting further no. of bytes for parameters
		tc[12] = 0x04; //numerator of signiture
		tc[13] = 0x02; //denominator exponent, actual denominator 2**expo
		tc[18] = 0x18; //represents no. of clock signal to pass between two successive metronome tick
		tc[19] = 0x08; //not understood clearly, but 0x8 is default

		tc[20] = 0x00;
		
		//Key Signiture
		tc[21] = (byte)0xff; // indicating meta event
		tc[22] = 0x59; // indicating key signiture
		tc[23] = 0x02; // indiacting further no. of bytes for parameters
		tc[24] = (byte)0xff; // indicating no. of sharp(+) and flat(-) keys
		tc[25] = 0x00; // indicating scale type e.g. 0 for major, 1 for minor and so on

		tc[26] = 0x00;

		//Tempo
		tc[27] = (byte)0xff; //indicating meta event
		tc[28] = 0x51; //indcating tempo
		tc[29] = 0x03; //indiacting further no. of bytes for parameters
		//rest feild is 3 byte long, representing in big-endian form in 3[] array 
		//setting to 100bpm
		tc[30] = 0x09;
		tc[31] = 0x27;
		tc[32] = (byte)0xc0;

		tc[33] = 0x00;

		//Midi channel event

		//1. Controller Event
		tc[34] = (byte)0xb0; // indicating controller event(0xb) on midi channel(0x0)
		tc[35] = 0x79; //controller type - Mode Messages
		tc[36] = 0x00; //value

		tc[37] = 0x00;

		//2. Program change event
		tc[38] = (byte)0xc0; //indicating program change event(0xc) on  on midi channel(0x0)
		tc[39] = 0x00; //indicating program number

		tc[40] = 0x00;

		//3. Controller Event 
		tc[41] = (byte)0xb0; // indicating controller event(0xb) on midi channel(0x0)
		//Control table - multiple control event

		tc[42] = 0x07; //controller type - main volume
		tc[43] = 0x64; //value

		tc[44] = 0x00;//specifying end

		tc[45] = 0x0a; //controller type - pan(left - right balance)
		tc[46] = 0x40; // value 
		
		tc[47] = 0x00;

		tc[48] = 0x5b; //controller type - reverb amount
		tc[49] = 0x00; //value 0x0 or 0x7f

		tc[50] = 0x00;

		tc[51] = 0x5d; //controller type - chorus 
		tc[52] = 0x00; //value  0x0 or 0x7f

		tc[53] = 0x21; //controller type - modulation wheel lsb
		tc[54] = 0x01; //value 

		tc[55] = (byte)0x00;

		//END TRACK
        tc[56] = 0x01;//Dont know
        tc[57] = (byte)0xff; // indicating meta event
        tc[58] = (byte)0xff; // indicating end of track
        tc[59] = 0x00; // indicating O MTrk_length event
        return tc;
	}

	public void add_note(int track_no,long pre_time, long time, int note_number, int velocity) {
        track[track_no - 1].add_note_train(pre_time, time, note_number, velocity);
    }

    public byte[] make_midi() {
	    int i = 0, j = 0, k = 0;
	    int len = MTdh_length + 8;
	    while(i < track.length) {
	        len = len + track[i++].get_track().length;
        }i = 0;
	    file = new byte[len];
	    byte[] hcnk = build_head_chunk();
	    while(j < MTdh_length + 8) {
	        file[i++] = hcnk[j++];
	    }j = 0;
	    while(j < track.length) {
	        hcnk = track[j++].get_track();
	        k = 0;
	        while(k < hcnk.length) {
	            file[i++] = hcnk[k++];
            }
        }return file;
    }

    public synchronized void  create_midi_file(String path, String file_name) {
        FileOutputStream fos = null;
	    if(file_length == 0) make_midi();
        String fs = System.getProperty("file.separator");
        if(path.length() == 0) {
            path = fs + "users" +fs;
            //create dir midi
            if((new File(path + "midi")).getParentFile().mkdir())path = path + fs;
            else ;

        }else path = path + fs;
        if(file_name == null) path = path + "midi" + name()+ ".midi";
        else path = path + "midi" + file_name+ ".midi";
        Main.pln("path : " + path );
        try {
            fos = new FileOutputStream(path);
            fos.write(file);
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fos!= null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public void allocate_midi() throws FileFormatException{
	    byte[] MThd = new byte[]{0x4d, 0x54, 0x68, 0x64};
	    int i = 0;
	    if(match(file, MThd, 0 , 4) == -1) {
	        throw new FileFormatException("File is not midi : Head do not have MTdh");
        }i = i + 4;
	    int Mthd_length = (int)to_hex(Arrays.copyOfRange(file, i, (i = i + 4)), 8);

	    head_chunk(Arrays.copyOfRange(file, i, i = i + Mthd_length));

	    int j = 0; //track iterator
        track = new Track[tracks];
        byte[] MTrk = new byte[]{0x4d, 0x54, 0x72, 0x6b};
        while(j < tracks) {
            if(match(file, MTrk, i , i = i + 4 ) == -1) {
                throw new FileFormatException("File Not midi : Track{"+ j +"}" + " do not have MTrk :" );
            }
            int MTrk_length = (int)to_hex(Arrays.copyOfRange(file, i, i = i + 4), 8);
            track[j++] = new Track(Arrays.copyOfRange(file, i, i = i + MTrk_length));
        }

    }

    public String name() {
	    return (new SimpleDateFormat("yyyy-MM-dd-HH-mm")).format(new Date());

    }

    public static byte[] scale(String scl) {
	    byte[] sca;
	    switch(scl) {
	        case "major":
                sca = new byte[]{0, 2, 4, 5, 7, 9, 11, 12};
                return sca;
            case "minor":
                sca = new byte[]{0, 2, 3, 5, 7, 8, 10, 12};
                return sca;
            default:
                sca = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9 ,10, 11, 12};
                return sca;
        }
    }

    public Track get_track(int track_no) {
	    if(track_no < tracks + 1){
	        return track[track_no - 1];
        }
	    return null;
    }

    public long to_hex(byte[] num, int bits) {
        int i = -1;
        long hex = 0x0;
        while(++i < num.length) {
            hex = hex << bits;
            hex = hex + (num[i] & ((1 << bits) - 1 ));
        }return  hex;
    }

    public long var_to_hex(byte[] var) {
	    int i = -1;
	    long hex = 0x0;
	    while(++i < var.length) {
            hex = hex << 0x7;
	        hex = hex + (var[i] & 0x7f);
        }return  hex;
    }

    public byte[] to_var_length(long k) {
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

    public byte[] to_fix_length(long k, int len, int bits) {
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
            fix = new byte[4];
            fix[3] = (byte)k;
        }return fix;

    }

    public int length(long k) {
	    return (int)(Math.log(k) / Math.log(2) + 1);
    }

    char[] digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public String to_hex_string_byte(byte[] var) {
	    String s = "[ ";
	    int i = 0;
	    int vart;
	    while(i < var.length) {
            vart = var[i++] & 0xff;
	        s = s + digit[vart >>> 4] + digit[vart & 0xf] + ", ";
        }s = s + "]";
	    return s;
    }

    public String to_hex_string(long num, int len) {
        StringBuilder s = new StringBuilder();
	    long i = num;
	    if(num < (1>>>32)) i = num & 0xffffffffL;
	    do{
	        s.insert(0, digit[(int)(i & 0xf)]);
        }while((i = i >>> 4) > 0);
	    if(s.length() == 1) {
	        s.insert(0,"0");
        }
        if(len == 0) return s.toString(); {
            if(len < s.length()) return s.substring(s.length() - len);
            else {
                int diff = len - s.length();
                while(--diff > -1) s.insert(0, "0");
            }
        }return s.toString();
    }


    public String to_string() {
        return "chunk MTrk_length : " + MTdh_length + ", tracks : "  + tracks + ", format : " + format + ", time division : " +time_div;
    }

    public String to_string(Track.Note[] art) {
        return art[0].to_String(art);
    }

    public String toString() {
        String s = "    Address |    00   01   2   03   04   05   06   07   08  09   0a   0b   0c   0d   0e   0f\n";
        StringBuilder sb = new StringBuilder(s);
        int i = 0;
        while(i < file.length ) {
            if(i % 16 == 0) sb.append("\n" + to_hex_string((i)/16, 11) + " | ");
            sb.append("   " + to_hex_string(file[i++], 2));
        }return sb.toString();
    }

    public String to_string(byte[] trk, int len) {
        String s = "    Address |    00   01   2   03   04   05   06   07   08  09   0a   0b   0c   0d   0e   0f\n";
        StringBuilder sb = new StringBuilder(s);
        int i = 0;
        while(i < len ) {
            if(i % 16 == 0) sb.append("\n" + to_hex_string((i)/16, 11) + " | ");
            sb.append("   " + to_hex_string(trk[i++], 2));
        }return sb.toString();
    }
    public class Track{
        /**
         * Unique ID in Midi File to identify the track start
         */
	    byte[] ID = {0x4d, 0x54, 0x72, 0x6b};

        /**
         * MTrk_length of track, includes all events + notes info. 4 byte memory space
         */
	    byte[] MTrk_array = new byte[4];

        /**
         * integer representation of len
         */
        int MTrk_length;

        /**
         * maintains events info
         */
	    byte[] event;

        /**
         * stack pointer to current MTrk_length, always event_length < event.MTrk_length
         */
	    int event_length = 0;

        /**
         * memory block to store note, with  there time intervals and velocity
         */
	    byte[] noter = new byte[64];

        /**
         * stack pointer to current MTrk_length, always noter_length < noter.MTrk_length
         */
        int noter_length = 0;


	    byte[] meta_end = {0x01, (byte)0xff, 0x2f, 0x0};

	    public Track(){
            int i = 0;
            event = new byte[60];
	        event[i++] = 0x0; // delta time+


            //Setting meta-events
            //always start with 0xff
            //format <0xff> <type> <len> <data>

            //Time Signiture
            event[i++] = (byte)0xff;  //indicating meta event
            event[i++] = 0x58; //indicating time signiture
            event[i++] = 0x04; //indiacting further no. of bytes for parameters
            event[i++] = 0x04; //numerator of signiture
            event[i++] = 0x02; //denominator exponent, actual denominator 2**expo
            event[i++] = 0x18; //represents no. of clock signal to pass between two successive metronome tick
            event[i++] = 0x08; //not understood clearly, but 0x8 is default

            event[i++] = 0x00;

            //Key Signiture
            event[i++] = (byte)0xff; // indicating meta event
            event[i++] = 0x59; // indicating key signiture
            event[i++] = 0x02; // indiacting further no. of bytes for parameters
            event[i++] = (byte)0xff; // indicating no. of sharp(+) and flat(-) keys
            event[i++] = 0x00; // indicating scale type e.g. 0 for major, 1 for minor and so on

            event[i++] = 0x00;

            //Tempo
            event[i++] = (byte)0xff; //indicating meta event
            event[i++] = 0x51; //indcating tempo
            event[i++] = 0x03; //indiacting further no. of bytes for parameters
            //rest feild is 3 byte long, representing in big-endian form in 3[] array
            //setting to 100bpm
            event[i++] = 0x09;
            event[i++]= 0x27;
            event[i++] = (byte)0xc0;

            event[i++] = 0x00;

            //Midi channel event

            //1. Controller Event
            event[i++] = (byte)0xb0; // indicating controller event(0xb) on midi channel(0x0)
            event[i++] = 0x79; //controller type - Mode Messages
            event[i++] = 0x00; //value

            event[i++] = 0x00;

            //2. Program change event
            event[i++] = (byte)0xc0; //indicating program change event(0xc) on  on midi channel(0x0)
            event[i++] = 0x00; //indicating program number

            event[i++] = 0x00;

            //3. Controller Event
            event[i++] = (byte)0xb0; // indicating controller event(0xb) on midi channel(0x0)
            //Control table - multiple control event

            event[i++] = 0x07; //controller type - main volume
            event[i++] = 0x70; //value

            event[i++] = 0x00;//specifying end

            event[i++] = 0x0a; //controller type - pan(left - right balance)
            event[i++] = 0x40; // value

            event[i++] = 0x00;

            event[i++] = 0x5b; //controller type - reverb amount
            event[i++] = 0x00; //value 0x0 or 0x7f

            event[i++] = 0x00;

            event[i++] = 0x5d; //controller type - chorus
            event[i++] = 0x00; //value  0x0 or 0x7f

            event[i++] = 0x00;

            event[i++] = (byte)0xff;
            event[i++] = 0x21; //controller type - modulation wheel lsb
            event[i++] = 0x01; //value

            event[i++] = (byte)0x00;

            event_length = i;
        }

        public Track(byte[] trk) {
	        try {
	        MTrk_array = to_fix_length(trk.length, 4, 8);
	        MTrk_length = trk.length;
	        byte[] noter_ID = new byte[]{0x00, 0x09};
	        int noter_start = match(trk, noter_ID);
	        Main.pln("MAIN " + noter_start + " " + to_hex_string(noter_start, 6));
	        event = Arrays.copyOfRange(trk, 0, noter_start);
	        event_length = event.length;
	        noter = Arrays.copyOfRange(trk, noter_start, match(trk, meta_end));
	        noter_length = noter.length;
	        }catch (Exception e) {
	            e.printStackTrace();
            }
        }

        public Track(boolean fl) {

        }

        public void set(byte[] trk) {
            MTrk_array = to_fix_length(trk.length, 4, 8);
            MTrk_length = trk.length;
            byte[] noter_ID = new byte[]{0x00, 0x09};
            int noter_start = match(trk, noter_ID);
            event = Arrays.copyOfRange(trk, 0, noter_start);
            event_length = event.length;
            noter = Arrays.copyOfRange(trk, noter_start, match(trk, meta_end));
            noter_length = noter.length;
        }

        public void add_note(long time, int note_number, int velocity) {
            if(noter_length + 12 > noter.length) {
                byte[] res = new byte[2 * noter.length];
                for(int i = 0; i < noter.length; i++) {
                    res[i] = noter[i];
                }noter = res;
            }
            time = time * time_div / time_sign();
            byte[] delta_time = to_var_length(time); // variable MTrk_length
            int i = 0;
            while(i < delta_time.length) {
                noter[noter_length++] = delta_time[i++];
            }
            if(noter_length - delta_time.length < 1) {
                noter[noter_length++] = (byte)0x90; //indicating note on, event.
            }noter[noter_length++] = (byte)note_number;
            noter[noter_length++] = (byte)velocity;

        }

        public void add_note_train(long pre_time, long time, int note_number, int velocity){
            if(noter_length + 12 > noter.length) {
                byte[] res = new byte[2 * noter.length];
                for(int i = 0; i < noter.length; i++) {
                    res[i] = noter[i];
                }noter = res;
            }
	        pre_time = pre_time * time_div / time_sign();
            byte[] delta_time = to_var_length(pre_time); // variable MTrk_length
            int i = 0;
            while(i < delta_time.length) {
                noter[noter_length++] = delta_time[i++];
            }
	        if(noter_length -delta_time.length < 1) {
                noter[noter_length++] = (byte)0x90; //indicating note on, event.
            }noter[noter_length++] = (byte)note_number;
            noter[noter_length++] = (byte)velocity;
            time = time * time_div / time_sign();
            delta_time = to_var_length(time); // variable MTrk_length
            i = 0;
            while(i < delta_time.length) {
                noter[noter_length++] = delta_time[i++];
            }
            noter[noter_length++] = (byte)note_number;
            noter[noter_length++] = 0x0;
        }

        public void add_note_plane(long pre_time, long time, int note_number, int velocity) {

        }

        public Note[] get_note_array() throws FileFormatException{
	        int i = 0, j, time = 0, note_time, delta_time, len = 0;
	        Note[] art_note = new Note[noter_length / 2];
            ArrayList<Byte> note_arrlist = new ArrayList<>();
	        while(i < noter_length) {
	            //delta time
                j = 0;
                while(noter[j++] > 0x80);
                delta_time = (int)to_hex(Arrays.copyOfRange(noter, i, i = i + j), 7);
                time = time + delta_time;

                if(noter[i] == 0x90) i++;

                if(noter[i + 1]!= 0){
                    note_arrlist.add(noter[i]);
                    art_note[len].time = time;
                    art_note[len].note = noter[i++];
                    art_note[len].velocity = noter[i++];
                }
                else{
                    if(!note_arrlist.isEmpty()) {
                        if(note_arrlist.remove(new Byte(noter[i])));
                        else throw new FileFormatException("File is Midi , but note{" + noter[i] + " has no start.");
                    }
                    else throw new FileFormatException("File is Midi , but note{" + noter[i] + " has no start.");
                    art_note[len].note_time = time - art_note[len].time;
                    len++;
                }

	        }

            return art_note;
        }

        public void close_notes() {
            HashSet<Integer> hs = new HashSet<>();
            int i = 0;
            int value = 0, pvalue;
            boolean del = true, nt = false, in = false; // if delta chunk & note indicator, and incomplete not closed
            while(i < noter_length) {
                value = noter[i++] & 0xff;
                if(del) {
                    while (value > 0x80) {
                        value = noter[i++] & 0xff;
                    }
                    del = false;
                }if(value == 0x90) {
                    nt = true;
                }if(nt) {
                    nt = false;
                    pvalue = noter[i++] & 0xff;
                    if (pvalue != 0) {
                        hs.add(value);
                    } else {
                        hs.remove(value);
                    }
                }
            }
            if(!hs.isEmpty()) {
                Iterator<Integer> ir = hs.iterator();
                while(ir.hasNext()){
                    value = ir.next();
                    ir.remove();
                    add_note(00,value, 00);
                }
            }
        }

        public  int  time_sign() {
	        byte[] time_signiture_event = {0x0, (byte)0xff, 0x58};
	        int start =  match(event, time_signiture_event);
	        int num = event[start + 3];
	        int deno = 1 << event[start + 4];
	        return num * 64 / deno;
        }
        public byte[] get_track() {
	        MTrk_length = 4 + 4 + event_length + noter_length + meta_end.length;
	        byte[] track = new byte[MTrk_length];
	        MTrk_array = to_fix_length(MTrk_length - 8, 4, 8);
	        int i = 0, j = 0;
	        while(j < ID.length) {
	            track[i++] = ID[j++];
            }j = 0;
	        while(j < 4) {
	            track[i++] = MTrk_array[j++];
            }j = 0;
            while(j < event_length) {
                track[i++] = event[j++];
            }j = 0;
            while(j < noter_length) {
                track[i++] = noter[j++];
            }j = 0;
            while(j < meta_end.length) {
                track[i++] =meta_end[j++];
            }return track;
        }
        public void set_time_signiture(int num, int deno, int midi_clocks, int x) {

        }

        public String toString() {
	        StringBuilder sb = new StringBuilder();
	        sb.append("ID : " + "MTrk");
	        sb.append("\tlength : " + MTrk_length);
	        sb.append("\nEvent : \n");
	        for(int i = 0; i < event_length; i++) {
	            sb.append("\t" + to_hex_string(event[i], 2));
	            if((i & 0xf) == 0) sb.append("\n");
            }
            sb.append("\nNoter : \n");
            for(int i = 0; i < noter_length; i++) {
                sb.append("\t" + to_hex_string(noter[i], 2));
                if((i & 0xf) == 0) sb.append("\n");
            }
            sb.append("\nMetaEnd : \n");
            for(int i = 0; i < meta_end.length; i++) {
                sb.append("\t" + to_hex_string(event[i], 2));
                if((i & 0xf) == 0) sb.append("\n");
            }
	        return sb.toString();
        }

        public class Note {
	        int time;

	        int note_time;

            @Deprecated
            byte on;

	        byte note;

	        byte velocity;

	        public String toString() {
	            return "{ x:" + time + ",y: N: " + note + ", Nt: " + note_time + " V: " + velocity + " }";
            }

            public String to_String(Note[] art) {
	            StringBuilder sb = new StringBuilder("Noter : \n");
	            for(int i = 0; i < art.length; i++) {
	                sb.append(art[i]);
	                sb.append("\n");
                }return sb.toString();
            }
        }

    }



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