## Gerrit Jenson Plugin
This plugin allows to transform static-check-report issues to comments in [Gerrit](http://code.google.com/p/gerrit/) review


## Maintainers
Long Chen<br/>
dragon.chern@foxmail.com


## Build
mvn clean eclipse:eclipse<br/>
mvn intsall<br/>
mvn package


## Description
This Gerrit-Jenson plugin is used to post the review of Issues of static code check to the Gerrit server, but how to generate the issues report is not the task of current plugin. If you want to use this plugin, you should generate the issue report by the third tool. The format of issue report must contain 5 fields, they are file, line, id, severity and msg. The file is the source file. The line is the number of issue, The id is the error key. The severity is the error level. The msg is the error message. The owner of project can customize the error level, You can set in the SonarQube Settings.Default condition uses the error level of the third tool.
