**Compute project artifacts/dependencies/files checksum digests and output them to individual or summary files.**

A common use case of this plugin is to generate a file containing checksums with the project delivery.


Goals Overview
--------------

General information about the goals.

* [checksum:artifacts](./artifacts-mojo.html) calculate the checksums of the project artifacts.

* [checksum:dependencies](./dependencies-mojo.html) calculate the checksums of the project dependencies.

* [checksum:files ](./files-mojo.html) calculate the checksums of the given files.

* [checksum:file](./file-mojo.html) calculate the checksum of the given file (for command line use).


Usage
-----

General instructions on how to use the checksum plugin can be found on the [usage page](./usage.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in the
[issue tracker](./issue-tracking.html). When creating a new issue, please provide a comprehensive description of your
concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason,
entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated.

Of course, patches are welcome, too. Contributors can check out the project from the
[source repository](./source-repository.html) and will find supplementary information in the
[guide to helping with Maven](http://maven.apache.org/guides/development/guide-helping.html).


Examples
--------

To provide you with better understanding of some usages of checksum-maven-plugin,
you can take a look into the following examples:

* [Generating project artifacts checksums](./examples/generating-project-artifacts-checksums.html)

* [Generating project checksum summary files](./examples/generating-checksum-summary-files.html)

* [Generating project dependencies checksums](./examples/generating-project-dependencies-checksums.html)

* [Invoking from the command line](./examples/invoking-from-the-command-line.html)

* [Using custom checksum algorithms](./examples/using-custom-checksum-algorithms.html)
