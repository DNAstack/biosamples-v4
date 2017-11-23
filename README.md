[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c2acf39bb65d4793ae3420c70ab51388)](https://www.codacy.com/app/afaulconbridge/biosamples-v4?utm_source=github.com&utm_medium=referral&utm_content=EBIBioSamples/biosamples-v4&utm_campaign=badger)

Quickstart
==========

Install Maven and JDK 8
Install docker-compose https://docs.docker.com/compose/

`mvn package`

`docker-compose build`

`docker-compose up biosamples-webapps-core`

Note: this will download around 1GB of docker containers

public interface at http://localhost:8081/biosamples/beta

internal RabbitMQ interface at http://localhost:15672/
internal Neo4J interface at http://localhost:7474/
internal Solr interface at http://localhost:8983/

An example of the JSON format that can be sent by POST to http://localhost:8081/biosamples/beta/samples is https://github.com/EBIBioSamples/biosamples-v4/blob/master/models/core/src/test/resources/TEST1.json

Development getting started
===========================

To checkout and compile the code, you will need Git, Maven, and a JDK 8. On ubuntu-based Linux distributions (16.04 or higher) you can do this with:

`sudo apt-get install maven git default-jdk`

Then you can check out and compile the code with:

`git clone https://github.com/EBIBioSamples/biosamples-v4 biosamples`
`cd biosamples`
`mvn -T 2C package`

Note: This will require a large download of Spring dependencies.
Note: This uses up to two threads per core of your machine.

At that point, you will have a local compiled version of all the biosamples tools.

To start a copy running on the local machine (e.g. to test any changes you have made) you can 
use Docker and Docker-compose. https://docs.docker.com/compose/

You can use `docker-compose up` to start all the services, or you can bring them up and down at 
will individually. See docker-compose.yml file for more information on service names and dependencies.


By default, the pipelines will not be run. They can be manually triggered as follows:

NCBI
----

Download the XML dump (~400Mb) to the current directory:

`wget http://ftp.ncbi.nih.gov/biosample/biosample_set.xml.gz`

Run the pipeline to send the data to the submission API via REST

`java -jar pipelines/ncbi/target/pipelines-ncbi-4.0.0-RC1.jar --ncbi`


Developing
==========

Docker can be run from within a virtual machine e.g VirtualBox. This is useful if it causes any 
problems for your machine or if you have an OS that is not supported.

You might want to mount the virtual machines directory with the host, so you can work in a standard 
IDE outside of the VM. VirtualBox supports this.

If you ware using a virtual machine, you might also want to configure docker-compose to start by 
default. 

As you make changes to the code, you can recompile it via Maven with:

`mvn -T 2C package`

And to get the new packages into the docker containers you will need to rebuild containers with:

`docker-compose build`

If needed, you can rebuild just a single container by specifying its name e.g.

`docker-compose build biosamples-pipelines`

To start a service, using docker compose will also start and dependent services it requires e.g.

`docker-compose up biosamples-webapp-api`

will also start solr, neo4j, mongo, and rabbitmq

To run an executable file in a docker container, and start its dependencies first use something like:

`docker-compose run --service-ports biosamples-pipelines`

If you want to add command line arguments note that these will entirely replace the executable in the 
docker-compose.yml file. So you need to do something like:

`docker-compose run --service-ports biosamples-pipelines java -jar pipelines-4.0.0-RC1.jar --debug`

If you want to connect debugging tools to the java applications running inside docker containers, 
see instructions at http://www.jamasoftware.com/blog/monitoring-java-applications/

Note that you can bring maven and docker together into a single commandline like:

`mvn -T 2C package && docker-compose build && docker-compose run --service-ports biosamples-pipelines`

Beware, Docker tar's and copies all the files on the filesystem from the location of docker-compose 
down. If you have data files there (e.g. downloads from ncbi, docker volumes, logs) then that process can
take so long that it makes using Docker impractical.
 
As docker-compose creates new volumes each time, you may fill the disk docker is working on. 
To delete all docker volumes use:

`docker volume ls -q | xargs -r docker volume rm` 

To delete all docker images use:

`docker images -q | xargs -r docker rmi`

NOTE: this will remove everything not just things for this project
 
 
MongoDB notes
=============

Cross-platform easy to use mongodb management tool http://www.mongoclient.com
 
 
Problems with spring-data-rest
==============================

This was originally using spring-data-rest to expose rest API for the repositories. But there are a number of 
problems with this (see below) and that was scrapped in favor of implementing custom HATEOAS compliant
endpoints.

Content type negotiation is not possible as it can't overlap with the URLs for the Thymeleaf controllers and
it can't serve XML even with the appropriate converters supplied.

When repeatedly sending JSON because it is a list of things with optional components, the optional 
parts can become mixed if the list ordering changes. Maybe this can be remedied by using map of 
attribute types instead?

Known issues
============

Solr has a limit on the field size (technically the term vector). Therefore the attribute values over 255 characters are not indexed in solr. 
