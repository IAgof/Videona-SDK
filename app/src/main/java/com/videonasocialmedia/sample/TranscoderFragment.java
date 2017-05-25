package com.videonasocialmedia.sample;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.videonasocialmedia.transcoder.MediaTranscoder;


import com.videonasocialmedia.transcoder.MediaTranscoder.MediaTranscoderListener;

import com.videonasocialmedia.transcoder.video.format.VideonaFormat;

import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.pipeline.TranscoderHelper;
import com.videonasocialmedia.videonamediaframework.pipeline.TranscoderHelperListener;
import com.videonasocialmedia.videonamediaframework.utils.TextToDrawable;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;
import static com.videonasocialmedia.sample.MainActivity.externalDir;
import static com.videonasocialmedia.sample.MainActivity.tempDir;

/**
 * Activities that contain this fragment must implement the
 * {@link TranscoderFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TranscoderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TranscoderFragment extends Fragment {
  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_PARAM1 = "param1";
  private static final String ARG_PARAM2 = "param2";
  // The request code must be 0 or greater.
  private static final int PLUS_ONE_REQUEST_CODE = 0;

  // TODO: Rename and change types of parameters
  private String mParam1;
  private String mParam2;

  private OnFragmentInteractionListener mListener;

  private static final String TAG = "TranscoderActivity";
  private static final int REQUEST_CODE_TRIM_VIDEO = 1;
  private static final int REQUEST_CODE_TRANSCODE_VIDEO = 2;
  private static final int REQUEST_CODE_OVERLAY_VIDEO = 3;
  private static final int REQUEST_CODE_FADE_INOUT_AUDIO = 4;
  private static final int PROGRESS_BAR_MAX = 1000;
  private ListenableFuture<Void> listenableFuture;


  @BindView(R.id.btnMixAudio) Button mixAudio;
  @BindView(R.id.textViewConsoleInfo) TextView textViewInfoProgress;
  @BindView(R.id.cancel_button) Button cancelButton;
  @BindView(R.id.transcode_trim_video) Button trimVideo;
  @BindView(R.id.transcode_video) Button transcodeVideo;
  @BindView(R.id.transcode_add_overlay_video) Button overlayVideo;
  @BindView(R.id.transcode_audio_fade_in_out) Button fadeInOutAudio;
  @BindView(R.id.transcode_video_fade_in_out) Button fadeInOutVideo;
  @BindView(R.id.progress_bar) ProgressBar progressBar;

  String outputAudioFadeInOut = externalDir + File.separator + "AudioFadeInOut_" +
      System.currentTimeMillis() + ".m4a";

  Drawable fadeTransition;
  boolean isVideoFadeActivated;
  boolean isAudioFadeActivated = false;
  int rotation;

  public TranscoderFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param param1 Parameter 1.
   * @param param2 Parameter 2.
   * @return A new instance of fragment TranscoderFragment.
   */
  // TODO: Rename and change types and number of parameters
  public static TranscoderFragment newInstance(String param1, String param2) {
    TranscoderFragment fragment = new TranscoderFragment();
    Bundle args = new Bundle();
    args.putString(ARG_PARAM1, param1);
    args.putString(ARG_PARAM2, param2);
    fragment.setArguments(args);

    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      mParam1 = getArguments().getString(ARG_PARAM1);
      mParam2 = getArguments().getString(ARG_PARAM2);
    }
    fadeTransition = ContextCompat.getDrawable(this.getContext(), R.drawable.alpha_transition_black);
    isVideoFadeActivated = getFadeTransitionActivatedFromPreferences();
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_transcoder, container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();

  }

  // TODO: Rename method, update argument and hook method into UI event
  public void onButtonPressed(Uri uri) {
    if (mListener != null) {
      mListener.onFragmentInteraction(uri);
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnFragmentInteractionListener) {
      mListener = (OnFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context.toString()
          + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  @OnClick(R.id.transcode_trim_video)
  public void onClickTrimVideo(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_TRIM_VIDEO);
  }

  @OnClick(R.id.transcode_video)
  public void onClickTranscodeVideo(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_TRANSCODE_VIDEO);
  }

  @OnClick(R.id.transcode_add_overlay_video)
  public void onClickOverlayVideo(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_OVERLAY_VIDEO);
  }

  @OnClick(R.id.transcode_audio_fade_in_out)
  public void onClickFadeInOutAudio(){
    startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("video/*"),
        REQUEST_CODE_FADE_INOUT_AUDIO);
  }

  @OnClick(R.id.cancel_button)
  public void onClickCancelButton(){
    listenableFuture.cancel(true);
  }

  @OnClick(R.id.btnMixAudio)
  public void onClickMixAudio(){
    textViewInfoProgress.setText("Mezclando audio ...");
    mixAudio();
  }

  private boolean getFadeTransitionActivatedFromPreferences() {

    return true;
  }

  private void mixAudio() {

    String inputVideo = externalDir + File.separator + "input_video_1.mp4";
    String inputVideo2 = externalDir + File.separator + "input_video_2.mp4";

    String outputAudio = externalDir + File.separator + "AudioMixed_" + System.currentTimeMillis()
        + ".m4a";

    File inputFile1 = new File(inputVideo);
    if (!inputFile1.exists()) {
      try {
        textViewInfoProgress.setText("Copiando vídeo 1 a sdcard");
        Utils.copyResourceToTemp(getActivity(), externalDir, "input_video_1", R.raw.video_test_one, ".mp4");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    File inputFile2 = new File(inputVideo2);
    if (!inputFile2.exists()) {
      try {
        textViewInfoProgress.setText("Copiando vídeo 2 a sdcard");
        Utils.copyResourceToTemp(getActivity(), externalDir, "input_video_2", R.raw.video_test_two, ".mp4");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    listenableFuture = MediaTranscoder.getInstance().mixAudioTwoFiles(inputVideo, inputVideo2, 0.90f,
            tempDir, outputAudio, getDurationFile(inputVideo), new MediaTranscoderListener() {
              @Override
              public void onTranscodeSuccess(String outputFile) {
                textViewInfoProgress.setText("Success " + outputFile);
                textViewInfoProgress.setTextColor(Color.GREEN);
              }

              @Override
              public void onTranscodeProgress(String progress) {
                textViewInfoProgress.setText(progress);
                textViewInfoProgress.setTextColor(Color.BLUE);
              }

              @Override
              public void onTranscodeError(String error) {
                textViewInfoProgress.setText(error);
                textViewInfoProgress.setTextColor(Color.RED);
              }

              @Override
              public void onTranscodeCanceled() {

              }
            });
  }

  public static long getDurationFile(String filePath){
    long duration = 0;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    retriever.setDataSource(filePath);
    duration = Integer.parseInt(retriever.extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_DURATION));
    return duration*1000;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(resultCode == RESULT_OK) {

      String externalDir = String.valueOf(Environment.getExternalStoragePublicDirectory(
          Environment.DIRECTORY_MOVIES));

      final String inPath = getPath(getActivity(), data.getData());
      final Video videoToEdit = new Video(inPath);
      videoToEdit.setTempPath(externalDir);
      final String exportedPath = videoToEdit.getTempPath();
     // progressBar.setMax(PROGRESS_BAR_MAX);
      final long startTime = SystemClock.uptimeMillis();
      final TranscoderHelperListener listener = new TranscoderHelperListener() {
        @Override
        public void onSuccessTranscoding(Video video) {
          Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");
          Log.d(TAG, "transcoded file " + exportedPath);
          File file = new File(exportedPath);
          onTranscodeFinished(true, "transcoded file placed on " + file.getAbsolutePath());
          startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(file),
              "video/mp4"));
        }

        @Override
        public void onErrorTranscoding(Video video, String message) {
          onTranscodeFinished(false, "Transcoder error. " + inPath);
       }
      };

      MediaTranscoder mediaTranscoder = MediaTranscoder.getInstance();
      TextToDrawable drawableGenerator = new TextToDrawable(getContext());
      TranscoderHelper transcoderHelper = new TranscoderHelper(drawableGenerator, mediaTranscoder);

      switchButtonEnabled(true);

      switch (requestCode) {
        case REQUEST_CODE_TRIM_VIDEO:{

            videoToEdit.setStartTime(5000);
            videoToEdit.setStopTime(10000);

            transcoderHelper.generateOutputVideoWithTrimming(fadeTransition,
                isVideoFadeActivated,isAudioFadeActivated, videoToEdit, new VideonaFormat(),"", listener);

          break;
        }
        case REQUEST_CODE_TRANSCODE_VIDEO: {
          //Example adapt video to format
          String destFinalPath = new File(videoToEdit.getMediaPath()).getParent() + File.separator
              + "videoAdapted.mp4";
          try {
            transcoderHelper.adaptVideoWithRotationToDefaultFormat(videoToEdit, new VideonaFormat(),
                destFinalPath, rotation, fadeTransition, isVideoFadeActivated, listener);
          } catch (IOException e) {
            e.printStackTrace();
          }

          break;
        }
        case REQUEST_CODE_OVERLAY_VIDEO: {

          videoToEdit.setClipText("Overlay text");
          videoToEdit.setClipTextPosition("CENTER");
          //videoToEdit.setClipTextPosition(TextEffect.TextPosition.TOP.name());

          transcoderHelper.generateOutputVideoWithOverlayImage(fadeTransition, isVideoFadeActivated,
              isAudioFadeActivated, videoToEdit, new VideonaFormat(), "", listener);

          break;
        }

        case REQUEST_CODE_FADE_INOUT_AUDIO: {

          listenableFuture = MediaTranscoder.getInstance().audioFadeInFadeOutToFile(inPath, 500, 500,
                  tempDir, outputAudioFadeInOut, new MediaTranscoderListener() {
                    @Override
                    public void onTranscodeSuccess(String outputFile) {
                      textViewInfoProgress.setText("Success " + outputFile);
                      textViewInfoProgress.setTextColor(Color.GREEN);
                    }

                    @Override
                    public void onTranscodeProgress(String progress) {

                    }

                    @Override
                    public void onTranscodeError(String error) {

                    }

                    @Override
                    public void onTranscodeCanceled() {

                    }
                  });
          break;
        }
        default:
          super.onActivityResult(requestCode, resultCode, data);
      }
    }
  }


  /**
   * Get a file path from a Uri. This will get the the path for Storage Access
   * Framework Documents, as well as the _data field for the MediaStore and
   * other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @author paulburke
   */
  public static String getPath(final Context context, final Uri uri) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
        // ExternalStorageProvider
        if (isExternalStorageDocument(uri)) {
          final String docId = DocumentsContract.getDocumentId(uri);
          final String[] split = docId.split(":");
          final String type = split[0];

          if ("primary".equalsIgnoreCase(type)) {
            return Environment.getExternalStorageDirectory() + "/" + split[1];
          }

          // TODO handle non-primary volumes
        }
        // DownloadsProvider
        else if (isDownloadsDocument(uri)) {

          final String id = DocumentsContract.getDocumentId(uri);
          final Uri contentUri = ContentUris.withAppendedId(
              Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

          return getDataColumn(context, contentUri, null, null);
        }
        // MediaProvider
        else if (isMediaDocument(uri)) {
          final String docId = DocumentsContract.getDocumentId(uri);
          final String[] split = docId.split(":");
          final String type = split[0];

          Uri contentUri = null;
          if ("image".equals(type)) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
          } else if ("video".equals(type)) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
          } else if ("audio".equals(type)) {
            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
          }

          final String selection = "_id=?";
          final String[] selectionArgs = new String[] {
              split[1]
          };

          return getDataColumn(context, contentUri, selection, selectionArgs);
        }
      }
      // MediaStore (and general)
      else if ("content".equalsIgnoreCase(uri.getScheme())) {
        return getDataColumn(context, uri, null, null);
      }
      // File
      else if ("file".equalsIgnoreCase(uri.getScheme())) {
        return uri.getPath();
      }
    }

    return null;
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @param selection (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  public static String getDataColumn(Context context, Uri uri, String selection,
                                     String[] selectionArgs) {

    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {
        column
    };

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
          null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }


  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }



  private void onTranscodeFinished(boolean isSuccess, String toastMessage) {
   // progressBar.setIndeterminate(false);
   // progressBar.setProgress(isSuccess ? PROGRESS_BAR_MAX : 0);
    switchButtonEnabled(false);
    //Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_LONG).show();
  }

  private void switchButtonEnabled(boolean isProgress) {
    trimVideo.setEnabled(!isProgress);
    transcodeVideo.setEnabled(!isProgress);
    overlayVideo.setEnabled(!isProgress);
    fadeInOutAudio.setEnabled(!isProgress);
    mixAudio.setEnabled(!isProgress);
    fadeInOutVideo.setEnabled(!isProgress);
    cancelButton.setEnabled(isProgress);
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {
    // TODO: Update argument type and name
    void onFragmentInteraction(Uri uri);
  }

}
