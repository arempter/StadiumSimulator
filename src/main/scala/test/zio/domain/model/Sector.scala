package test.zio.domain.model

case class SectorWithCapacity(name: String, capacity: Capacity = Capacity(10, 25))
case class Sector(name: String)
