A test for SSL/TLS certificates

This program tests SSL certificates and the like.

You can either use the truststore and serverkeystore files that came with the project
or create new ones.  To create new files (in bief) use the following commands:

`openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes`

`keytool -importcert -keystore truststore -file ca-certificate.pem.txt -alias ca -storepass whatever`

`keytool –keystore serverkeystore –genkey –alias server -keyalg rsa -storepass whatever`

`keytool –keystore serverkeystore -storepass whatever –certreq –alias server –file server.csr`

`openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial`

`keytool -importcert -keystore keystore -storepass whatever -file ca-certificate.pem.txt -alias ca`

`keytool -importcert -keystore keystore -storepass whatever -file server.cer -alias server`

Compile the program with the following command (you must have maven installed):

mvn package

Run the server with the following

java -cp target\ssltest3-1.0-SNAPSHOT-jar-with-dependencies.jar SSLTest server

In another window, run the client with the following command

java -cp target\ssltest3-1.0-SNAPSHOT-jar-with-dependencies.jar SSLTest client

When the client gets a connection it should prompt you with a string like "localhost:6789> ".  Type in something and it should be echoed back.  Quit by entering "quit".

In a slightly more accessible format, here are the commands to create the truststore and serverkeystore.

1) Create the local CA self-signed certificate and private key

openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes

2) Create the truststore

keytool -importcert -keystore truststore -file ca-certificate.pem.txt -alias ca  -storepass whatever

3) Create the keystore

keytool –keystore keystore –genkey –alias server -keyalg rsa -storepass whatever

4) Create a certificate signing request

keytool –keystore serverkeystore -storepass whatever –certreq –alias server  –file server.csr

5) Sign the server CSR with the local CA

openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial

6) Import the local CA to the server

keytool -importcert -keystore keystore -storepass whatever -file ca-certificate.pem.txt -alias ca

7) Import the singed certificate to the sever

keytool -importcert -keystore keystore -storepass whatever -file server.cer -alias server

Compile the program with the following command (you must have maven installed):

mvn package

Run the server with the following

java -cp target\ssltest3-1.0-SNAPSHOT-jar-with-dependencies.jar SSLTest server

In another window, run the client with the following command

java -cp target\ssltest3-1.0-SNAPSHOT-jar-with-dependencies.jar SSLTest client

When the client gets a connection it should prompt you with a string like "localhost:6789> ".  Type in something and it should be echoed back.  Quit by entering "quit".