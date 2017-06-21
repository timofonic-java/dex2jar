package com.googlecode.d2j.reader;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.googlecode.d2j.util.Mutf8;

import java.io.UTFDataFormatException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.util.Pair;

public class ProfiledByteBuffer {

    ////////////////////////INACCURATE, BUT FAST, PROFILING CODE////////////////////////

    class SizeAndLocation {
        int size;
        int location;

        public SizeAndLocation(int size, int location) {
            this.size = size;
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            SizeAndLocation that = (SizeAndLocation) o;

            if (size != that.size) { return false; }
            return location == that.location;
        }

        @Override
        public int hashCode() {
            int result = size;
            result = 31 * result + location;
            return result;
        }
    }

    private java.nio.ByteBuffer innerByteBuffer;
    static Multimap<String, SizeAndLocation> memoryAccesses = HashMultimap.create();
    static Map<SizeAndLocation, Integer> countSize = new HashMap<>();
    static Map<Long, SizeAndLocation> locations = new HashMap<>();

    static public String currentClass = "header";
    private void incrementProfile(int index, int size) {
        SizeAndLocation sizeAndLocation = new SizeAndLocation(size, index);
        color(sizeAndLocation);

        if (true) {
            return;
        }
        memoryAccesses.put(currentClass, sizeAndLocation);
        Integer count = countSize.get(sizeAndLocation) != null ? countSize.get(sizeAndLocation) : 0;
        countSize.put(sizeAndLocation, count + 1);

        SizeAndLocation sizeAndLocation1 = locations.get((long) index);
        if (sizeAndLocation1 !=null) {
            System.err.println(sizeAndLocation.size < sizeAndLocation1.size ? sizeAndLocation.size :
                    sizeAndLocation1.size);
        }
    }
    public static void setClass(String clazz) {
        currentClass = clazz;
    }
    public static double print(String className) {
        if (true) {
            return 0;
        }
        double count = 0;
        for (SizeAndLocation sizeAndLocation : memoryAccesses.get(className)) {
            count += (double) sizeAndLocation.size / countSize.get(sizeAndLocation);
        }
        System.out.println(count + ", " + className);
        memoryAccesses.removeAll(className);
        return count;
    }

    ////////////////////////PROFILING CODE////////////////////////


    static ArrayList<String> colorBuffer[] = new ArrayList[10000000];
    static ArrayList<Pair<String, Long>> multiColoring[] = new ArrayList[10000000];// new ArrayList[10000000];
    public static void color(SizeAndLocation sizeAndLocation) {
        for (int i = sizeAndLocation.location; i < sizeAndLocation.size + sizeAndLocation.location; i++) {
            if (colorBuffer[i] == null) {
                colorBuffer[i] = Lists.newArrayList();
            }
            // optimization. if more than six. who cares who owns it.
            if (colorBuffer[i].size() < 6) {
                colorBuffer[i].add(currentClass);
            }
        }
    }
    /**
     * Less accurate than {link @color} but faster. If overlaps happen the attribution can be significantly wrong.
     * As a result I only use this for the string tracking since it would be otherwise unbearably slow.
     * @param sizeAndLocation
     */
    public static void multiColor(SizeAndLocation sizeAndLocation) {
        if (multiColoring[sizeAndLocation.location] == null) {
            multiColoring[sizeAndLocation.location] = new ArrayList<>();
        }
        multiColoring[sizeAndLocation.location].add(new Pair<>(currentClass, (long) sizeAndLocation.size));
    }
    public static double getTotalColoring() {
        double total = 0;
        HashMap<String, Double> classCounts = new HashMap<>();
        for (ArrayList<String> classNames : colorBuffer) {
            if (classNames == null) {
                continue;
            }
            for (String className : classNames) {
                double increment = 1.0 / classNames.size();
                if (classCounts.containsKey(className)) {
                    classCounts.put(className, classCounts.get(className) + increment);
                } else {
                    classCounts.put(className, increment);
                }
                total += increment;
            }
        }
        for (ArrayList<Pair<String, Long>> pairs : multiColoring) {
            if (pairs == null) {
                continue;
            }
            for (Pair<String, Long> pair : pairs) {
                double increment = pair.getValue() / pairs.size();
                String className = pair.getKey();
                if (classCounts.containsKey(className)) {
                    classCounts.put(className, classCounts.get(className) + increment);
                } else {
                    classCounts.put(className, increment);
                }
                total += increment;
            }
        }
        for (String s : classCounts.keySet()) {
            System.out.println(classCounts.get(s) + ", " + s);
        }
        System.err.println("Total coloring = " + total);
        return total;
    }

    public static void clear() {
        colorBuffer = new ArrayList[10000000];
        multiColoring = new ArrayList[10000000];
        memoryAccesses.clear();
        countSize.clear();
        locations.clear();
        currentClass = "header";
    }

    ////////////////////////ACCURATE BUT IN-EFFICIENT PROFILING CODE #2////////////////////////
    // Colors every single byte in the dex file by the classes that depended on these bytes.
    // After done converting the dex to class files, we check every byte in the colored array to
    // determine which classes have ownership of which bytes.
    //
    // This is particularly slow for the string portions.

    public ProfiledByteBuffer(java.nio.ByteBuffer innerByteBuffer) {
        this.innerByteBuffer = innerByteBuffer;
    }

    public ProfiledByteBuffer duplicate() {
        return new ProfiledByteBuffer(innerByteBuffer.duplicate());
    }

    public final ProfiledByteBuffer position(int newPosition) {
        innerByteBuffer.position(newPosition);
        return this;
    }

    public short getShort() {
        incrementProfile(position(), 2);
        return innerByteBuffer.getShort();
    }

    public int getInt() {
        incrementProfile(position(), 4);
        return innerByteBuffer.getInt();
    }

    public int getInt(int index) {
        incrementProfile(index, 4);
        return innerByteBuffer.getInt(index);
    }

    public ProfiledByteBuffer asReadOnlyBuffer() {
        return new ProfiledByteBuffer(innerByteBuffer.asReadOnlyBuffer());
    }

    public final ProfiledByteBuffer order(ByteOrder bo) {
        innerByteBuffer.order(bo);
        return this;
    }

    public ProfiledByteBuffer slice() {
        return new ProfiledByteBuffer(innerByteBuffer.slice());
    }

    public final void limit(int a) {
        innerByteBuffer.limit(a);
    }

    public byte get() {
        incrementProfile(position(), 1);
        return innerByteBuffer.get();
    }

    public ProfiledByteBuffer get(byte[] data) {
        incrementProfile(position(), data.length);
        innerByteBuffer.get(data);
        return this;
    }

    public int position() {
        return innerByteBuffer.position();
    }

    public String Mutf8_decode(StringBuilder sb) throws UTFDataFormatException {
        int position = innerByteBuffer.position();
        String str = Mutf8.decode(innerByteBuffer, sb);
        int position2 = innerByteBuffer.position();
        ProfiledByteBuffer.multiColor(new SizeAndLocation(position2 - position, position));
        // Results in a significantly bigger value.
        //ProfiledByteBuffer.color(new SizeAndLocation(position2 - position, position));
        return str;
    }
}
