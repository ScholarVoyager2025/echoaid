# README.md

## Introduction

This is the code repository for the article `XXXXX`. It includes the source code, experiment records, sample videos and introduction videos.

## Experiment Records

The records locate at `/data` directory. In `accuracy.md` file, there are results that evaluate system's accuracy and universality.

## Sample Videos

The sample Videos locate at `/video-exmples` directory. The examinees used these videos to test the system:

1. Watermelon
2. Bedding
3. Toothpaste

## Introduction Vidos

The introduction videos located at `/video-introduction` directory. There are Chinese and English Versions.

## Preparation Before Running the Program

Please use the latest version of Android Studio to open the `src` directory. Additionally, prepare an Android device running Android 10+. You will also need an iFlytek account capable of real-time speech transcription, and a Baidu Cloud account with ERNIE-3.5 and ERNIE-4.0-latest services enabled. Replace the `*` placeholders in the code with your API Key and other necessary information in the relevant files:

```
src\dhh-android\app\src\main\java\com\example\audiocapturer\utils\Web.kt
src\dhh-android\app\src\main\java\com\example\audiocapturer\utils\Ernie.kt
```

## Running the Program

Now you can run the program on your Android device. During execution, grant the necessary permissions, invoke the floating window, and start capturing in a video or other live streaming application to utilize the functionalities.

## Contact Us

Due to the requirement for blind peer review, we are not providing author information or contact details at this time.