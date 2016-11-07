# Client-Server Application with Diffie-Hellman encrypting

Program allows user to create server-clients safe connection, using Diffie-Hellman algorithm. Exchanging data are encoding with base64 and optionally with Caesar cipher. 
You can read more about DH algorithm here:

`````
https://en.wikipedia.org/wiki/Diffie%E2%80%93Hellman_key_exchange.
`````
Application was built with Gradle 3.1 tool.

## System requirements

- Java 1.8 -  code contains base64 methods and lambda expressions, so you won't be able to compile it on earlier version of Java.

## Installing

You can download the newest Java Development Kit from here

```
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
```

Or in LINUX systems you can put this line into terminal
```
sudo apt-get install openjdk-8-jdk
```

## Compile
To compile the appliactions you don't need to have Gradle in your system.Just go to Server or Client directory and you will find there
```
gradlew.bat (for Windows)  or gradlew (for Linux)
```
Run this file, using terminal. After that go ./build/libs, where you will find generated .jar file.

To run Server.jar on  e.g. 8080 port,  write in your terminal
```
java -jar Server.jar 8080
```

Now the server is ready to start connection with Client. Go to Client/build/libs directory and write
```
java -jar Client.jar yourName 8080 localhost
```
If you want to encrypting you conversation with CesearCipher, append encrypt at the end of line.
```
java -jar Client.jar yourName 8080 localhost encrypt
```

To end conversation write
```
exit
```
## Running the tests

To run test with gradle you need to download Gradle. You can do it from site:
```
https://gradle.org/
```

To run test move to Client or Server directory and in terminal run command 
```
gradle test
```

Now you can find results of your test in ./build/reports/tests in index,html file.
Application cotains just few simple tests for Caesar Cipher methods.

## Built With

* [Gradle](https://gradle.org/) 


## Purpose for project

Project was realised during academic course "Security of network services". 

## Authors

* **Jedrzej Mirowski** - (https://github.com/host3234)

## License

This project is licensed under the MIT License.