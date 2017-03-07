A test for SSL/TLS certificates

This program tests SSL certificates and the like.

This program is intended to test out local certificate authorities. Follow these steps to create a local CA, a truststore and a server keystore

1) Create the local CA self-signed certificate and private key

openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem.txt -out ca-certificate.pem.txt -days 365 -nodes

2) Create the truststore

keytool -import -keystore truststore -file ca-certificate.pem.txt -alias ca  -storepass whatever

3) Create the server keystore

keytool –keystore serverkeystore –genkey –alias server -keyalg rsa -storepass whatever

4) Create a certificate signing request for the server

keytool –keystore serverkeystore -storepass whatever –certreq –alias server  –file server.csr

5) Sign the server CSR with the local CA

openssl x509 -req -CA ca-certificate.pem.txt -CAkey ca-key.pem.txt -in server.csr -out server.cer -days 365 –CAcreateserial

6) Import the local CA to the server keystore

keytool -import -keystore serverkeystore -storepass whatever -file ca-certificate.pem.txt -alias ca

7) Import the singed certificate to the sever kestore

keytool -import -keystore serverkeystore -storepass whatever -file server.cer -alias server

Compile the program with the following command:

mvn package

Run the server with the following

java -cp target\ssltest3-1.0-SNAPSHOT-jar-with-dependencies.jar SSLTest server

In another window, run the client with the following command

java -cp target\ssltest3-1.0-SNAPSHOT-jar-with-dependencies.jar SSLTest client

When the client gets a connection it should prompt you with a string like "localhost:6789> ".  Type in something and it should be echoed back.  Quit by entering "quit".