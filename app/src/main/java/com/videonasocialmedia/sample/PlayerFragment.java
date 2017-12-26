package com.videonasocialmedia.sample;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.videonasocialmedia.videonamediaframework.model.media.Music;
import com.videonasocialmedia.videonamediaframework.model.media.Video;
import com.videonasocialmedia.videonamediaframework.playback.VideonaPlayerExo;
import com.videonasocialmedia.videonamediaframework.playback.VideonaPlayerExo2;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.videonasocialmedia.sample.MainActivity.externalDir;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PlayerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends Fragment implements VideonaPlayerExo.VideonaPlayerListener {
  // TODO: Rename parameter arguments, choose names that match
  // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
  private static final String ARG_PARAM1 = "param1";
  private static final String ARG_PARAM2 = "param2";

  // TODO: Rename and change types of parameters
  private String mParam1;
  private String mParam2;

  private OnFragmentInteractionListener mListener;

  @BindView(R.id.videona_player) VideonaPlayerExo2 videonaPlayer;


  public PlayerFragment() {
    // Required empty public constructor
  }



  /**
   * Use this factory method to create a new instance of
   * this fragment using the provided parameters.
   *
   * @param param1 Parameter 1.
   * @param param2 Parameter 2.
   * @return A new instance of fragment PlayerFragment.
   */
  // TODO: Rename and change types and number of parameters
  public static PlayerFragment newInstance(String param1, String param2) {
    PlayerFragment fragment = new PlayerFragment();
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
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_player, container, false);
    ButterKnife.bind(this, view);
    return view;
  }

  @Override
  public void onStart(){
    super.onStart();
    videonaPlayer.setListener(this);
  }

  @Override
  public void onResume(){
    super.onResume();
    videonaPlayer.onShown(getActivity());
    showPreviewWithVideoMusicAndVoiceOver();
    //showPreviewWithVideo();
  }

  @Override
  public void onPause() {
    super.onPause();
    videonaPlayer.onPause();
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

  @Override
  public void newClipPlayed(int currentClipIndex) {

  }

  public void showPreviewWithVideoMusicAndVoiceOver() {

    List<Video> movieList = new ArrayList<Video>();
    String videoPath = externalDir + File.separator + "video2min.mp4";
    File inputFile = new File(videoPath);
    if (!inputFile.exists()) {
      try {
        Utils.copyResourceToTemp(getActivity(), externalDir, "inputvideo", R.raw.inputvideo, ".mp4");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    String audioTest = externalDir + File.separator + "audio_test_one.m4a";
    File audioFile = new File(audioTest);
    if(!audioFile.exists()){
      try{
        Utils.copyResourceToTemp(getActivity(), externalDir, "audio_test_one", R.raw.audio_test_one, ".m4a");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    String audioTest2 = externalDir + File.separator + "audio_test_two.m4a";
    File audioFile2 = new File(audioTest2);
    if(!audioFile2.exists()){
      try{
        Utils.copyResourceToTemp(getActivity(), externalDir, "audio_test_two", R.raw.audio_test_two, ".m4a");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Video videoShare = new Video(videoPath, 0.6f);
    movieList.add(videoShare);




    videonaPlayer.setVoiceOver(new Music(audioTest2, 0.4f, 0));
    videonaPlayer.bindVideoList(movieList);
    videonaPlayer.setMusic(new Music(audioTest, 0.3f, 0));

  }

  public void showPreviewWithVideo() {

    List<Video> movieList = new ArrayList<Video>();
    String videoPath = externalDir + File.separator + "inputvideo.mp4";
    File inputFile = new File(videoPath);
    if (!inputFile.exists()) {
      try {
        Utils.copyResourceToTemp(getActivity(), externalDir, "inputvideo", R.raw.inputvideo, ".mp4");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Video videoShare = new Video(videoPath, 1f);
    movieList.add(videoShare);

    videonaPlayer.bindVideoList(movieList);
    videonaPlayer.seekTo(0);

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
