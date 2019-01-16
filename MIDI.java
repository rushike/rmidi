package rmidi;

import rmidi.Match;
import rmidi.Constant;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.*;

import rmidi.Convert;
import rmidi.FileFormatException;

public class MIDI{
    private File file;
    private FileInputStream fips; // file input stream for current file
    private int length = 0;
    private byte[] sz = new byte[4];
    private byte[] format_type = new byte[2];
    private int track_count_int = 0;
    private byte[] track_count = new byte[2];
    private int time_div_int = 0;
    private byte[] time_div = new byte[2];
    private File[] tracks_io;
    private Track[] tracks;

    // public MIDI(String file_name){
    //     file = new File(file_name);
    //     byte[] buf = new byte[4];
    //     int rt = 0;
    //     int itr = 0;
    //     try {
    //         fips = new FileInputStream(file);
    //         rt = fips.read(buf, 0, 4);
    //         itr += 4;
    //         if(Match.match(buf, Constant.HEAD_ID) != 0) throw new FileFormatException("FILE with invalid Head ID");
    //         rt = fips.read(sz, 0, 4);
    //         itr += 4;
    //         len = Convert.INT(sz, 1);
           
    //         rt = isf.read(format_type, 0, 2);
    //         itr += 2;

    //         rt = fips.read(track_count, 0, 2); //reading the track info
    //         itr += 2;

    //         if ((time_div[0] >>> 7) == 1 ) throw new FileFormatException("Time Division used is not currently supported."); 
            
    //         rt = fips.read(time_div, 0, 2); //reading the time division, throughout the midi file
    //         itr += 2;

    //         tracks = new Track[Convert.INT(track_count, 0)];
    //         ini_tracks();
    //     }catch(FileNotFoundException e){
    //         e.printStackTrace();
    //     }
    // }

    public MIDI(int format_type, int track_count, int time_div){
        track_count_int = track_count;
        time_div_int = time_div;
        this.time_div = Convert.to_fix_length(time_div, 2, 8);
        this.format_type = Convert.to_fix_length(format_type, 2, 8);
        this.track_count = Convert.to_fix_length(track_count, 2, 8);
        // System.out.println("Len L " + Arrays.toString(Convert.to_fix_length(format_type, 5, 8)));
        tracks = new Track[track_count];
        ini();
    }

    private void ini() {
        for(int i = 0; i < tracks.length; i++){
            tracks[i] = new Track(i);
        }
    } 

    public Track track(int track_no){
        if(track_no > tracks.length) throw new IndexOutOfBoundsException("track no is invalid");
        return tracks[track_no];
    }

    // private final void ini_tracks(){
    //     byte[] buffer = new byte[4];
    //     int len = 0;
    //     int rt;
    //     for(File f : tracks){
    //         rt = fips.read(buffer, 0, 4);
    //         if(Match.match(buffer, Constant.TRK_ID) != 0) throw new FileFormatException("FILE with invalid Track ID");
    //         rt = fips.read(buffer);
    //         len = Convert.INT(buffer, 0);
    //         f = File.createTempFile(prefix, suffix, directory);
    //     }
    // }

    public byte[] to_byte_array(){
        ArrayList<Byte> byte_list = new ArrayList<>();
            add_byte_array_to_data(byte_list, Constant.MThd);
            add_byte_array_to_data(byte_list, new byte[4]);
            add_byte_array_to_data(byte_list, format_type);
            add_byte_array_to_data(byte_list, track_count);
            add_byte_array_to_data(byte_list, time_div);
            for(Track e  : tracks){
                add_byte_array_to_data(byte_list, e.to_byte_array());
            }
            byte[] len_arr = Convert.to_fix_length(6, 4, 8);
            // System.out.println("Len L " + Arrays.toString(len_arr));
            for(int i = 4; i < 8; i++){
                byte_list.set(i, len_arr[i - 4]);
            }
            return Convert.bytelist_to_bytearray(byte_list);
    }

    private void add_byte_array_to_data(ArrayList<Byte> data, byte[] array){
        for(byte ele : array){
            data.add(ele);
        }length += array.length;
    }

    public void create_file(String file_name){
        String dir = System.getProperty("user.dir");
        String separator = System.getProperty("file.separator");
        File f = new File(dir + separator + file_name + ".mid");
        try {
            FileOutputStream fops = new FileOutputStream(f);
            fops.write(to_byte_array());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public class Track{
        private int id;
        private int length;
        
        ArrayList<Event> trk_event = new ArrayList<>();

        public Track(int id){
            this.id = id;
            init();
        }

        public void init(){
            trk_event.add(Event.MetaEvent(0, 0x58, null, 0x4, 0x2, 0x18, 0x8)); // Meta Event : Time Signature
            trk_event.add(Event.MetaEvent(0, 0x59, null, 0x0, 0x0)); // Meta Event : Key Signature
            trk_event.add(Event.ChannelEvent(0, 0xb, 0x0, 79, 0)); //Channel Event : Controller  Blank Select
            trk_event.add(Event.ChannelEvent(0, 0xc, 0, 0x0)); // Channel Event : Program Change Event
            trk_event.add(Event.ChannelEvent(0, 0xb, 0, 0x7, 0x64)); //Channel Event : Controller  Main Volume
            trk_event.add(Event.ChannelEvent(0, 0xb, 0, 0x0a, 0x40)); //Channel Event : Controller  Pan
            trk_event.add(Event.ChannelEvent(0, 0xb, 0, 0x5b, 0x00)); //Channel Event : Controller  External Effects Depth
            trk_event.add(Event.ChannelEvent(0, 0xb, 0, 0x5d, 0x00)); //Channel Event : Controller  formerly Chorus Depth
            trk_event.add(Event.MetaEvent(0, 0x21, null, 0x01, 0x00)); // Meta Event : Midi Port/ Should be prior to anyone
            trk_event.add(Event.MetaEvent(0, 0x2f, null)); //Meta Event : End of Track
        }

        public void push_note(int note_length, int channel_no, int note_val, int intensity){
            Event end = pop(); // Removing meta end of track
            //Do things here
            trk_event.add(Event.ChannelEvent(delta_time(note_length), 0x9, channel_no, note_val, intensity));
            trk_event.add(Event.MetaEvent(01, 0x2f, null)); //Meta Event : End of Track
        }

        public void close_note(int note_length, int channel_no, int note_val){
            Event end = pop(); // Removing meta end of track
            //Do things here
            trk_event.add(Event.ChannelEvent(delta_time(note_length), 0x8, channel_no, note_val, 30));
            trk_event.add(Event.MetaEvent(01, 0x2f, null)); //Meta Event : End of Track
        }

        public int delta_time(int note_length){
            if(note_length == 0) return 0;
            return time_div_int * note_length / note_length;
        }

        private Event pop(){
            return trk_event.remove(trk_event.size() - 1);
        }

        public byte[] to_byte_array(){
            ArrayList<Byte> byte_list = new ArrayList<>();
            add_byte_array_to_data(byte_list, Constant.TRK_ID);
            add_byte_array_to_data(byte_list, new byte[4]);
            for(Event e  : trk_event){
                add_byte_array_to_data(byte_list, e.to_byte_array());
            }
            byte[] len_arr = Convert.to_fix_length(len(), 4, 8);
            for(int i = 4; i < 8; i++){
                byte_list.set(i, len_arr[i - 4]);
            }
            byte[] res = new byte[byte_list.size()];
            return Convert.bytelist_to_bytearray(byte_list);
        }
        public int len(){
            int szn = 0;
            for(Event e : trk_event){
                szn += e.len();
            }return szn;
        }
        private void add_byte_array_to_data(ArrayList<Byte> data, byte[] array){
            for(byte ele : array){
                data.add(ele);
            }length += array.length;
        }
    }

    public static class Event{
        private int delta_time = 0;

        private String type = null;

        private byte event_id = 0;

        private byte meta_event_type = 0;

        private int length = 0;

        private ArrayList<Byte> data = new ArrayList<>();


        public Event(){
        }

        public static Event ChannelEvent(int delta_time, int event_id, int channel_no, int ... parameters){
            Event event = new Event();
            event.type = "CHANNEL";
            event.delta_time = delta_time;
            event.event_id = (byte)(((event_id & 0xf) << 4) + (channel_no & 0xf));
            for(int param : parameters) {
                event.data.add((byte)(param & 0xff));
            }
            return event;
        }

        public static Event MetaEvent(int delta_time, int meta_event_type, String text, int ... parameters){
            Event event = new Event();
            event.type = "META";
            event.delta_time = delta_time;
            event.event_id = (byte)0xff;
            event.meta_event_type = (byte)(meta_event_type & 0xff);
            if(text != null){
                byte[] text_byte = text.getBytes(StandardCharsets.US_ASCII);
                event.add_byte_array_to_data(event.data, text_byte);
            }else {
                event.add_int_array_to_data(event.data, parameters);
            }
            return event;
        }

        public static Event SysEvent(int delta_time, int event_id, int ... data){
            Event event = new Event();
            event.type = "Sys";
            event.delta_time = delta_time;
            event.event_id = (byte)(event_id & 0xff);
            event.add_int_array_to_data(event.data, data);
            return event;
        }

        public byte[] to_byte_array(){
            ArrayList<Byte> byte_list = new ArrayList<>(); 
            if(type.equalsIgnoreCase("CHANNEL")){ // Channel Event
                add_byte_array_to_data(byte_list, Convert.to_var_length(delta_time));
                add_byte_array_to_data(byte_list, Convert.to_var_length(event_id));
                byte_list.addAll(data);
            }else if(type.equalsIgnoreCase("META")){ //Meta Event
                add_byte_array_to_data(byte_list, Convert.to_var_length(delta_time));
                add_byte_array_to_data(byte_list, Convert.to_var_length(event_id));
                add_byte_array_to_data(byte_list, Convert.to_var_length(meta_event_type));
                add_byte_array_to_data(byte_list, Convert.to_var_length(data.size()));
                byte_list.addAll(data);
            }else { //Sys Event
                add_byte_array_to_data(byte_list, Convert.to_var_length(delta_time));
                add_byte_array_to_data(byte_list, Convert.to_var_length(event_id));
                add_byte_array_to_data(byte_list, Convert.to_var_length(data.size()));
                byte_list.addAll(data);
            }
            byte[] res = new byte[byte_list.size()];
            return Convert.bytelist_to_bytearray(byte_list);
        }
        public int len(){
            return data.size();
        }
        private void add_byte_array_to_data(ArrayList<Byte> data, byte[] array){
            for(byte ele : array){
                data.add(ele);
            }
            length = array.length;
        }

        private void add_int_array_to_data(ArrayList<Byte> data, int[] array){
            for(int ele : array){
                data.add((byte)(ele & 0xff));
            }length += array.length;
        }

    }

    public static class EventFormat{
        String name;
        int id;
        String datatype;
        int length;

        public EventFormat(String name, int id, String datatype, int length){
            this.name = name;
            this.id = id;
            this.datatype = datatype;
            this.length = length;
        }

        @Override
        public Strting toString(){
            return "{" + "Name : " + name +  ", ID : " + id + ", datatype : "+ datatype + ", length : " + length + " }";
        }

        

    }

    
} 


