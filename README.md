How to setup the Server, Cache, and client to test TCP and SNW protocols:

# Step 1:
compile your java files using javac version 17

# Step 2:
take the compiled .class files and put it into each respective folder as follows:

client folder:
- Client.class
- MyFile.class
- SNW_Transport.class
- TCPTransport.class

cache folder:
- Cache.class
- MyFile.class
- SNW_Transport.class
- TCPTransport.class

server folder:
- Server.class
- MyFile.class
- SNW_Transport.class
- TCPTransport.class

# Step 3 (If you want to get it working locally, skip to step 5):
compress the src with the folders you just compiled the .class files in each folder for from the previous step and send the file to the server using this command:
```scp src.zip [NetID]@[serverIP]:[Directory of your home]```


# Step 4:
Unzip your zip file in the server using the following command:
```python3 -m zipfile -e src.zip .```
This will extract the file in the directory it is in.

# Step 5:
Open 3 terminals and log into the server on each and go to the client, cache, and server directory on each terminal. Now enter each of these commands into each respective terminal (note: the server only accepts ports in the range of 20000-24000):

server terminal:
Usage: java Server <port> <protocol>
```
Using TCP example: 
java Server 21000 tcp
Using SNW example:
java Server 21000 snw
```

cache terminal:
Usage: java Cache <port> <serverIP> <serverPort> <protocol>
```
Using TCP example:
java Cache 22000 localhost 21000 tcp
Using SNW example:
java Cache 22000 localhost 21000 snw
```

client terminal:
Usage: java Client <serverIP> <serverPort> <cacheIP> <cachePort> <protocol>
```
Using TCP example:
java Client localhost 21000 localhost 22000 tcp
Using SNW example:
java Client localhost 21000 localhost 22000 snw
```

# Step 6:
Simply use the get or put commands from the client side and everything should work.

example commands:
put test.tst //sends file to server
get test.txt //gets file from server or cache