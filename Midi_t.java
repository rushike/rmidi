package com.midi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Midi_t {
    private int file_length = 0;

    public byte[] file = new byte[file_length];

    int MTdh_length;

    private int tracks;

    private int  format;

    private int time_div;

    public Track[] track;

    public Midi_t(String path) {
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
            if(match(file, MTrk, i ,  i + 4 ) == -1) {
                throw new FileFormatException("File Not midi : Track{"+ j +"}" + " do not have MTrk :" );
            }
            int MTrk_length = (int)to_hex(Arrays.copyOfRange(file, i + 4,  i + 8), 8);
            track[j++] = new Track(Arrays.copyOfRange(file, i, i = i + 8 + MTrk_length));
        }

    }

    public Track get_track(int trk) {
        return track[trk - 1];
    }

    /**
     *  Converts byte [] to integer
     * @param num byte[] big-endian representation
     * @param bits bits involue in data storage
     * @return long integer of num[]
     */
    public long to_hex(byte[] num, int bits) {
        int i = -1;
        long hex = 0x0;
        while(++i < num.length) {
            hex = hex << bits;
            hex = hex + (num[i] & ((1 << bits) - 1 ));
        }return  hex;
    }

    char[] digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Convert var byte array to hex-representation
     * @param var
     * @return hex string of var byte[]
     */
    public String to_hex_string(byte[] var) {
        String s = "[ ";
        int i = 0;
        int vart;
        while(i < var.length) {
            vart = var[i++] & 0xff;
            s = s + digit[vart >>> 4] + digit[vart & 0xf] + ", ";
        }s = s + "]";
        return s;
    }

    /**
     * Convert num to hexadecimal
     * @param num integer to be converted
     * @param len length of return hex-string
     * @return
     */
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

    /**
     * toString methods encodes midi in String as Hex Editor do in notepad++
     * @return
     */
    public String toString() {
        String s = "    Address |    00   01   2   03   04   05   06   07   08  09   0a   0b   0c   0d   0e   0f\n";
        StringBuilder sb = new StringBuilder(s);
        int i = 0;
        while(i < file.length ) {
            if(i % 16 == 0) sb.append("\n" + to_hex_string((i)/16, 11) + " | ");
            sb.append("   " + to_hex_string(file[i++], 2));
        }return sb.toString();
    }

    public class Track{
        /**
         * Unique ID in Midi File to identify the track start
         * MTrk = {0x4d, 0x54, 0x72, 0x6b}
         */
        byte[] ID = {0x4d, 0x54, 0x72, 0x6b};

        /**
         * MTrk_length of track, includes all events + notes info. 4 byte memory space
         * byte[] representation of track length
         */
        byte[] MTrk_array;

        /**
         * integer representation of track length
         */
        int MTrk_length;

        /**
         * maintains events info, Event class
         */
        Event[] event;

        /**
         * stack pointer to current MTrk_length, always event_length < event.MTrk_length
         */
        //int event_length = 0;

        int event_length = 0;

        @Deprecated
        private int PREV_TYPe;

        public Track(byte[] track) throws FileFormatException {
            //Alloacate event array space for at least 128 events initially
            event = new Event[128];

            int ptr = 0; //start with 0 index from track
            Main.pln("Track : " + to_hex_string(track));
            if(match(track, ID, 0, 4) != 0) throw new FileFormatException("Track don't start with ID : MTrk ");

            MTrk_array = Arrays.copyOfRange(track,  4, ptr = ptr + 8);
            MTrk_length = (int)to_hex(MTrk_array, 8);

            event_length = ptr - 8;
            //Event series start
            while(ptr < (MTrk_length + 8)) {
                ensure_event_size();
                event[event_length] = new Event();
                ptr = event[event_length++].set(track, ptr); //set pointer to next event delta-time
                Main.pln(  "evt :"+(event_length - 1) +".) "+event[event_length - 1].toString());
                Main.pln("");
            }

        }

        public int ensure_event_size(){
            if(event_length + 10 > event.length) {
                Event[] art = new Event[event_length * 2];
                for(int i = 0; i < event.length; i++) {
                    art[i] = event[i];
                }event = art;
            }
            return 1;
        }

        /**
         * ABSOLUTE method when called allocs AB_TIME for all notes present
         */
        public void ABSOLUTE() {
            Event[] cln = Arrays.copyOfRange(event, 0, event_length);
            HashMap<Integer, Event> map = new HashMap<>();
            int itr = -1;
            long clk = 0;
            Event trd;
            while (++itr< cln.length) {
                clk = cln[itr].TIME + clk;
                cln[itr].AB_TIME = (int)clk;

                if(cln[itr].get_type() > 0x8f && cln[itr].get_type() < 0xa0) {

                    if((cln[itr].evt[2] & 0xff)!= 0){
                        map.put((cln[itr].evt[1] & 0xff), cln[itr]);
                    }
                    else{
                        if(map.containsKey(cln[itr].evt[1] & 0xff)){
                            trd = map.remove(cln[itr].evt[1] & 0xff);
                            trd.INTERVAL = (int)(clk - trd.AB_TIME);
                        }
                        else{

                        }
                    }

                }else if(cln[itr].get_type() > 0x7f && cln[itr].get_type() < 0x90){
                    if(map.containsKey(cln[itr].evt[1] & 0xff)) {
                        trd = map.remove(cln[itr].evt[1] & 0xff);
                        trd.INTERVAL = (int)(clk - trd.AB_TIME);
                    }
                    else{

                    }
                }
            }
        }

        public Dimen[] to_note_graph() {
            ABSOLUTE();
            ArrayList<Dimen> dct = new ArrayList<>();
            Dimen ele;
            for(int i = 0; i < event_length; i++) {
                if(event[i].INTERVAL != 0) {
                    ele = (new Dimen(3));
                    ele.set_co(event[i].AB_TIME, event[i].get_note_number(), event[i].INTERVAL);
                    dct.add(ele);
                }
            }return dct.toArray(new Dimen[dct.size()]);
        }

        public class Event{
            /**
             * Delta - Time as Integer
             */
            private int TIME;

            /**
             * Delta -Time as variable length
             */
            byte[] time;

            /**
             * Absolute time as integer
             */
            @Deprecated
            int AB_TIME;

            @Deprecated
            int INTERVAL;

            /**
             * Stores event info in array
             */
            byte[] evt;

            int evt_length;

            @Deprecated
            private int len;

            private int type;


            /**
             *Assumes track start with delta time
             * @param track byte[] of track
             * @param ptr indicating current pointing or position of event in track
             */
            public int set(byte[] track, int ptr) throws FileFormatException {
                int ptr_j;

                if((ptr + 4) < track.length) ptr_j = time_length(Arrays.copyOfRange(track, ptr, ptr + 4));
                else ptr_j = time_length(Arrays.copyOfRange(track, ptr, track.length));
                time = Arrays.copyOfRange(track, ptr, ptr = ptr + ptr_j);
                TIME = (int)to_hex(time, 7);

                // meta event
                if((track[ptr] & 0xff) == 0xff) { //meta event
                    type = 0xff * 0x100 + (track[ptr + 1] & 0xff);
                    PREV_TYPe = type;
                    len = track[ptr + 2];
                    evt_length = ptr + len + 3;
                    evt = Arrays.copyOfRange(track, ptr, evt_length);
                    return evt_length;
                }

                else if((track[ptr] & 0xff) == 0xf0) { //Divided Sys-Event
                    type = 0xf0;
                    PREV_TYPe = type;
                    len = track[ptr + 1];
                    evt_length = ptr + len + 2;
                    evt = Arrays.copyOfRange(track, ptr, evt_length);
                    return evt_length;
                }
                else if((track[ptr] & 0xff) == 0xf7) { //Authorization Sys-Event
                    type = 0xf7;
                    PREV_TYPe = type;
                    len = track[ptr + 1];
                    evt_length = ptr + len + 2;
                    evt = Arrays.copyOfRange(track, ptr, evt_length);
                    return evt_length;
                }

                else if((track[ptr] & 0xff) > 0x7f && (track[ptr] & 0xff) < 0xf0) {//channel event
                    int parameters = 2; // parameter no. in channel event 1 or 2, default 2
                    type = track[ptr] & 0xff;
                    PREV_TYPe = type;
                    if(type > 0xbf && type < 0xe0) parameters = 1; //0xc(Program Change Event) and 0xd(AfterTouch Event) no. of parameter is 1
                    evt_length = ptr + parameters + 1; //event length is channel indicator byte + parameters
                    evt = Arrays.copyOfRange(track, ptr, evt_length);
                    return evt_length;
                }
                else{
                    if(PREV_TYPe < 0xf0) {
                        type = PREV_TYPe;
                        PREV_TYPe = type;
                        evt = new byte[]{(byte)type, track[ptr], track[ptr + 1] };
                        return ptr + 2;
                    }
                    throw new FileFormatException("Delta Time with no specified midi event : ");
                }
            }

            public int time_length(byte[] track) {
                for(int i = 0; i < track.length; i++){
                    if((track[i] & 0xff)< 0x80){
                        return i + 1;
                    }
                }
                return 1;
            }

            public int get_type() {
                return type;
            }

            public int get_note_number() {
                if(type == 0x90 || type == 0x80) {
                    return evt[1];
                }return  -1;
            }
            
            public int get_velocity() {
                if(type == 0x90 || type == 0x80) {
                    return evt[2];
                }return  -1;
            }
            
            public String toString() {
                StringBuilder sb = new StringBuilder("\nDelta-time : ");
                sb.append(to_hex_string(TIME, 4));
                if(type > 0x100 || type == 0xf0 || type == 0xf7 ) {
                    sb.append(", Len : " + len +"  ");
                }
                sb.append(", type :" + to_hex_string(type, 4));
                sb.append(",    Event : ");
                sb.append(to_hex_string(evt));
                return sb.toString();
            }

            public String toNoter() {
                return "AB_TIME : " + AB_TIME + ", NOTE : " + to_hex_string(evt[1], 2) + ", INTERVAL : " + INTERVAL;
            }

            @Override
            public boolean equals(Object obj) {
                Event cmp = (Event)obj;
                if(TIME != cmp.TIME) return false;
                if(type != cmp.type) return false;
                if(evt_length != cmp.evt_length) return false;
                int i = 0;
                while(i < evt_length) {
                    if(evt[i++] != cmp.evt[i++])return false;
                }return true;
            }

            @Override
            public int hashCode() {
                return TIME + type + (int)(10000 * Math.sin(TIME));
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
