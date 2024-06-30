# README.md

## Introduction

This is the code repository for the article `EchoAid: Enhancing Livestream Shopping Accessibility for the DHH Community`. It includes the source code, experiment records, sample videos, and introduction videos.

## Supplementary Materials

### Experiment  Records

1. `data/accuracy.md`: Results that evaluate the system's accuracy and universality using 20 samples from different video platforms and topics.

### Sample Videos

Video samples are used to assess users cognitive load, user experience (Video 1), and memory retention (Videos 2 and 3).  

2. `video-exmaples/Watermelon.mp4`: The first video sample about selling watermelon.
3. `video-examples/Bedding.mp4`: The second video sample about selling bedding.
4. `video-examples/Toothpaste.mp4`: The third video sample about selling toothpaste.

### Introduction Videos

5. `video-introduction/Introduction_En.mp4`: The English version of the introduction video.
6. `video-introduction/Introduction_Cn.mp4`: The Chinese version of the introduction video.

## Preparation Before Running the Program

Please use the latest version of Android Studio to open the `src` directory. Additionally, prepare an Android device running Android 10 or higher. You will also need an iFlytek account capable of real-time speech transcription, and a Baidu Cloud account with ERNIE-3.5 and ERNIE-4.0-latest services enabled. Replace the `*` placeholders in the code with your API Key and other necessary information in the relevant files:

```
src/dhh-android/app/src/main/java/com/example/audiocapturer/utils/Web.kt
src/dhh-android/app/src/main/java/com/example/audiocapturer/utils/Ernie.kt
```

## Running the Program

Now you can run the program on your Android device. During execution, grant the necessary permissions, invoke the floating window, and start capturing in a video or other live streaming application to utilize the functionalities.

## Contact Us

Due to the requirement for blind peer review, we are not providing author information or contact details at this time.