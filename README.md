Distributed control framework to handle a Terracotta cluster and tests clients

## How to build

    mvn clean install -DskipTests

## Run specific tests

    mvn test -f integration-test/pom.xml -Dtest=<test-name>

Be careful not to cd directly into the module, you would not use the right kit version !

## Use specific location for Angela kits

    -Dangela.rootDir=/Users/adah/kits/

## Do not delete after run

    -Dangela.skipUninstall=true

## Specify JVM vendor

    -Djava.build.vendor=zulu

## Things to know

 * Angela is looking for JDK's in `$HOME/.m2/toolchains.xml`, the standard Maven toolchains file.
 See https://maven.apache.org/guides/mini/guide-using-toolchains.html to get its format and learn more about it.
 * Angela uses SSH to connect to remote hosts, so every non-localhost machine name is expected to be accessible via ssh,
 with everything already configured for passwordless authentication.
 * Angela spawns a small controlling app on every remote hosts that is very network-latency sensitive and uses lots of
 random ports. In a nutshell, this means that testing across WANs or firewalls just doesn't work. 
 * Angela expects a writeable `/data` folder (or at least a pre-created, writeable `/data/angela` folder) on every
 machine she runs on, i.e.: the one running the test as well as all the remote hosts.
