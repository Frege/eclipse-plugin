What is fregIDE?
================

*fregIDE* is an Eclipse plugin that supports the [frege programming language](https://github.com/Frege/).

Currently, we do not have an update site for the ready-to-go plugin. We're working on this.
You can [download](https://github.com/Frege/eclipse-plugin/downloads/) a zip-archive and install from that.

Meanwhile, one can do the following to get the latest frege compiler code in the plugin. 
This assumes that one already has installed the IMP plugin's as described in the [tutorial](https://github.com/Frege/eclipse-plugin/wiki/).

  1. Clone the frege repository
  2. Clone the eclipse-plugin under that very name so that `eclipse-plugin/` and `frege/` have the same parent directory.
  3. Follow the instructions to [recompile the compiler](https://github.com/Frege/frege/wiki/Getting-Started). 
  4. Update the eclipse plugin with the compiler sources: `make -f frege.mk compiler sources`
  5. Start-up eclipse and import the eclipse-plugin project from filesystem.
  6. Build the project (don't be scared by 100s of Java warnings - you can get rid of by turning off 4 warning options)
  7. You can now make a *Run Configuration* to run the project as eclipse application. Run that.
  8. Once this works, you can easily stay up do date by polling frege changes, then make compiler sources again and re-build the eclipse-plugin project.

If you can't rebuild the compiler, or just don't want to, 
you can also download the latest java code jar and unpack it below
the `src/` directory before step 5.