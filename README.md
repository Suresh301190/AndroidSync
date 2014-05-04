AndroidSync
===========

A Collaborative Video Down-loader

Abstract
===========

AndroidSync is an application which enables a collaborative method on smart phones for file downloading among multiple users, present at same location, wishing to download the same file without losing out on quality and time. Today, group of known people wanting to watch same video have to download the video individually on their mobile phones, which is at times accompanied by degraded video quality and long waiting time. With this application we propose a solution which provides a quick and efficient approach which makes use of the resources on all smart-phones of the group in a cooperative way so as to reduce the time taken for downloading improve the video viewing experience.

Introduction
=========== 

This application allows a group of smart phones wanting to download the same video,present within close proximity of each other, to perform video downloading in a collaborative manner.It can be used in scenarios like in colleges where a lecture can be downloaded and viewed simultaneously by students without any time delay.It makes use of available Wi-Fi and Bluetooth to perform downloading and device communication respectively.It makes use of master slave architecture where one phone which initiate the download is referred to as Master and others which are added by it are called as Slaves.The downloading takes place in parts which are defined by the master and allocated to slaves based on their availability. The slaves then send the downloaded portions of the video back to master using bluetooth connectivity which is followed by the stitching of these portions at master side.The complete video will then be broadcast to all the slaves.It provides the flexibility of role allocation to  smartphones where the user can decide which role he wants to play and select accordingly.

Project Specifications
===========

1. The application works on Android platform.
2. Device should support bluetooth and Wi-Fi functionalities.
3. Master can communicate only with bluetooth paired devices.
4. Presence of network connectivity is a must for downloading purpose.
5. Only one device can act as master to initiate this process.
6. It can support maximum 6 + 1 devices at a time i.e 6 slaves and 1 master.

Application WorkFlow
===========

1. On the launch of application, user is provided with the Master-Slave options.
2. Master then provides the URL of video, it wants to initiate the download of.
3. This video is then divided into Maximum eight chunks which are to be downloaded collaboratively amongst the master-slaves.
4. Master starts downloading the first part and then performs the chunk allocation to slaves according to their availability.
5. The downloading at master side continues as the scheduling is done on a separate thread which is Managed by DownloadManager.
6. Master Keeps pinging Slaves every few seconds depending upon the portion size to check if its alive and responding, if no response is received by the slave it is assumed to be dead or out of coverage, the the portion is added to the global pool.
7. Slaves, once allocated a chunk begin the downloading and notifies master regarding the success or failure. If no network is available slave sends a NAK to signal Master that is can’t download
8. On receiving the notification, master then allocates the remaining chunk(if any) to slave which  notified its success.
9. In case of failure the DownloadManager makes sure the file is downloaded by doing recovery mechanism by removing slave, adding the portion to global pool.
10. Once all the parts are downloaded successfully, master performs merging operation on its side.
11. The complete video is then broadcast to all the slaves.

@author
Suresh Rangaswamy






 


