**ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED**

```
  ##    ####    ###   #    #  ###  #   #  #####  #####   
 #  #   #   #  #   #  #    #   #   #   #  #       #   #  
#    #  #   #  #      #    #   #   #   #  #       #   #  
#    #  #   #  #      #    #   #    # #   #       #   #  
#    #  ####   #      ######   #    # #   ####    #   #  
######  ##     #      #    #   #    # #   #       #   #  
#    #  # #    #      #    #   #     #    #       #   #  
#    #  #  #   #   #  #    #   #     #    #       #   #  
#    #  #   #   ###   #    #  ###    #    #####  #####  
```

**ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED ARCHIVED**

**This Git repository is obsolete and has been archived.**

----

## LAAWS for of the LOCKSS Daemon

This is the source tree for the LOCKSS daemon.
See [http://www.lockss.org/](#) for information about the LOCKSS project.

### Obtaining Source

`git clone https://gitlab.lockss.org/laaws/laaws-daemon.git`

To update the local copy run within you local lockss-daemon dir:

`git pull`

### Building and Installing

### Dependencies:
- This daemon should be built with Java 8.
- Ant 1.7.1 or greater.  (http://ant.apache.org/)
- Python 2.5 or greater (but not 3.x).


### Other Dependencies:

#### Junit
Junit is included in the LOCKSS source distribution, but the Ant targets that invoke JUnit (test-xxx) require the JUnit jar to be on Ant's classpath.  The easiest way to do that is to copy lib/junit.jar into Ant's lib directory (\<ant-install-dir\>/ant/lib) or your local .ant/lib directory.

##### JAVAHOME Environment variable
For some of the tools the JAVAHOME env var must be set to the directory in which the JDK is installed.  (I.e., it's expected that tools.jar can be found in $JAVAHOME/lib)

### To Build
-`ant test-all`

Builds the system and runs all unit tests

-`ant test-one -Dclass=org.lockss.foo.TestBar`

Builds the system and runs one JUnit test class.

-`ant -projecthelp`

Lists other build options

-`ant btf`

Build out the test frameworks to allow running a daemon and testing on local machine.
