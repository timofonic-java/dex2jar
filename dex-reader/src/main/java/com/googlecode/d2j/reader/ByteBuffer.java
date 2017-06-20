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

public class ByteBuffer {

    ////////////////////////PROFILING CODE////////////////////////

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

    // technically this double counts some things.... if you have an access (1,20) and (1,5) then the (1,5) should be
    // subsumed.

    private java.nio.ByteBuffer innerByteBuffer;
    static Multimap<String, SizeAndLocation> memoryAccesses = HashMultimap.create();
    static Map<SizeAndLocation, Integer> countSize = new HashMap<>();
    static Map<Long, SizeAndLocation> locations = new HashMap<>();

    static public String currentClass = "header";
    private void incrementProfile(int index, int size) {
        SizeAndLocation sizeAndLocation = new SizeAndLocation(size, index);
        memoryAccesses.put(currentClass, sizeAndLocation);
        Integer count = countSize.get(sizeAndLocation) != null ? countSize.get(sizeAndLocation) : 0;
        countSize.put(sizeAndLocation, count + 1);

        SizeAndLocation sizeAndLocation1 = locations.get((long) index);
        if (sizeAndLocation1 !=null) {
            System.err.println(sizeAndLocation.size < sizeAndLocation1.size ? sizeAndLocation.size :
                    sizeAndLocation1.size);
        }

        color(sizeAndLocation);
    }
    public static void setClass(String clazz) {
        currentClass = clazz;
    }
    public static void print(String className) {
        double count = 0;
        for (SizeAndLocation sizeAndLocation : memoryAccesses.get(className)) {
            count += (double) sizeAndLocation.size / countSize.get(sizeAndLocation);
        }
        System.out.println(count + ", " + className);
        memoryAccesses.removeAll(className);
    }

    ////////////////////////PROFILING CODE////////////////////////

    static ArrayList<String> colorBuffer[] = new ArrayList[10000000];
    public static void color(SizeAndLocation sizeAndLocation) {
        for (int i = sizeAndLocation.location; i < sizeAndLocation.size + sizeAndLocation.location; i++) {
            if (colorBuffer[i] == null) {
                colorBuffer[i] = Lists.newArrayList();
            }
            //colorBuffer[i].add(currentClass);
        }
    }
    public static int getTotalColoring() {
        int count = 0;
        for (ArrayList<String> strings : colorBuffer) {
            if (strings != null) {
                count++;
                System.out.println("1");

            } else {
                System.out.println("0");
            }
        }
        System.out.println("Total coloring: " + count);
        return count;
    }

    ////////////////////////PROFILING CODE #2////////////////////////

    //public static int count = 0;

    public ByteBuffer(java.nio.ByteBuffer innerByteBuffer) {
        this.innerByteBuffer = innerByteBuffer;
    }

    public ByteBuffer duplicate() {
        return new ByteBuffer(innerByteBuffer.duplicate());
    }

    public final ByteBuffer position(int newPosition) {
        innerByteBuffer.position(newPosition);
        return this;
    }

    public short getShort() {
        incrementProfile(position(), 2);
        //System.out.println("getShort()");
        return innerByteBuffer.getShort();
    }

    public int getInt() {
        //System.out.println("getInt()");
        incrementProfile(position(), 4);
        return innerByteBuffer.getInt();
    }

    public int getInt(int index) {
        //System.out.println("getInt()");
        incrementProfile(index, 4);
        return innerByteBuffer.getInt(index);
    }

    public ByteBuffer asReadOnlyBuffer() {
        return new ByteBuffer(innerByteBuffer.asReadOnlyBuffer());
    }

    public final ByteBuffer order(ByteOrder bo) {
        innerByteBuffer.order(bo);
        return this;
    }

    public ByteBuffer slice() {
        return new ByteBuffer(innerByteBuffer.slice());
    }

    public final void limit(int a) {
        innerByteBuffer.limit(a);
    }

    public byte get() {
        incrementProfile(position(), 1);
        return innerByteBuffer.get();
    }

    public ByteBuffer get(byte[] data) {
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
        ByteBuffer.color(new SizeAndLocation(position, position2 - position));
        incrementProfile(position(), position2 - position);
        return str;
    }
}
