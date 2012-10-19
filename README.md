What is fregIDE?
================

*fregIDE* is an Eclipse plugin that supports the [frege programming language](https://github.com/Frege/).

Currently, we do not have a download site for the ready-to-go plugin. We're working on this.

Meanwhile, one can do the following to get the latest frege compiler code in the plugin. 
This assumes that one already has installed the IMP plugin's as described in the [tutorial](https://github.com/Frege/eclipse-plugin/wiki/).

  - clone the frege repository
  - clone the eclipse-plugin under that very name so that `eclipse-plugin/` and `frege/` have the same parent directory.
  - Follow the instructions to [recompile the compiler](https://github.com/Frege/frege/wiki/Getting-Started). 
  - Update the eclipse plugin with the compiler sources: `make -f frege.mk compiler sources`
  - start-up eclipse and import the eclipse-plugin project from filesystem.
  - build the project (don't be scared by 100s of Java warnings - you can get rid of by turning off 4 warning options)
  - You can now make a *Run Configuration* to run the project as eclipse application. Run that.
  - For frege projects, you may need to add `.../frege/build` to the build path.
  - Once this works, you can easily stay up do date by polling frege changes, then make compiler sources again and re-build the eclipse-plugin project
