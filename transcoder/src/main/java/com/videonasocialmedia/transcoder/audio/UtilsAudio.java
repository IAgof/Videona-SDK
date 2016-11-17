/*
 * Copyright (c) 2015. Videona Socialmedia SL
 * http://www.videona.com
 * info@videona.com
 * All rights reserved
 */

package com.videonasocialmedia.transcoder.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class UtilsAudio {

    // Create
    final public static String PATH_APP = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES) + File.separator + "AudioEngine";

    // 16 bits
    public static final int RECORDER_BPP = 16;
    // 48KHz
    public static final int RECORDER_SAMPLERATE = 48000;
    // 1 canal, mono
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO; //AudioFormat.CHANNEL_IN_MONO;
    // Num channels
    public static final int RECORDER_NUM_CHANNELS = 1; // 2
    // PCM
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static int bufferSize = 0;


    public static void copyWaveFile(String inFilename, String outFilename){

        // Configuración del buffer temporal donde se almacena el archivo de audio pcm
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = RECORDER_NUM_CHANNELS; //1; // Original 2, convierte de mono a stereo
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {

            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Write Wave Header 44 bytes
    private static void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
                                            int channels, long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }


    /** Future use

    // add external subtitle track to mp4 file
    public void subtitle() {

        try {

            //String filePath = Constants.PATH_APP + File.separator  + "/sample1.mp4";
            String filePath = Environment.getExternalStorageDirectory() + "/sample1.mp4";
            Movie countVideo = MovieCreator.build(filePath);

            // SubTitleを追加
            TextTrackImpl subTitleEng = new TextTrackImpl();
            subTitleEng.getTrackMetaData().setLanguage("eng");

            subTitleEng.getSubs().add(new TextTrackImpl.Line(0, 1000, "Five"));
            subTitleEng.getSubs().add(new TextTrackImpl.Line(1000, 2000, "Four"));
            subTitleEng.getSubs().add(new TextTrackImpl.Line(2000, 3000, "Three"));
            subTitleEng.getSubs().add(new TextTrackImpl.Line(3000, 4000, "Two"));
            subTitleEng.getSubs().add(new TextTrackImpl.Line(4000, 5000, "one"));
            countVideo.addTrack(subTitleEng);

            // 出力
            Container container = new DefaultMp4Builder().build(countVideo);
            //String outputFilePath = Constants.PATH_APP + File.separator  + "/output_subtitle.mp4";
            String outputFilePath = Environment.getExternalStorageDirectory() + "/output_subtitle.mp4";
            FileOutputStream fos = new FileOutputStream(outputFilePath);
            FileChannel channel = fos.getChannel();
            container.writeContainer(channel);
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();

        }

    }

     */

    public static void cleanDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) { //some JVMs return null for empty dirs
                for (File f : files) {
                    if (f.isDirectory()) {
                        cleanDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
    }

}
