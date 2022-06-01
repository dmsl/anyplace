package cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.model

import cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.utils.GeneratedType

data class GeneratedEvent(val genType: GeneratedType, val data: FloatArray, val timestamp: Long) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeneratedEvent

        if (genType != other.genType) return false
        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = genType.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
