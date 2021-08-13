# ScopedMediaPicker

ScopeMideaPicker is media picker for Image, Video which supports Android 10 & 11 file system (without requestLegacyExternalStorage flag), it includes both with camera/gallery and guess what, permissions are handled internally, yayy!

Also added supports for File Picker with file types of your choice.

You just need to call start method and return call back to library and that's it, your work is done amigos :)

It also allow multiple image/video pick and also allow to crop image functionality in case of single pick (Typically used for profile picture pick)

follow simple instruction below 

### Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }

### Step 2. Add the dependency

    dependencies {
            implementation 'com.github.bhoomit11:ScopedMediaPicker:1.0.0'
    }

### Step 3. Initialize your media picker

    private val scopedMediaPicker by lazy {
        ScopedMediaPicker(
            activity = this@MediaPickerActivity,  
            fragment = this@MediaPickerFragment  // Optional, Only if used with fragment
            requiresCrop = true, // Optional
            allowMultipleImages = true, // Optional
        )
    }

Okay so let's discuss step 3,
To initialize media picker you need to add several configuration as per you requirement like  
> **activity** - If you're using your media player in activity, give your activity instance here, so it will give you callback in this same acivity for override method _onRequestPermissionsResult_ and _onActivityResult_, also this will used as context in library, so even with the fragment this one is required parameter
>
> **fragment** - If you're using your media player in fragment, give your fragment instance here, so it will give you callback in this same fragment for override method _onRequestPermissionsResult_ and _onActivityResult_,
>
> **requiresCrop** - If need crop functionality on your picked image you can pass this as true, **please note** this won't work with allowMultipleImages as true, it works only for single pick image like select profile picture, so if this param is true then allowMultipleImages should be false.
>
> **allowMultipleImages** - If need multiple image pickup

### Step 4. Start picking up your media

    btnCapture.setOnClickListener {

       scopedMediaPicker.startMediaPicker(
               mediaType = ScopedMediaPicker.MEDIA_TYPE_IMAGE or
                           ScopedMediaPicker.MEDIA_TYPE_VIDEO
           ) { pathList, type ->
           when (type) {
               ScopedMediaPicker.MEDIA_TYPE_IMAGE -> {
               // Handle your images here
               }
               ScopedMediaPicker.MEDIA_TYPE_VIDEO -> {
                // Handle your videos here
               }
           }

    }

You'll get you list of image paths in pathList ArrayList. (First element in case of single pick)
Also you need to pass mediaType parameter here

> **mediaType** - this is required parameter,
>>_ScopedMediaPicker.MEDIA_TYPE_IMAGE_ for image picker only  
>>_ScopedMediaPicker.MEDIA_TYPE_VIDEO_ for video picker only  
>>_ScopedMediaPicker.MEDIA_TYPE_IMAGE or ScopedMediaPicker.MEDIA_TYPE_VIDEO_ for both

### Step 4.1. Start picking up your files

    btnFile.setOnClickListener {
        scopedMediaPicker.startFilePicker(
            fileTypes = arrayListOf(
                ScopedMediaPicker.PDF,
                ScopedMediaPicker.DOC,
                ScopedMediaPicker.PPT,
                ScopedMediaPicker.XLS,
                ScopedMediaPicker.TXT,
                ScopedMediaPicker.ZIP
            )
        ) { list ->
            // Handle your files here
        }
    }

fileTypes defines array of support file types you want to pick with file picker
You'll get you list of FileData object contains 5 things:
> fileUri (To open document use this URI)  
> fileInputStream (To upload a file purpose use this Inputstream)  
> fileName  
> fileExtension  
> mimeType (While opening a URI with intent you can use this mimeType)

### Step 5. Return intent data to your ScopedMediaPicker,
if you don't follow this step callback will not return you scaled image/video in _start_ calback.

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        scopedMediaPicker.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        scopedMediaPicker.onActivityResult(requestCode, resultCode, data)
    }


So yeah, that's it, check example in GIT repo for more
and keep posting you reviews   
Happy Coding :)

### License
```
Copyright 2021 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
