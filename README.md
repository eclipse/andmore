# Contributing

[Andmore](https://www.eclipse.org/andmore) is a fork of the former MOTODEV Studio and Android Development Tools plugins for eclipse.
The goal is to enhance the Google ADT plugins to provide additional features and functionality that ADT currently
does not provide.

This is an open source community lead project, so contributions are encouraged.  All code contributions will
be licensed under an Eclipse Public License.

To get started, <a href="https://www.eclipse.org/legal/clafaq.php">sign the Contributor License Agreement</a>.  You need to have a CLA on file with the Eclipse Foundation.

## Commit Message Format

The Eclipse Foundation requires that git commit messages follow particular format.  An example is as follows:

    [410937] Auto share multiple projects in single job
    
    When multiple projects are imported together, perform all the necessary
    auto shares in a single job rather than spawning a separate job for each
    project.
    
    Bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=410937
    Also-by: Some Otherperson <otherperson@someplace.net>
    Signed-off-by: Joe Somebody <somebody@someplace.net>

Andmore contains a template that should be enabled on your forks to provide the template.   This is located in the templates directory.

To configure git to use this template for all commit messages do the following:

    git config commit.template ./templates/commit.template

This will provide the following information for you to fill out:

    [bugnumber] Summary Description
    
    Detail Description here about the commit.
    
    Bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=bugnumber
    Signed-off-by: Joe Somebody <somebody@someplace.net>

All commits need to reference an existing Bugzilla number.  Any pull request that does 
not reference a bugzilla number won't be merged until a bugzilla is filed.  

Please make sure that your CLA is signed with the Eclipse Foundation, and that your commits are authored by the same email address as you referenced in your CLA.

You can change your default user.email address for the git repository:

     git config user.email youremail@someplace.com

## Bug Reports

You can file new bugs and feature requests at

https://bugs.eclipse.org/bugs/enter_bug.cgi?product=andmore

You can also use the Mylyn plugins in Eclipse with the Bugzilla connector to add bugs to
Andmore. The top-level areas for bugs in Andmore are

* Core - Android core functionality. This covers working with Android file types and the Android build process
* General - Interacting with the rest of Eclipse, preferences, about box, common UI elements, such as menus, toolbars, etc.
* Releng - Anything to do with the Andmore build system, p2, targets, etc.

## Installing

Nightly builds are available for adopters and others to use and install into a eclipse environment.  Currently Eclipse Luna or
higher are supported but other versions may work.

### Eclipse Marketplace

Andmore can be installed from the [Eclipse Marketplace](https://marketplace.eclipse.org/content/andmore).

### Nightly Update Site

The nightly p2 repository can be found at the following location:

http://download.eclipse.org/andmore/nightly

To install add the above url to your Available Software Sites.  There are several features that can be installed, the main ones you
need are the Andmore Core and if you want to do native development, then the Android Native Tools.

# Building Andmore

The following describes how to build Andmore and bring the source code into an 
existing eclipse environment to development and improve.

## Building from the Command Line

To compile.  You need Maven 3.x installed. Once Maven has been installed, you are ready 
to build.  Use the command:

    mvn clean install

Running the above will compile all features and plugins.  It will also generate a p2 update
site that can be used to install the plugins into your own Eclipse installation.

The site location is:

_andmore-core/site/target/repository_

You can add this as a local repository for Eclipse to install from.

If you want to build a complete product, then use the following command:

    mvn clean install -Pproduct
    
This will also build all supported platform versions of the full IDE as well.  These can be found in the directory 

_andmore-core/site/target/products_

Windows, MacOSX, and Linux builds are available.

If you want to skip tests during your local builds the following command will do the job.

    mvn clean install -PskipTests 

Before submitting a pull request back to eclipse/andmore, you must build with tests and 
all tests must pass.

### A note on MacOS

If using MacOS, confirm that Maven is using the correct JDK (Oracle, not Apple) using

    mvn -version

If the version shown in the response does not match what is shown when entering

    javac -version
    
You will need to force Maven to use the correct JDK by setting $JAVA_HOME in .mavenrc or
your .bash_profile.

    export JAVA_HOME=/Library/Java/JavaVirtualMachines/{jdk-version}/Contents/Home

This may occur if you've installed the Apple 1.6 JDK for other work and Maven is pointing 
to that version of the JDK.

## Building from Eclipse

To build from Eclipse, use the Eclipse SDK. You will need to add the m2e plugins after the
SDK is installed.  Java 1.7 or 1.8 are supported.

1. Choose _File > Import > Maven > Existing Maven Projects_
1. Choose the `pom.xml` in the root directory of Andmore project
1. When the projects are imported, there will be some errors. Ignore them for now.
1. Choose _Preferences > Plug-in Development > Target Platform_
1. Choose the target that ends with `andmore.target`. If there are other Andmore targets with different names, ignore them.
1. There will be some sub-projects that do not need to be in the workspace. They will have compile errors because they are for a different OS than you are using. It is alright to close them in Eclipse, but don't delete them from the filesystem as the Maven build compiles them properly.
1. Create a run configuration. You will probably need to add *-XX:MaxPermSize=256m* or higher to the VM Arguments setting on the Arguments tab.

### Eclipse Project set

There is a project set that can be imported as an alternative to using Maven. This file
is in `android-core/plugins/org.eclipse.andmore/projectSet.psf`.  The project set file is
not guaranteed to be always updated, so if dependency issues arise, use the Maven import.

1. Choose _File > Import... > Team > Team Project Set_
1. Set _URL_ to
https://raw.githubusercontent.com/eclipse/andmore/develop/android-core/plugins/org.eclipse.andmore/projectSet.psf
1. Click _Finish_ to complete the import.
1. Follow steps 3 to 7 from section above.

# Hudson Builds

The status of Andmore builds can be found on the Eclipse Hudson server.

https://hudson.eclipse.org/andmore/

If you submit a pull request, we will submit it to Hudson first to see how it affects the
build. The status of builds against a PR can be found here:

https://hudson.eclipse.org/andmore/job/Andmore-Develop-PullRequests/

## Andmore Logo

*The Android robot is reproduced or modified from work created and shared by Google and 
used according to terms described in the Creative Commons 3.0 Attribution License.*

