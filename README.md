# ScopedMediaPicker

ScopeMideaPicker is media picker for Image and Video which supports Androd 10 & 11 file system (without requestLegacyExternalStorage flag), it includes both with camera/gallery and guess what, permissions are handled internally, yayy!

You just need to call start metho and return call back to library and thats it, your work is done amogoes :)

It also allow multiple image pick and also allow to crop image functionality in case of single pick (Typically used for profile picture pick)

follow simple instruction below 

Step 1. Add the JitPack repository to your build file 

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.bhoomit11:ScopedMediaPicker:Tag'
	}

Step 3. Initialize your media picker

    private val scopedMediaPicker by lazy {
        ScopedMediaPicker(
            activity = this@MediaPickerActivity,  
            fragment = this@MediaPickerFragment  // Optional, Only if used with fragment
            requiresCrop = true, // Optional
            allowMultipleImages = true, // Optional
            mediaType = ScopedMediaPicker.MEDIA_TYPE_IMAGE or ScopedMediaPicker.MEDIA_TYPE_VIDEO
        )
    }

Okay so let's discuss step 3,
To initlaize media picker you need to add several configuration as per you requirement like  
> **Activity** - If you're using your media player in activity, give your activity instance here, so it will give you callback in this same acivity for override method _onRequestPermissionsResult_ and _onActivityResult_, also this will used as context in library, so even with the fragment this one is required parameter
>
> Fragment - If you're using your media player in fargment, give your fragment instance here, so it will give you callback in this same fragment for override method _onRequestPermissionsResult_ and _onActivityResult_,
>
> requiresCrop - If require crop fuctionality on your picked image you can pass this as true, **please note** this won't work with allowMultipleImages as true, it works only for single pick image like select profile picture, so if this param is true then allowMultipleImages should be false.
>
> allowMultipleImages - If require multiple image pickup
>
> mediaType - this is required parameter,  
>>_ScopedMediaPicker.MEDIA_TYPE_IMAGE_ for image picker only  
>>_ScopedMediaPicker.MEDIA_TYPE_VIDEO_ for video picker only  
>>_ScopedMediaPicker.MEDIA_TYPE_IMAGE or ScopedMediaPicker.MEDIA_TYPE_VIDEO_ for both

Step 4. Start picking up your media

    btn_capture.setOnClickListener {

        scopedMediaPicker.start { path, type ->
            if (type == ScopedMediaPicker.MEDIA_TYPE_IMAGE) {
                // You get your image path here
            } else {
                // You get your video path here
            }
        }

    }

Now here you get path as string for your picked image/video
but what if _allowMultipleImages_ is true, no worries you just have to call another call back for that in ScopedMediaPicker

    scopedMediaPicker.startForMultiple { pathList, type ->
        Log.e("List",pathList.toString())
    }

You'll get you list of image paths in pathList ArrayList.

So yeah, that's it, check exampe in GIT repo for more  
and keep posting you reviews   
Happy Coding :)