package com.polydus.hextileslib

import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class HexMap(val width: Int, val height: Int, shape: MapShape): Serializable{

    private val tiles = ArrayList<Tile>()
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
    lateinit var midTile: Tile
        private set

    enum class MapShape(type: Int){
        Rectangle(0),
        Diamond(1)
    }

    /***
     * If no shape is passed, the default shape is a Rectangle
     */

    constructor(width: Int, height: Int) : this(width, height, MapShape.Rectangle)

    /***
     * empty constructor for serialization purposes
     */

    constructor(): this(4, 4)

    init {
        if(width < 4 || height < 4) throw Exception("can't have < 4 width or height!")

        var initX = 1
        var initY = 0

        var lowestY1 = 0
        var lowestY2 = 0
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

                if(columns == width / 2 && rows == height / 2){
                    midTile = tile
                }

                if(tile.y1 < lowestY1) lowestY1 = tile.y1
                if(tile.y2 < lowestY2) lowestY2 = tile.y2
                if(tile.x < lowestX) lowestX = tile.x

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

        when(shape){
            HexMap.MapShape.Rectangle -> { }
            HexMap.MapShape.Diamond -> {
                val removedTiles = ArrayList<Tile>()
                val limit = ((width + height) / 2) / 3
                for(t in tiles){
                    if(midTile.getDeltaTo(t) > limit){
                        removedTiles.add(t)
                    }
                }
                tiles.removeAll(removedTiles)

                //reset all the tiles. Not optimal, I know
                val newTiles = ArrayList<Tile>()
                lowestY1 = 0
                lowestY2 = 0
                lowestX = 0

                for(t in tiles){
                    val tile = Tile(newTiles.size, this)
                    newTiles.add(tile)
                    tile.init(t.y1, t.y2, t.x)
                    if(t == midTile) midTile = tile
                    if(tile.y1 < lowestY1) lowestY1 = tile.y1
                    if(tile.y2 < lowestY2) lowestY2 = tile.y2
                    if(tile.x < lowestX) lowestX = tile.x
                }
                tiles.clear()
                tiles.addAll(newTiles)
            }
        }

        tileMap.clear()

        for(tile in tiles){
            tile.init(tile.y1 + -lowestY1, tile.y2 + -lowestY2, tile.x + -lowestX)
            tileMap[tile.posString()] = tile
        }

        var nextTile: Tile? = midTile.bottomLeft
        while(nextTile != null){
            bottomLeftTile = nextTile
            nextTile = nextTile.bottomLeft
        }

        nextTile = midTile.bottomRight
        while(nextTile != null){
            bottomRightTile = nextTile
            nextTile = nextTile.bottomRight
        }

        nextTile = midTile.topLeft
        while(nextTile != null){
            topLeftTile = nextTile
            nextTile = nextTile.topLeft
        }

        nextTile = midTile.topRight
        while(nextTile != null){
            topRightTile = nextTile
            nextTile = nextTile.topRight
        }

        while(topLeftTile.top != null) topLeftTile = topLeftTile.top!!
        while(topRightTile.top != null) topRightTile = topRightTile.top!!
        while(bottomLeftTile.bottom != null) bottomLeftTile = bottomLeftTile.bottom!!
        while(bottomRightTile.bottom != null) bottomRightTile = bottomRightTile.bottom!!

        cornerTiles = arrayOf(bottomLeftTile, bottomRightTile,
                topRightTile, topLeftTile)
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
     * @param excludeType
     * excludes tiles of a certain type, or null if not excluding
     * any type.
     *
     * @return the List<Tile>, or empty List if no valid path is found.
     * */

    fun getPath(origin: Tile, target: Tile, excludeType: Int?): List<Tile>{
        if(origin == target) return emptyList()
        if(origin.terrainType == excludeType || target.terrainType == excludeType) return emptyList()
        //check if origin or target can be reached at all
        if(origin.getPassableAdjacents(excludeType).isEmpty()
                || target.getPassableAdjacents(excludeType).isEmpty()) return emptyList()

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

            val adjacents = tile.getPassableAdjacents(excludeType)

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
        while(tile?.parent != null){
            result.add(tile)
            tile = tile.parent
        }

        return result.reversed()
    }

    fun getPath(originID: Int, targetID: Int, excludeType: Int?): List<Tile>{
        val origin = getTile(originID) ?: return emptyList()
        val target = getTile(targetID) ?: return emptyList()
        return getPath(origin, target, excludeType)
    }

    /**
     * Creates a List<Tile> that represents the closest path from
     * the origin tile to a tile that is adjacent to the target.
     *
     * @param origin
     * current tile, not included in result
     *
     * @param target
     * destination tile, included in result
     *
     * @param excludeType
     * excludes tiles of a certain type, or null if not excluding
     * any type.
     *
     * @return the List<Tile>, or empty List if no valid path is found.
     * */

    fun getPathToAdjacentOf(origin: Tile, target: Tile, excludeType: Int?): List<Tile>{
        if(origin == target) return emptyList()
        if(origin.getPassableAdjacents(excludeType).isEmpty()
                || target.getPassableAdjacents(excludeType).isEmpty()) return emptyList()
        if(origin.terrainType == excludeType) return emptyList()

        val adj = target.getPassableAdjacents(excludeType)
        var result = listOf<Tile>()

        for(a in adj){
            val path = getPath(origin, a, excludeType)
            if(path.isNotEmpty() && (path.size < result.size || result.isEmpty())){
                result = path
            }
        }

        return result
    }

    fun getPathToAdjacentOf(originID: Int, targetID: Int, excludeType: Int?): List<Tile>{
        val origin = getTile(originID) ?: return emptyList()
        val target = getTile(targetID) ?: return emptyList()
        return getPathToAdjacentOf(origin, target, excludeType)
    }

    fun getTile(id: Int): Tile?{
        return try {
            tiles[id]
        } catch (e: IndexOutOfBoundsException){
            null
        }
    }

    fun getTile(y1: Int, y2: Int, x: Int): Tile?{
        if(y1 < 0 || y2 < 0 || x < 0) return null //can't have negative indices
        return getTile(y1 + y2, x)
    }

    fun getTile(y: Int, x: Int): Tile?{
        if(y < 0 || x < 0) return null //can't have negative indices
        return try {
            tileMap["${y}y|${x}x"]!!
        } catch (e: NullPointerException){
            null
        }
    }

    fun getTileWithDelta(origin: Tile, y1: Int, y2: Int, x: Int): Tile?{
        return getTile(origin.y1 + y1, origin.y2 + y2, origin.x + x)
    }

    /**
     *
     * Returns tiles that are within a certain range of the origin.
     * @return List<Tile> with the origin and other tiles within the delta range (inclusive)
     *
     * */

    fun getTilesWithinDelta(origin: Tile, delta: Int): List<Tile>{
        if(delta < 0) return emptyList()
        if(delta == 0) return listOf(origin)
        val result = ArrayList<Tile>()
        for(t in tiles) if(t.isAdjacent(origin) || t.getDeltaTo(origin) <= delta) result.add(t)
        return result
    }

    /**
     *
     * Returns tiles that are have a specific delta to the origin.
     * @return List<Tile> with the origin and other tiles with the delta.
     *
     * */

    fun getTilesWithDelta(origin: Tile, delta: Int): List<Tile>{
        if(delta < 0) return emptyList()
        if(delta == 0) return listOf(origin)
        if(delta == 1) return origin.getNonNullAdjacents()
        val result = ArrayList<Tile>()
        for(t in tiles) if(t.getDeltaTo(origin) == delta) result.add(t)
        return result
    }

    fun tileAmount(): Int{
        return tiles.size
    }
}