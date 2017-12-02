# NAVIFT99-server
### Java server for viewing and editing USN NAVFITs. 

##### Part of NAVFITOnline project: [client-cert-auth](https://github.com/ansonl/client-cert-auth), [navfit99-js](https://github.com/ansonl/navfit99-js), and [navfit99-server](https://github.com/ansonl/navfit99-server)

![NAVFIT hi res](https://github.com/navfit99/navfit99.github.io/blob/master/assets/img/navfit99-256.png?raw=true)

### Use NAVFITOnline at **[https://navfit99.github.io](https://navfit99.github.io)**. \

-----

#### Steps to host your own NAVFITOnline server:

1. Install [Maven](https://maven.apache.org/).
  - You can extract the Maven folder to your `\Program Files` or `\bin` directory. 
  - Make sure the destination folder is included in your `$PATH` environment variable. 
  
2. Get code with `git clone https://github.com/ansonl/navfit99-js.git` or use *Clone or download* button above.
3. Set `$PORT`, `$NAVFIT_AES_KEY`, and `$REDIS_URL` environment variables. 
  - `$PORT` is the integer port number to bind on.
  - `$NAVFIT_AES_KEY` is a 128 bit (16 character) key that is used to by the server program to encrypt NAVFIT data at rest on Redis storage. 
  - `$REDIS_URL` is connection URL for your Redis storage instance. 
4. Compile the project by running `mvn clean package` in the root project directory (the project folder). 
5. Run the compiled JAR with `java -jar target/navfit99-server-1.0-jar-with-dependencies.jar`. 

##### Notes

- If you use Heroku for hosting, `$REDIS_URL` is provided for your instance if you use Heroku's *Heroku Redis* addon.\
  - **Free Heroku instances do not support SSL connections and stunnel between the free instance and Heroku Redis instance. 
  - ** See [here](https://devcenter.heroku.com/articles/securing-heroku-redis) for more information. 
- `$NAVFIT_AES_KEY` will be truncated to 128 bits if it is longer than 128 bits.
- Getting Maven setup didn't lead me to any good resources or examples. If you aren't interested in this project but want a drop in Maven configuration `pom.xml` file for starting your own project, you may find this project's `pom.xml` useful. Just update the package identifier, update dependency links, and you will have a working JAR that includes dependencies. 
 
#### License

All work in this project is copyrighted by Anson Liu unless specified to be created by another author or otherwise. Work on this project by Anson Liu is made available under MIT License with attribution required. 
 
#### Resources
 
This project was made possible because it builds upon great Java packages created by others. Some notable packages are listed below and I have left comments in the code for many other acknowledgements.
- [UCanAccess - A Pure Java JDBC Driver for Microsoft Access](http://ucanaccess.sourceforge.net/)
- [Jedis - A blazingly small and sane redis java client](https://github.com/xetorthio/jedis)
- [Sun HttpServer - Dependency free HttpServer included in Java](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html)
- [json-simple - Simple JSON encoding and decoding that just worked](https://github.com/fangyidong/json-simple)
- [Apache Commons](https://commons.apache.org/)
