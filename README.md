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


HOWTO: 
build it: 
- Add references to the libraries for json-simple, beanstemc, mysql-connector. 
- build a jarfile. 

run it: 
- start on commandline: 
java -jar BeanstalkWorkerResult.jar

It will connect to the database and beanstalkd using hardcoded settings.
It will then listen to the beanstalk tube: result


TODO: 
- make the result worker configurable over commandline and / or config file. 