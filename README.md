What is fregIDE?
================

*fregIDE* is an Eclipse plugin that supports the [frege programming language](https://github.com/Frege/).

Currently, we do not have an update site for the ready-to-go plugin. We're working on this.
You can [download](https://github.com/Frege/eclipse-plugin/downloads/) a zip-archive and install from that.

If you want to help develop the frege IDE, follow the steps below. 
This assumes that one already has installed the IMP plugin's as described in the [tutorial](https://github.com/Frege/eclipse-plugin/wiki/).

  1. [Download](https://github.com/Frege/frege/downloads/) frege compiler/library jar. 
  2. Clone the eclipse-plugin repository.
  3. Copy the compiler jar to `eclipse-plugin/lib/fregec.jar`.
  4. Fire up eclipse and import repository `eclipse-plugin` as project from filesystem.
  5. Build the project (don't be scared by some Java warnings)
  6. Write code to improve the *fregIDE*, fix bugs, add features ....
  7. You can now make a *Run Configuration* to run the project as eclipse application. Be sure the VM arguments include `-XX:MaxPermSize=256m -XX:+TieredCompilation -Xms40m -Xss4m -Xmx768m`.
  8. Apply and Run.
  9. Once this works, you can replace the fregec.jar and rebuild `eclipse-plugin` whenever the need arises.

