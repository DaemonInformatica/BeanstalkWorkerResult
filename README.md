BeanstalkWorkerResult
=====================

Part of a network of programs to bruteforce MD5 hashes using beanstalkd. This is the decoder


Why did I write this: 
One of my favorite branches of computer science is parallel computing. The art of dividing CPU-resource intensive projects
over multiple / many computers and gathering the results as pieces of the puzzle to put back together. Somebody introduced me
to the simple beanstalk-queue and I figured 'Lets think of an excuse to implement something around this. 

Thus the MD5 bruteforce hash-cracker was born. 

To use this, you need to download 4 projects in total: 
- BeanstalkManagerMD5 
- BeanstalkWorkerDecodeMD5 
- BeanstalkWorkerResult (This project) 
- webservice database installation and webconsole. 

What does the BeanstalkWorkerResult do? 

This project is the simplest: Listen to the result queue and for each job that is received: 
- Get the result details. 
- push to database. 
