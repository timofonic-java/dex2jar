## Build Process 

This is a fork of [dex2jar](https://github.com/pxb1988/dex2jar). Additional code has been added to determine the size breakdown per class. 

Run dex2jar normally in order to convert an apk into jar files. During the transformation, additional profiling code determines which parts of the dex file are read from in order to create each class in the jar. After the transformation is complete, we tally how many bytes of the dex file each class needed to read. Based on this, we subdivide each bytes' blame between all classes that needed the byte.

This is a naive approach and correspondly slow. My hope is that its naivity makes it bulletproof.

## Usage

> sh d2j-dex2jar.sh -f ~/path/to/apk_to_decompile.apk > output.txt

The jar file will be `apk_to_decompile-dex2jar.jar`.
And the analytics about class size will be inside `output.txt`.

## Build Process 

```
cd dex2jar-2.x
gradle clean distZip
# if build successfull, the zip file is under dex-tools/build/distributions/
# unzip by calling the following
open dex-tools/build/distributions/dex-tools-2.1-SNAPSHOT.zip
```
