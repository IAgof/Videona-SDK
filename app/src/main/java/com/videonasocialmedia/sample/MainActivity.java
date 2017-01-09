package com.videonasocialmedia.sample;

import android.Manifest;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.List;

import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements PlayerFragment.OnFragmentInteractionListener, TranscoderFragment.OnFragmentInteractionListener {

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;

  public static String externalDir = Environment.getExternalStoragePublicDirectory(
      Environment.DIRECTORY_MOVIES) + File.separator + "VideonaSDK";
  public static String tempDir = externalDir + File.separator + ".temp";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    // Give the TabLayout the ViewPager
    TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
    tabLayout.setupWithViewPager(mViewPager);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });

    Dexter.initialize(this);
    ButterKnife.bind(this);

    checkManifestPermission();
    checkPaths();

  }

  private void checkManifestPermission() {
    Dexter.checkPermissions(new MultiplePermissionsListener() {
      @Override
      public void onPermissionsChecked(MultiplePermissionsReport report) {/* ... */}

      @Override
      public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                     PermissionToken token) {/* ... */}
    }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  private void checkPaths() {
    File externalDirPath = new File(externalDir);
    if (!externalDirPath.exists()) {
      externalDirPath.mkdir();
    }

    File tempDirPath = new File(tempDir);
    if (!tempDirPath.exists()) {
      tempDirPath.mkdir();
    } else {
      Utils.cleanDirectory(tempDirPath);
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onFragmentInteraction(Uri uri) {
    //
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 2;
    private String tabTitles[] = new String[] { "Player", "Transcoder"};

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      if(position == 0){
        return new PlayerFragment().newInstance("Hola", "Adios");
      } else {
        if(position == 1){
          return new TranscoderFragment().newInstance("Hello", "Bye");
        }
      }
      return new PlayerFragment().newInstance("Hola", "Adios");
    }

    @Override
    public int getCount() {
      // Show PAGE_COUNT total pages.
      return PAGE_COUNT;
    }


    @Override
    public CharSequence getPageTitle(int position) {
      // Generate title based on item position
      return tabTitles[position];
    }
  }
}
