package com.polydus.hextileslib

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class HexMap(val width: Int, val height: Int): Serializable{

    private var tiles = arrayListOf<Tile>()
    private val tileMap = HashMap<String, Tile>()

    private val cornerTiles: Array<Tile>

    lateinit var bottomLeftTile: Tile
        private set
    lateinit var bottomRightTile: Tile
        private set
    lateinit var topLeftTile: Tile
        private set
    lateinit var topRightTile: Tile
        private set

    init {
        if(width < 1 || height < 1) throw Exception("can't have < 1 width or height!")

        var initX = 1
        var initY = 0

        var lowestX = 0

        for(columns in 0 until width){
            if(columns % 2 == 0){
                initX--
            } else {
                initY++
            }
            for(rows in 0 until height){
                val tile = Tile(tiles.size, this)
                tiles.add(tile)

                tile.init(initX + rows, initY + rows, columns)

                if(tile.y1 < lowestX) lowestX = tile.y1

                if(columns == 0){
                    if(rows == 0) bottomLeftTile = tile
                    if(rows == height - 1) topLeftTile = tile
                }
                if(columns == width - 1){
                    if(rows == 0) bottomRightTile = tile
                    if(rows == height - 1) topRightTile = tile
                }
            }
        }
        cornerTiles = arrayOf(
                bottomLeftTile, bottomRightTile,
                topRightTile, topLeftTile)

        val offset = -lowestX

        tileMap.clear()

        for(tile in tiles){
            tile.init(tile.y1 + offset, tile.y2, tile.x)


            tileMap[tile.toString()] = tile
        }

    }

    /**
     * Creates a List<Tile> that represent a path from
     * the origin tile to the destination tile.
     *
     * @param origin
     * current tile, not included in result
     *
     * @param target
     * destination tile, included in result
     *
     * @return the List<Tile>, or empty List if no valid path is found.
     * */
    fun getPath(origin: Tile, target: Tile): List<Tile>{
        if(origin == target) return listOf()
        if(origin.impassable || target.impassable) return listOf()
        //check if origin or target can be reached at all
        if(origin.getPassableAdjacents().isEmpty()
                || target.getPassableAdjacents().isEmpty()) return listOf()

        for(t in tiles) t.resetPathVars()

        val openList = HashSet<Tile>()
        val closedList = HashSet<Tile>()

        openList.add(origin)

        origin.distToOrigin = 0
        origin.distToTarget = origin.getDeltaTo(target)

        while(!openList.isEmpty()){
            var tile = openList.elementAt(0)

            for(t in openList){
                if(t.pathCost < tile.pathCost) tile = t
            }
            if(tile == target) break

            openList.remove(tile)
            closedList.add(tile)

            val adjacents = tile.getPassableAdjacents()

            for(a in adjacents){
                val newDistToOrigin = tile.distToOrigin + 1
                if(newDistToOrigin < a.distToOrigin){
                    openList.remove(a)
                    closedList.remove(a)
                }
                if(!openList.contains(a) && !closedList.contains(a)){
                    a.distToOrigin = newDistToOrigin
                    a.distToTarget = a.getDeltaTo(target)
                    a.parent = tile
                    openList.add(a)
                }
            }
        }

        val result = ArrayList<Tile>()

        var tile: Tile? = target
        while(tile != null && tile.parent != null){
            result.add(tile)
            tile = tile.parent
        }

        return result.reversed()
    }

    fun getTile(id: Int): Tile?{
        try {
            return tiles[id]
        } catch (e: IndexOutOfBoundsException){
            return null
        }
    }

    fun getTile(y1: Int, y2: Int, x: Int): Tile?{
        if(y1 < 0 || y2 < 0 || x < 0) return null //can't have negative indices

        return try {
            tileMap["$y1.$y2.$x"]!!
        } catch (e: NullPointerException){
            null
        }
    }

    fun getTileWithDelta(origin: Tile, y1: Int, y2: Int, x: Int): Tile?{
        return getTile(origin.y1 + y1, origin.y2 + y2, origin.x + x)
    }

    fun tileAmount(): Int{
        return tiles.size
    }
}