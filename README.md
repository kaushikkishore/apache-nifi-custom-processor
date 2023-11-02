# Apache NiFi Custom processor. 

This will help you to learn about how to create apache nifi plugin/processor. 
You can also leverage the existing controller service in the processor to access the details. 
In this case I am trying to access AWS authentication service in my custom processor. 

Here are the use cases which i have covered in this repo. 

1. Using a controller in the custom processor. 
2. Sending out multiple responses from the custom processor. 


## NiFi custom processor

install JAVA and maven

To check the if these are installed run these commands

`java -version`
The version must be 11 or more.

To check the maven installation
`mvn -version`
Maven can be latest also.

Now you need to generate the code boilerplate for the custom processors.

1. `mvn archetype:generate`

2. This will search all available sources. Now it will ask for search.
   Enter `nifi` in search to search nifi.

3. Now Once you will select the nifi it will ask for what type of application

4. You need to select between `processor` or `service`

5. next step is tell the project name(Artifact base name) - it could be anything like `sample`

6. Next it will ask the groupId - name that as package name `com.org` so your project will be <group-id>.processors.<base-name>

7. Artifact Id - wil be part of your project artifact id. Which is there in `pom.xml`

8. Next specify the version for the processor.

9. Next specify name for the project.

10. In last say Yes

Now that will generate the boilerplate code for you.

### To build

`mvn clean install -Denforcer.skip=true`

### Deploy

Now a folder would have been generated like `nifi-<app-name>-nar`

Move inside the folder and then go to `target`

from this folder you can copy the `nar` file and then you need to deploy
this into the nifi installation directory.

You have 2 options

1. `extensions` folder where the plugin will be available without the restart.
2. in `lib` folder you need to restart.

> Note - In both cases I had to restart the server. 

once the deployment is done your processor will be available in the processor section.

## To add log for your custom processors

in nifi folder navigate to `conf` folder and add this line in bottom. You can define you level of logging here also.

```
<logger name="com.astuto.processors.cloudwatchLogReader.MetricReader" level="DEBUG"/>
```

This line you will get in your `src/main/resources/META-INF.service/org.apache.nifi.processor.Processor` file. At the bottom of this file the path will be there.

```
com.org.processors.artifact.main
```
