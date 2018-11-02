package com.polydus.hextileslib

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class TestMap {

    lateinit var map: HexMap

    @Test
    fun testSize(){
        val width = 100
        val height = 100
        map = HexMap(width, height)

        val size = serialize(map).size
        println("${map.tileAmount()} tiles. ${size / 1000}kb map obj size. ${size / map.tileAmount()}bytes/tile.")
    }

    @Test
    fun testAdjacents() {
        val width = 100
        val height = 100
        map = HexMap(width, height)

        assertNotNull((map.bottomRightTile.getAdjacent(Tile.ADJACENT_TOP_LEFT)))

        assertEquals(
                map.bottomRightTile.getAdjacent(Tile.ADJACENT_TOP_LEFT),
                map.bottomRightTile.getAdjacent(-109))

        assertEquals(
                map.bottomRightTile.getAdjacent(Tile.ADJACENT_TOP_LEFT),
                map.bottomRightTile.getAdjacent(113))
        assertNotEquals(map.bottomRightTile.getAdjacent(Tile.ADJACENT_TOP_LEFT),
                map.bottomRightTile.getAdjacent(114))
    }

    @Test
    fun testDiamondMap(){
        val width = 100
        val height = 100
        map = HexMap(width, height, HexMap.MapShape.Diamond)

        val size = serialize(map).size
        println("${map.tileAmount()} tiles. ${size / 1000}kb map obj size. ${size / map.tileAmount()}bytes/tile.")
    }

    @Test
    fun test2(){
        val width = 10
        val height = 10
        map = HexMap(width, height)

        var tile = map.bottomLeftTile
        while(true){
            if(tile.top == null){
                println(map.topLeftTile)
                println(tile)
                //assert(tile == map.topLeftTile)
                break
            }
            tile = tile.top!!
        }
    }

    @Test
    fun test(){
        val width = 100
        val height = 100
        map = HexMap(width, height)

        assertNotNull(map.bottomRightTile)
        assertNotNull(map.bottomLeftTile)
        assertNotNull(map.topLeftTile)
        assertNotNull(map.topRightTile)

        var tile = map.bottomLeftTile
        while(true){
            if(tile.top == null){
                assert(tile == map.topLeftTile)
                break
            }
            tile = tile.top!!
        }

        assert(map.bottomLeftTile.getDeltaTo(map.topLeftTile) == height - 1)
        assert(map.bottomLeftTile.getDeltaTo(map.bottomRightTile) == width - 1)
    }

    @Test
    fun testImpossiblePath(){
        val width = 10
        val height = 10
        map = HexMap(width, height)
        val excludedType = 1
        val blockedTile = map.getTile(
                map.bottomLeftTile.y1 + -3,
                map.bottomLeftTile.y2 + 3,
                map.bottomLeftTile.x + 6)
        blockedTile!!.terrainType = excludedType

        var next: Tile? = blockedTile
        while(next != null){
            next.terrainType = excludedType
            next = next.top
        }

        val a = map.getPath(map.bottomLeftTile, map.bottomRightTile, excludedType)
        assert(a.isEmpty())
    }

    @Test
    fun testPath(){
        val width = 100
        val height = 100
        map = HexMap(width, height)
        val time = System.nanoTime()
        val path = map.getPath(map.bottomLeftTile, map.topRightTile, null)
        val diff = TimeUnit.MILLISECONDS.convert(System.nanoTime() - time, TimeUnit.NANOSECONDS)
        println("Generating ${path.size} tile path in a ${width}x${height} map took ${diff}ms")
    }


    @Test
    fun testSpeed(){
        val width = 100
        val height = 100
        val time = System.nanoTime()
        map = HexMap(width, height)
        val diff = TimeUnit.MILLISECONDS.convert(System.nanoTime() - time, TimeUnit.NANOSECONDS)
        println("Generating ${width}x${height} map took ${diff}ms")
    }

    /*@Test
    fun testForInvalidArguments() {
        val exception = assertThrows<Exception> { HexMap(40, -50) }
        assert(exception.message == "can't have < 1 width or height!")
    }*/

    @Throws(IOException::class)
    private fun serialize(obj: Any): ByteArray {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(obj)
        return baos.toByteArray()
    }
}