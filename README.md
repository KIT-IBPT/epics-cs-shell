# EPICS CS-Shell Java Server

The CS-Shell is EPICS business logic server, which provides easy way to implement server side logic in Java and offer it to various application through EPICS and CA interface and communication protocol. 

It has similar role as EPICS Database has for IOC, yet does not replaces it. 

The CS-Shell application server is not intended for writing device drivers, this is efficiently and sufficiently covered by EPICS IOC. 

The CS-Shell application server is designed for implementing of algorithms and complex procedures as server process using capacity of JAVA programming language and supporting Java libraries.

The CS-Shell application has been developed for and it is used at [KARA](https://www.ibpt.kit.edu/kara.php) and [FLUTE](https://www.ibpt.kit.edu/flute.php) at [Institute of Beam Physics and Technology / KIT](https://www.ibpt.kit.edu/).

## The Application Server and CSS

The CS-Shell application server works well together and supplements functionality of CSS. Perfect example is complete ANKA alarm server solution, which is build around CSS BEAST alarm.

The CS-Shell alarm server is implemented on top of CS-Shell EPICS application server with focus on supporting functionality of CSS BEAST alarm engine.

## Documentation

Development documentation can be found here: [https://kit-ibpt.github.io/epics-cs-shell/](https://kit-ibpt.github.io/epics-cs-shell/).

## Copyright / License

This project is licensed under the terms of the [GNU Lesser General Public License v3.0 license](LICENSE) by 
[Karlsruhe Institute of Technology's Institute of Beam Physics and Technology](https://www.ibpt.kit.edu/) 
and was developed by [igor@scictrl.com](http://scictrl.org).
