package com.polydus.hextileslib

import java.io.Serializable

class Tile(val id: Int, val hexMap: HexMap): Serializable, Comparable<Tile> {

    var y1 = 0
        private set
    var y2 = 0
        private set
    var x = 0
        private set
    val y: Int
        get() = y1 + y2

    //flip when this tile has impassable terrain. Used in pathfinding.
    //var impassable = false

    //use when you use pathfinding and want to exlude certain terrain.
    var terrainType = -1

    companion object {
        const val ADJACENT_TOP = 0
        const val ADJACENT_TOP_RIGHT = 1
        const val ADJACENT_BOTTOM_RIGHT = 2
        const val ADJACENT_BOTTOM = 3
        const val ADJACENT_BOTTOM_LEFT = 4
        const val ADJACENT_TOP_LEFT = 5

        fun oppositeIndex(index: Int): Int{
            when(index){
                ADJACENT_TOP -> return ADJACENT_BOTTOM
                ADJACENT_TOP_RIGHT -> return ADJACENT_BOTTOM_LEFT
                ADJACENT_BOTTOM_RIGHT -> return ADJACENT_TOP_LEFT
                ADJACENT_BOTTOM -> return ADJACENT_TOP
                ADJACENT_BOTTOM_LEFT -> return ADJACENT_TOP_RIGHT
                ADJACENT_TOP_LEFT -> return ADJACENT_BOTTOM_RIGHT
            }
            return -1
        }
    }
    
    /*
     *    Tile layout diagram
     *
     *                         Higher y1                    Higher y2
     *                            \\                          //
     *                             \\                        //
     *                              \\                      //
     *                               \\                    //
     *                                \\                  //
     *                                 \\   /--------\   //          
     *                                     /          \   
     *                                    /            \ 
     *                           /--------\    top     /--------\
     *                          /          \          /          \
     *                         /            \--------/            \
     *                         \  topLeft   /--------\  topRight  /
     *                          \          /          \          /
     *          <--------------  \--------/            \--------/  -------------->
     *  Lower X <--------------  /--------\    this    /--------\  --------------> Higher X
     *                          /          \          /          \
     *                         /            \--------/            \
     *                         \ bottomLeft /--------\ bottomRight/
     *                          \          /          \          /
     *                           \--------/            \--------/
     *                                    \   bottom   / 
     *                                     \          /   
     *                                //    \--------/   \\
     *                               //                   \\
     *                              //                     \\
     *                             //                       \\
     *                            //                         \\
     *                           //                           \\
     *                        Lower y1                     Lower y2
     *
     *
     * Adjacent tiles are assigned in a clockwise direction from
     * the top. Adjacent tiles can be null when they don't exist.
     * 
     */
    
    var top: Tile? = null
        get() { return hexMap.getTile(y1 + 1, y2 + 1, x + 0) }

    var topRight: Tile? = null
        get() { return hexMap.getTile(y1 + 0, y2 + 1, x + 1) }

    var bottomRight: Tile? = null
        get() { return hexMap.getTile(y1 + -1, y2 + 0, x + 1) }

    var bottom: Tile? = null
        get() { return hexMap.getTile(y1 + -1, y2 + -1, x + 0) }

    var bottomLeft: Tile? = null
        get() { return hexMap.getTile(y1 + 0, y2 + -1, x + -1) }

    var topLeft: Tile? = null
        get() { return hexMap.getTile(y1 + 1, y2 + 0, x + -1) }

    fun getAdjacent(direction: Int): Tile?{
        val index: Int =
                if(direction < 0){
                    if(direction % 6 == 0){
                        0
                    } else {
                        (direction % 6) + 6
                    }
                } else {
                    direction % 6
                }
        when(index){
            ADJACENT_TOP -> return top
            ADJACENT_TOP_RIGHT -> return topRight
            ADJACENT_BOTTOM_RIGHT -> return bottomRight
            ADJACENT_BOTTOM -> return bottom
            ADJACENT_BOTTOM_LEFT -> return bottomLeft
            ADJACENT_TOP_LEFT -> return topLeft
        }
        return null
    }

    /***
     * empty constructor for serialization purposes
     */

    constructor(): this(0, HexMap())

    /**
     * Creates an array of all adjacent tiles in clockwise order, or empty
     * array if no valid tiles exist. There should never be an empty array
     * returned unless the map consists of only one tile.
     *
     * @return the array
     * */
    fun getAdjacents(): List<Tile?>{
        return listOf(top, topRight, bottomRight, bottom, bottomLeft, topLeft)
    }

    /**
     * Creates an array of all non-null adjacent tiles in clockwise order,
     * or empty array if no valid tiles exist. There should never be an
     * empty array returned unless the map consists of only one tile.
     *
     * @return the array
     * */
    fun getNonNullAdjacents(): List<Tile>{
        return listOf(top, topRight, bottomRight, bottom, bottomLeft, topLeft)
                .filterNot { it == null }.requireNoNulls()
    }

    /**
     * Creates an array of all non-null adjacent tiles in clockwise order,
     * or empty array if no valid tiles exist. There should never be an
     * empty array returned unless the map consists of only one tile.
     *
     * @return the array
     * */
    fun getPassableAdjacents(): List<Tile>{
        return getPassableAdjacents(null)
    }

    fun getPassableAdjacents(excludeType: Int?): List<Tile>{
        return listOf(top, topRight, bottomRight, bottom, bottomLeft, topLeft)
                .filterNot { it == null }
                .filterNot { it?.terrainType == excludeType }
                .requireNoNulls()
    }

    internal fun init(y1: Int, y2: Int, x: Int){
        this.y1 = y1
        this.y2 = y2
        this.x = x
    }

    fun getDeltaTo(destination: Tile): Int{
        if(destination == this) return 0
        return (
                Math.abs(y1 - destination.y1) +
                Math.abs(y2 - destination.y2) +
                Math.abs(x - destination.x)
                ) / 2
    }
    
    fun isAdjacent(tile: Tile): Boolean{
        if(this.id == tile.id) return false
        if(Math.abs(this.y1 - tile.y1) > 1) return false
        if(Math.abs(this.y2 - tile.y2) > 1) return false
        if(Math.abs(this.x - tile.x) > 1) return false
        return true
    }

    fun posString(): String{
        return "${y}y|${x}x"
    }

    override fun toString(): String {
        return posString()
        //return "${posString()}|${if (impassable) 1 else 0}i"
    }

    override fun compareTo(other: Tile): Int {
        return getDeltaTo(other)
    }

    //pathfinding vars, used internally

    internal var distToOrigin = -1
    internal var distToTarget = -1

    internal var pathCost = 0
        get() = distToOrigin + distToTarget

    internal var parent: Tile? = null

    internal fun resetPathVars(){
        distToOrigin = -1
        distToTarget = -1
        parent = null
    }

}