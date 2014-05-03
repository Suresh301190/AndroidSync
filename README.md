AndroidSync
===========

Collaborative Video Downloader 
Android-Sync

@author
Suresh Rangaswamy

Abstract

Collaborative video downloader is an application which enforces a collaborative method on smart phones for video streaming among multiple users, present at same location, wishing to download the same video without losing out on quality and time. Today, group of known people wanting to watch same video have to download the video individually on their mobile phones, which is at times accompanied by degraded video quality and long buffering time. With this application we propose a solution which provides a quick and efficient approach which makes use of the resources on all smartphones of the group in a cooperative way so as to improve the video viewing experience.

Introduction 

This application allows a group of smart phones wanting to download the same video,present within close proximity of each other, to perform video downloading in a collaborative manner.It can be used in scenarios like in colleges where a lecture can be downloaded and viewed simultaneously by students without any time delay.It makes use of available Wi-Fi and bluetooth to perform downloading and device communication respectively.It makes use of master slave architecture where one phone which initiate the download is referred to as Master and others which are added by it are called as Slaves.The downloading takes place in parts which are defined by the master and allocated to slaves based on their availability. The slaves then send the downloaded portions of the video back to master using bluetooth connectivity which is followed by the stitching of these portions at master side.The complete video will then be broadcasted to all the slaves.It provides the flexibilty of role allocation to  smartphones where the user can decide which role he wants to play and select accrodingly.

Project Specifications

The application works on Android platform.

Device should support bluetooth and Wi-Fi functionalities.

Master can communicate only with bluetooth paired devices.

Presence of network connectivity is a must for downloading purpose.

Only one device can act as master to initiate this process.

It can support maximum 6 + 1 devices at a time i.e 6 slaves and 1 master.
Application WorkFlow


On the launch of application, user is provided with the Master-Slave options.

Master then provides the URL of video, it wants to initiate the download of.

This video is then divided into Maximum eight chunks which are to be downloaded collaboratively amongst the master-slaves.

Master starts downloading the first part and then performs the chunk allocation to slaves according to their availability.

The downloading at master side continues as the scheduling is done on a separate thread which is Managed by DownloadManager.

Master Keeps pinging Slaves every few seconds depending upon the portion size to check if its alive and responding, if no response is received by the slave it is assumed to be dead or out of coverage, the the portion is added to the global pool.

Slaves, once allocated a chunk begin the downloading and notifies master regarding the success or failure. If no network is available slave sends a NAK to signal Master that is can’t download

On receiving the notification, master then allocates the remaining chunk(if any) to slave which  notified its success.

In case of failure the DownloadManager makes sure the file is downloaded by doing recovery mechanism by removing slave, adding the portion to global pool.

Once all the parts are downloaded successfully, master performs stitching operation on its side.

The complete video is then broadcasted to all the slaves.






 


