/*
    The MIT License (MIT)
    Copyright (c) 2013 Vlad Stirbu
    
    Permission is hereby granted, free of charge, to any person obtaining
    a copy of this software and associated documentation files (the
    "Software"), to deal in the Software without restriction, including
    without limitation the rights to use, copy, modify, merge, publish,
    distribute, sublicense, and/or sell copies of the Software, and to
    permit persons to whom the Software is furnished to do so, subject to
    the following conditions:
    
    The above copyright notice and this permission notice shall be
    included in all copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
    LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
    OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
    WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.vladstirbu.cordova;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import androidx.core.content.FileProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.os.Environment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLConnection;
import java.net.URL;
import java.io.*;

@TargetApi(Build.VERSION_CODES.FROYO)
public class CDVInstagramPlugin extends CordovaPlugin {

    private static final FilenameFilter OLD_IMAGE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("instagram");
        }
    };

    CallbackContext cbContext;
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d("com.cs.distribution", "Inside execute");
        this.cbContext = callbackContext;
        
        if (action.equals("share")) {
            String imageString = args.getString(0);
            String captionString = args.getString(1);
            String type = args.getString(3);
             String extension = args.getString(4);
            Log.d("com.cs.distribution",type);
          
            PluginResult result = new PluginResult(Status.NO_RESULT);
            result.setKeepCallback(true);
            Log.d("com.cs.distribution","outside check:");
            // if(type.equals("image")){
                Log.d("com.cs.distribution",type);
                this.share(imageString, captionString, type, extension);
            // }
            // else if(type.equals("video")){
            //     this.shareVideo(imageString, captionString);
            // }
           
            return true;
        }else if(action.equals("shareMultiple")){
            JSONArray mediaArray = args.getJSONArray(0);
            // String captionString = 'some';
            String type = "image";
            //  String extension = args.getString(4);
            Log.d("com.cs.distribution","image");
          
            PluginResult result = new PluginResult(Status.NO_RESULT);
            result.setKeepCallback(true);
            Log.d("com.cs.distribution","outside check:");
            if(type.equals("image")){
                Log.d("com.cs.distribution","image");
                this.shareMultiple(mediaArray);
            }
            // else if(type.equals("video")){
            //     this.shareVideo(imageString, captionString);
            // }
           
            return true;
    
        } else if (action.equals("isInstalled")) {
            this.isInstalled();
        } else {
            callbackContext.error("Invalid Action");
        }
        return false;
    }
    
    private void isInstalled() {
        try {
            this.webView.getContext().getPackageManager().getApplicationInfo("com.instagram.android", 0);
            this.cbContext.success(this.webView.getContext().getPackageManager().getPackageInfo("com.instagram.android", 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            this.cbContext.error("Application not installed");
        }
    }

      private void share(String imageString, String captionString, String type, String extension) {
        Log.d("com.cs.distribution","Image");
        String fileExtention = extension;
        String contentType = "image/*";
        if(type.equals("video")){
            fileExtention = ".mp4";
            contentType = "video/*";
        }
        if (imageString != null && imageString.length() > 0) { 
          byte[] imageData = Base64.decode(imageString, 0);
          
          File file = null;  
          FileOutputStream os = null;
          
          File parentDir = this.webView.getContext().getExternalFilesDir(null);
          File[] oldImages = parentDir.listFiles(OLD_IMAGE_FILTER);
          for (File oldImage : oldImages) {
              oldImage.delete();
          }

          try {
              file = File.createTempFile("instagram", fileExtention, parentDir);
              os = new FileOutputStream(file, true);
          } catch (Exception e) {
              e.printStackTrace();
          }

          try {
              os.write(imageData);
              os.flush();
              os.close();
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }

          Intent shareIntent = new Intent(Intent.ACTION_SEND);
          shareIntent.setType(contentType);

          if (Build.VERSION.SDK_INT < 26) {
              // Handle the file uri with pre Oreo method    
              shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
          } else {
              // Handle the file URI using Android Oreo file provider
              FileProvider FileProvider = new FileProvider();

              Uri photoURI = FileProvider.getUriForFile(
                      this.cordova.getActivity().getApplicationContext(),
                      this.cordova.getActivity().getPackageName() + ".provider",
                      file);

              shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
              shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          }

          shareIntent.putExtra(Intent.EXTRA_TEXT, captionString);
          shareIntent.setPackage("com.instagram.android");

          this.cordova.startActivityForResult((CordovaPlugin) this, Intent.createChooser(shareIntent, "Share to"), 12345);   
        } 
        else {
          this.cbContext.error("Expected one non-empty string argument.");
        }
    }

    private void shareMultiple(JSONArray mediaArray) {
      Log.d("com.cs.distribution","Image");
      // String fileExtention = ".png";
      String contentType = "image/*";
      try{
        if (mediaArray.length() > 0) {
          Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
          shareIntent.setType(contentType);
          ArrayList<Uri> fileUris = new ArrayList<Uri>();
          File cacheFile = null;
          Uri photoURI = null;
          final String dir = getDownloadDir();
          Log.d("tag", "dir: "+dir);
          for (int i = 0; i < mediaArray.length(); i++) {
            Uri fileUri = getFileUriAndSetType(shareIntent, dir, mediaArray.getString(i), null, i);
            Log.d("tag", "cacheFile: "+cacheFile);
            photoURI = FileProvider.getUriForFile(this.webView.getContext(), this.cordova.getActivity().getPackageName()+".sharing.provider", new File(fileUri.getPath()));
            Log.d("tag", "photoURI: "+photoURI);
            fileUris.add(photoURI);
          }

          Log.d("tag", "fileUris: "+fileUris);
          shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);

          // shareIntent.putExtra(Intent.EXTRA_STREAM, photoURI);
          shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          shareIntent.putExtra(Intent.EXTRA_TEXT, "hello");
          shareIntent.setPackage("com.instagram.android");

          this.cordova.startActivityForResult((CordovaPlugin) this, Intent.createChooser(shareIntent, "Share to"), 12345);
        }
      } 
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    private void saveFile(byte[] bytes, String dirName, String fileName) throws IOException {
      final File dir = new File(dirName);
      final FileOutputStream fos = new FileOutputStream(new File(dir, fileName));
      fos.write(bytes);
      fos.flush();
      fos.close();
    }

    
    private static String getFileName(String url) {
      if (url.endsWith("/")) {
        url = url.substring(0, url.length()-1);
      }
      final String pattern = ".*/([^?#]+)?";
      Pattern r = Pattern.compile(pattern);
      Matcher m = r.matcher(url);
      if (m.find()) {
        return m.group(1);
      } else {
        return "file";
      }
    }

  private static final Map<String, String> MIME_Map = new HashMap<String, String>();
  static {
    MIME_Map.put("3gp",   "video/3gpp");
    MIME_Map.put("apk",   "application/vnd.android.package-archive");
    MIME_Map.put("asf",   "video/x-ms-asf");
    MIME_Map.put("avi",   "video/x-msvideo");
    MIME_Map.put("bin",   "application/octet-stream");
    MIME_Map.put("bmp",   "image/bmp");
    MIME_Map.put("c",     "text/plain");
    MIME_Map.put("class", "application/octet-stream");
    MIME_Map.put("conf",  "text/plain");
    MIME_Map.put("cpp",   "text/plain");
    MIME_Map.put("doc",   "application/msword");
    MIME_Map.put("docx",  "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    MIME_Map.put("xls",   "application/vnd.ms-excel");
    MIME_Map.put("xlsx",  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    MIME_Map.put("exe",   "application/octet-stream");
    MIME_Map.put("gif",   "image/gif");
    MIME_Map.put("gtar",  "application/x-gtar");
    MIME_Map.put("gz",    "application/x-gzip");
    MIME_Map.put("h",     "text/plain");
    MIME_Map.put("htm",   "text/html");
    MIME_Map.put("html",  "text/html");
    MIME_Map.put("jar",   "application/java-archive");
    MIME_Map.put("java",  "text/plain");
    MIME_Map.put("jpeg",  "image/jpeg");
    MIME_Map.put("jpg",   "image/*");
    MIME_Map.put("js",    "application/x-javascript");
    MIME_Map.put("log",   "text/plain");
    MIME_Map.put("m3u",   "audio/x-mpegurl");
    MIME_Map.put("m4a",   "audio/mp4a-latm");
    MIME_Map.put("m4b",   "audio/mp4a-latm");
    MIME_Map.put("m4p",   "audio/mp4a-latm");
    MIME_Map.put("m4u",   "video/vnd.mpegurl");
    MIME_Map.put("m4v",   "video/x-m4v");
    MIME_Map.put("mov",   "video/quicktime");
    MIME_Map.put("mp2",   "audio/x-mpeg");
    MIME_Map.put("mp3",   "audio/x-mpeg");
    MIME_Map.put("mp4",   "video/mp4");
    MIME_Map.put("mpc",   "application/vnd.mpohun.certificate");
    MIME_Map.put("mpe",   "video/mpeg");
    MIME_Map.put("mpeg",  "video/mpeg");
    MIME_Map.put("mpg",   "video/mpeg");
    MIME_Map.put("mpg4",  "video/mp4");
    MIME_Map.put("mpga",  "audio/mpeg");
    MIME_Map.put("msg",   "application/vnd.ms-outlook");
    MIME_Map.put("ogg",   "audio/ogg");
    MIME_Map.put("pdf",   "application/pdf");
    MIME_Map.put("png",   "image/png");
    MIME_Map.put("pps",   "application/vnd.ms-powerpoint");
    MIME_Map.put("ppt",   "application/vnd.ms-powerpoint");
    MIME_Map.put("pptx",  "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    MIME_Map.put("prop",  "text/plain");
    MIME_Map.put("rc",    "text/plain");
    MIME_Map.put("rmvb",  "audio/x-pn-realaudio");
    MIME_Map.put("rtf",   "application/rtf");
    MIME_Map.put("sh",    "text/plain");
    MIME_Map.put("tar",   "application/x-tar");
    MIME_Map.put("tgz",   "application/x-compressed");
    MIME_Map.put("txt",   "text/plain");
    MIME_Map.put("wav",   "audio/x-wav");
    MIME_Map.put("wma",   "audio/x-ms-wma");
    MIME_Map.put("wmv",   "audio/x-ms-wmv");
    MIME_Map.put("wps",   "application/vnd.ms-works");
    MIME_Map.put("xml",   "text/plain");
    MIME_Map.put("z",     "application/x-compress");
    MIME_Map.put("zip",   "application/x-zip-compressed");
    MIME_Map.put("",       "*/*");
  }


  private String getMIMEType(String fileName) {
    String type = "*/*";
    int dotIndex = fileName.lastIndexOf(".");
    if (dotIndex == -1) {
      return type;
    }
    final String end = fileName.substring(dotIndex+1, fileName.length()).toLowerCase();
    String fromMap = MIME_Map.get(end);
    return fromMap == null ? type : fromMap;
  }

  private byte[] getBytes(InputStream is) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];
    while ((nRead = is.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }

  private Uri getFileUriAndSetType(Intent sendIntent, String dir, String image, String subject, int nthFile) throws IOException {
    // we're assuming an image, but this can be any filetype you like
    String localImage = image;
    if (image.endsWith("mp4") || image.endsWith("mov") || image.endsWith("3gp")){
      sendIntent.setType("video/*");
    } else if (image.endsWith("mp3")) {
      sendIntent.setType("audio/x-mpeg");
    } else {
      sendIntent.setType("image/*");
    }

    if (image.startsWith("http") || image.startsWith("www/")) {
      String filename = getFileName(image);
      localImage = "file://" + dir + "/" + filename.replaceAll("[^a-zA-Z0-9._-]", "");
      if (image.startsWith("http")) {
        // filename optimisation taken from https://github.com/EddyVerbruggen/SocialSharing-PhoneGap-Plugin/pull/56
        URLConnection connection = new URL(image).openConnection();
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null) {
          final Pattern dispositionPattern = Pattern.compile("filename=([^;]+)");
          Matcher matcher = dispositionPattern.matcher(disposition);
          if (matcher.find()) {
            filename = matcher.group(1).replaceAll("[^a-zA-Z0-9._-]", "");
            if (filename.length() == 0) {
              // in this case we can't determine a filetype so some targets (gmail) may not render it correctly
              filename = "file";
            }
            localImage = "file://" + dir + "/" + filename;
          }
        }
        saveFile(getBytes(connection.getInputStream()), dir, filename);
        // update file type
        String fileType = getMIMEType(image);
        sendIntent.setType(fileType);
      }
    } 
    Log.d("tag", "localImage: "+localImage);
    return Uri.parse(localImage);
  }

  private String getDownloadDir() throws IOException {
    // better check, otherwise it may crash the app
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
      // we need to use external storage since we need to share to another app
      final String dir = this.webView.getContext().getExternalFilesDir(null) + "/socialsharing-downloads";
      createOrCleanDir(dir);
      return dir;
    } else {
      return null;
    }
  }

  private void createOrCleanDir(final String downloadDir) throws IOException {
    final File dir = new File(downloadDir);
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        throw new IOException("CREATE_DIRS_FAILED");
      }
    } else {
      cleanupOldFiles(dir);
    }
  }

  private void cleanupOldFiles(File dir) {
    for (File f : dir.listFiles()) {
      //noinspection ResultOfMethodCallIgnored
      f.delete();
    }
  }
    
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      Log.v("Instagram", "shared ok");
      if(this.cbContext != null) {
        this.cbContext.success();
      }
    } else if (resultCode == Activity.RESULT_CANCELED) {
      Log.v("Instagram", "share cancelled");
      if(this.cbContext != null) {
        this.cbContext.error("Share Cancelled");
      }
    }
  }
}
