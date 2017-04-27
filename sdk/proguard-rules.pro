# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/alvaromartinez/android-sdk-linux/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class com.videonasocialmedia.videonamediaframework.model.VMComposition { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.model.Constants { public *;}

-keep class com.videonasocialmedia.videonamediaframework.model.media.Music { public *;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.Audio { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.model.licensing.License { public *;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.transitions.Transition { public *;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.Video { public  *; }
-keep class com.videonasocialmedia.videonamediaframework.model.media.Media { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.Profile { public <methods>;}

-keep class com.videonasocialmedia.videonamediaframework.model.media.effects.Effect { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.effects.TextEffect$** { public *;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.effects.ShaderEffect { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.effects.OverlayEffect { public <methods>;}

-keep class com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalItemOnTrack
-keep class com.videonasocialmedia.videonamediaframework.model.media.exceptions.IllegalOrphanTransitionOnTrack

-keep class com.videonasocialmedia.videonamediaframework.model.media.utils.** { *; }

-keep class com.videonasocialmedia.videonamediaframework.model.media.track.AudioTrack { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.track.MediaTrack { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.model.media.track.Track { public <methods>;}

-keep class com.videonasocialmedia.videonamediaframework.utils.TextToDrawable { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.utils.TimeUtils { public <methods>;}

-keep class com.videonasocialmedia.videonamediaframework.playback.VideonaPlayerExo** { *;}
-keep interface com.videonasocialmedia.videonamediaframework.playback.VideonaPlayer { *;}
-keep interface com.videonasocialmedia.videonamediaframework.playback.VideonaPlayer$* { *; }
-keep class com.videonasocialmedia.videonamediaframework.playback.customviews.AspectRatioVideoView

-keep class com.videonasocialmedia.videonamediaframework.pipeline.ApplyAudioFadeInFadeOutToVideo { public *;}
-keep class com.videonasocialmedia.videonamediaframework.pipeline.ApplyAudioFadeInFadeOutToVideo$* { public *;}
-keep class com.videonasocialmedia.videonamediaframework.pipeline.AudioCompositionExportSession { public *;}
-keep class com.videonasocialmedia.videonamediaframework.pipeline.AudioCompositionExportSession$* { public *;}
-keep class com.videonasocialmedia.videonamediaframework.pipeline.AudioMixer { public <methods>;}
-keep interface com.videonasocialmedia.videonamediaframework.pipeline.AudioMixer$OnMixAudioListener { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.pipeline.TranscoderHelper { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.pipeline.TranscoderHelperListener { public <methods>;}
-keep class com.videonasocialmedia.videonamediaframework.pipeline.VMCompositionExportSessionImpl { public <methods>;}
-keep interface com.videonasocialmedia.videonamediaframework.pipeline.VMCompositionExportSession { *; }
-keep interface com.videonasocialmedia.videonamediaframework.pipeline.VMCompositionExportSession$* { *; }

-keep class com.videonasocialmedia.videonamediaframework.muxer.Appender { public <methods>;}

-keep class com.videonasocialmedia.transcoder.MediaTranscoder { public <methods>;}
-keep interface com.videonasocialmedia.transcoder.video.format.MediaFormatStrategy { public <methods>; }
-keep class com.videonasocialmedia.transcoder.video.overlay.Overlay { public <methods>; }
#-keep interface com.videonasocialmedia.transcoder.MediaTranscoderListener { public <methods>;} // fixme: seems to not exist
-keep interface com.videonasocialmedia.transcoder.MediaTranscoder$MediaTranscoderListener { public <methods>; }
-keep class com.videonasocialmedia.transcoder.video.format.VideonaFormat { public <methods>;}
-keep class com.videonasocialmedia.transcoder.video.overlay.Image { public <methods>;}

-keep class com.videonasocialmedia.transcoder.video.overlay.Filter { public <methods>;}
-keep class com.videonasocialmedia.transcoder.audio.listener.** { public <methods>;}


-keepattributes Exceptions, InnerClasses
