/*
    The MIT License (MIT)
    Copyright (c) 2013 - 2014 Vlad Stirbu

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

var exec = require('cordova/exec');

var hasCheckedInstall,
    isAppInstalled;

function shareDataUrl(dataUrl, caption, callback, mode, type, platform) {

  var imageData;
  var extension = '.'+dataUrl.match(/[^:/]\w+(?=;|,)/)[0];
  if(type.toLowerCase() == "image"){
    imageData = dataUrl.replace(/data:image\/(png|jpeg);base64,/, "");
  }
  else if(type.toLowerCase() == "video"){
    imageData = dataUrl.replace(/data:video\/(mp4);base64,/, "");
  }

    if (cordova && cordova.plugins && cordova.plugins.clipboard && caption !== '') {
      console.log("copying caption: ", caption);
      cordova.plugins.clipboard.copy(caption);
    }
    console.log(type);
    console.log("executing native share");
    console.log("extensionextension", extension);
    let data = imageData;
      if(platform !== "ios")
        data = _asArray(imageData);
    exec(
        function () {
            callback && callback(null, true);
        },
        function (err) {
            callback && callback(err);
        }, "Instagram", "share", [data, caption, mode, type, extension]
    );
}

_asArray = function (param) {
  if (param == null) {
    param = [];
  } else if (typeof param === 'string') {
    param = new Array(param);
  }
  return param;
};

var Plugin = {
  SHARE_MODES: {
    DEFAULT: 0,
    IGO: 1,
    IG: 2,
    LIBRARY: 3
  },
  // calls to see if the device has the Instagram app
  isInstalled: function (callback) {
    exec(function (version) {
      hasCheckedInstall = true;
      isAppInstalled = true;
      callback && callback(null, version ? version : true);
    },

    function () {
      hasCheckedInstall = true;
      isAppInstalled = false;
      callback && callback(null, false);
    }, "Instagram", "isInstalled", []);
  },
  share: function () {
    console.log(arguments);
    var data,
        caption,
        callback,
        mode = this.SHARE_MODES.DEFAULT,
        type="image",
        platform="ios"

    switch(arguments.length) {
    case 2:
      data = arguments[0];
      caption = '';
      callback = arguments[1];
      break;
    case 3:
      data = arguments[0];
      caption = arguments[1];
      callback = arguments[2];
      break;
    case 4:
        data = arguments[0];
        caption = arguments[1];
        callback = arguments[2];
        mode = arguments[3];
        break;
    case 5:
      data = arguments[0];
      caption = arguments[1];
      callback = arguments[2];
      mode = arguments[3];
      type = arguments[4].toLowerCase();
      break;
      case 6:
        data = arguments[0];
        caption = arguments[1];
        callback = arguments[2];
        mode = arguments[3];
        type = arguments[4].toLowerCase();
        platform = arguments[5].toLowerCase();
        break;
    default:
    }

    // sanity check
    if (hasCheckedInstall && !isAppInstalled) {
      console.log("oops, Instagram is not installed ... ");
      return callback && callback("oops, Instagram is not installed ... ");
    }

    var canvas = document.getElementById(data),
        magic = "data:image";

    if (canvas) {
      shareDataUrl(canvas.toDataURL(), caption, callback, mode, type, platform);
    }
    else if (data.slice(0, magic.length) == magic) {
      shareDataUrl(data, caption, callback, mode, type.toLowerCase(), platform);
    }
    else if(data.slice(0, "data:video/mp4".length) ==  "data:video/mp4" )
    {
      shareDataUrl(data, caption, callback, mode, type.toLowerCase(), platform);
    }
    else
    {
      console.log("oops, Instagram image data string has to start with 'data:image' or 'data:video/mp4'.")
    }
  },
  downloadToLibrary: function () { //only ios
    console.log(arguments[0]);
    var data,
        callback,
        type="image" 
        
       
    data = arguments[0];
    callback = arguments[1];
    type = arguments[2].toLowerCase();

    console.log("***************");
    console.log(type, callback);

    var imageData;
    if(type.toLowerCase() == "image"){
      imageData = data.replace(/data:image\/(png|jpeg);base64,/, "");
    }
    else if(type.toLowerCase() == "video"){
      imageData = data.replace(/data:video\/(mp4);base64,/, "");
    }

    exec(
      function () {
          callback && callback(null, true);
      },
      function (err) {
        console.log(err);
          callback && callback(err);
      }, "Instagram", "downloadToLibrary", [imageData, type.toLowerCase()]
    );
    
  },
  shareAsset: function (successCallback, errorCallback, assetLocalIdentifier) {
      // sanity check
      if (hasCheckedInstall && !isAppInstalled) {
          console.log("oops, Instagram is not installed ... ");
          return errorCallback && errorCallback("oops, Instagram is not installed ... ");
      }
      exec(successCallback, errorCallback, "Instagram", "shareAsset", [assetLocalIdentifier]);
  },
  shareMultiple: function (assets, successCallback, errorCallback) {
    // sanity check
    if (hasCheckedInstall && !isAppInstalled) {
        console.log("oops, Instagram is not installed ... ");
        return errorCallback && errorCallback("oops, Instagram is not installed ... ");
    }
    exec(successCallback, errorCallback, "Instagram", "shareMultiple", [assets]);
  }
};

module.exports = Plugin;