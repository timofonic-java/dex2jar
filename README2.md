* build with `./gradlew  clean distZip`
* open  dex-tools/build/distributions/dex-tools-2.1-SNAPSHOT.zip
* open dex-tools/build/distributions/dex-tools-2.1-SNAPSHOT/d2j-dex2jar.sh and edit with the following
	* add "-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
* run with java -Xms512m -Xmx1024m -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -classpath "${_classpath}" "com.googlecode.dex2jar.tools.Dex2jarCmd" --force "/Users/brianattwell/Uber/android/buck-out/gen/apps/presidio/sample/presidio-training-sessions/lesson201/bin_debug.apk"

Analysis: https://source.android.com/devices/tech/dalvik/dex-format

Results from a single dex file in Carbon:
	string_ids_size * 4 = 30756 // count of strings in the string identifiers list
	type_ids_size * 4 = 6728 // count of elements in the type identifiers list, at most 65535
	field_ids_size * 12 = 32460
	method_ids_size * 8 = 21608
	class_defs_size * 32 = 38496
	data_size = 


How well does this match with the dex size:

My ultimiate goal is to find the size of each class file:
* what approach should I take? 
	* try and hook into the last step of he class creation step and find all attributing dex elements? Will this work? Does DexClassNode reference the original elements in all cases?
	* alternative: hook into the part of DexFileReader that parses the actual class. See DexFileReader#611.
		--> Add breakpoint and take a look at the values that exist here. Might be illuminating.
		--> DexFileReader#710
		--> start by counting the total method sizes.
	* do something stupid. That just profiles the buffer accesses.
	* alternative: just look at the class files that get output. The dex file is kind of useless anyway?


OTHER THINGS I'M LOOKING INTO:
* just directly dump the class files and look at the sizes in there...
* https://github.com/JesusFreke/smali

RESULTS:
* with multicoloring I get 1.4MB
* with regular coloring: 1.8MB