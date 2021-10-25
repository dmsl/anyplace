package utils

import java.io._
import java.nio.file.Files

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{AbstractController, ControllerComponents}

@Singleton
class AnyPlaceTilerHelper @Inject()(cc: ControllerComponents,
                                conf: Configuration,
                                api: AnyplaceServerAPI)
  extends AbstractController(cc) {

    private val ANYPLACE_TILER_SCRIPT_START =  "start-anyplace-tiler.sh"
    private val FLOOR_PLANS_ROOT_DIR = "floor_plans" + File.separatorChar
    private val FLOOR_TILES_DIR = "static_tiles" + File.separatorChar

    val FLOOR_TILES_ZIP_NAME = "tiles_archive.zip"

    def getTilerScriptStart(): String = {
        conf.get[String]("tilerRootDir")  + File.separatorChar + ANYPLACE_TILER_SCRIPT_START
    }

    def getRootFloorPlansDir(): String = {
        conf.get[String]("floorPlansRootDir") + File.separatorChar
    }

    def getRootFloorPlansDirFor(buid: String): String = {
        getRootFloorPlansDir() + buid + File.separatorChar
    }

    def getRootFloorPlansDirFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid) + "fl_" + floor + File.separatorChar
    }

    def getFloorPlanFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid, floor) + "fl_" + floor
    }

    def getFloorTilesDirFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid, floor) + FLOOR_TILES_DIR
    }

    def getFloorTilesZipFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        getRootFloorPlansDirFor(buid, floor) + FLOOR_TILES_DIR +
          FLOOR_TILES_ZIP_NAME
    }

    // CHECK:PM
    def getFloorTilesZipLinkFor(buid: String, floor: String): String = {
        if (buid.trim().isEmpty || floor.trim().isEmpty) {
            return null
        }
        //TODO:NN from /developers .. this must also change..
        api.SERVER_FULL_URL + api.sep +
          "anyplace/floortiles/" + buid + api.sep + floor + api.sep + FLOOR_TILES_ZIP_NAME
    }

    /**
     * Stores floorplan(images) to fileSystem.
     * @param buid
     * @param floor_number
     * @param file
     * @return
     */
    def storeFloorPlanToServer(buid: String, floor_number: String, file: File): File = {
        LOG.D2("storeFloorPlanToServer")
        val dirS = getRootFloorPlansDirFor(buid, floor_number)
        val dir = new File(dirS)

        LOG.D3("storeFloorPlanToServer: dir: " + dir.getAbsolutePath)
        LOG.D3("storeFloorPlanToServer: file: " + file.getAbsolutePath)
        LOG.D3("storeFloorPlanToServer: file size: " + file.length())
        dir.mkdirs()
        if (!dir.isDirectory || !dir.canWrite || !dir.canExecute) {
            throw new AnyPlaceException("Floor plans directory is inaccessible!")
        }
        val name = "fl" + "_" + floor_number
        val dest_f = new File(dir, name)
        var fout: FileOutputStream = null
        fout = new FileOutputStream(dest_f)
        Files.copy(file.toPath, fout)
        fout.close()
        dest_f
    }

    def tileImage(imageFile: File, lat: String, lng: String): Boolean = {
        if (!imageFile.isFile || !imageFile.canRead()) {
            return false
        }
        val imageDir = imageFile.getParentFile
        if (!imageDir.isDirectory || !imageDir.canWrite() || !imageDir.canRead()) {
            throw new AnyPlaceException("Server do not have the permissions to tile the passed argument[" +
              imageFile.toString +
              "]")
        }
        val pb = new ProcessBuilder(getTilerScriptStart(), imageFile.getAbsolutePath().toString(), lat,
            lng, "-DISLOG")
        val log = new File(imageDir, "anyplace_tiler_" + imageFile.getName + ".log")
        pb.redirectErrorStream(true)
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        try {
            val p = pb.start()
            val is = p.getInputStream
            val br = new BufferedReader(new InputStreamReader(is))
            var line = br.readLine()
            while (line != null) {
                LOG.D(">" + line)
                line = br.readLine()
            }
            p.waitFor()
            if (p.exitValue() != 0) {
                val err = "Tiling for image[" + imageFile.toString + "] failed with exit code[" +
                  p.exitValue() +
                  "]!"
                LOG.E(err)
                throw new AnyPlaceException(err)
            }
        } catch {
            case e: IOException => {
                val err = "Tiling for image[" + imageFile.toString + "] failed with IOException[" +
                  e.getMessage +
                  "]!"
                LOG.E(err)
                throw new AnyPlaceException(err)
            }
            case e: InterruptedException => {
                val err = "Tiling for image[" + imageFile.toString + "] failed with InterruptedException[" +
                  e.getMessage +
                  "]!"
                LOG.E(err)
                throw new AnyPlaceException(err)
            }
        }
        true
    }

    def tileImageWithZoom(imageFile: File, lat: String, lng: String, zoom:String): Boolean = {
        if (!imageFile.isFile || !imageFile.canRead) { return false }

        val imageDir = imageFile.getParentFile
        if (!imageDir.isDirectory || !imageDir.canWrite || !imageDir.canRead) {
            throw new AnyPlaceException("Server do not have the permissions to tile the passed argument[" +
              imageFile.toString + "]")
        }
        val pb = new ProcessBuilder(getTilerScriptStart(), imageFile.getAbsolutePath, lat,
            lng,"-DISLOG",zoom)
        val log = new File(imageDir, "anyplace_tiler_" + imageFile.getName + ".log")
        pb.redirectErrorStream(true)
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        try {
            val p = pb.start()
            val is = p.getInputStream
            val br = new BufferedReader(new InputStreamReader(is))
            var line = br.readLine()
            while (line != null) {
                LOG.D(">" + line)
                line = br.readLine()
            }
            p.waitFor()
            if (p.exitValue() != 0) {
                val err = "Tiling for image[" + imageFile.toString + "] failed with exit code[" +
                  p.exitValue() +
                  "]!"
                LOG.E(err)
                throw new AnyPlaceException(err)
            }
        } catch {
            case e: IOException => {
                val err = "Tiling for image[" + imageFile.toString + "] failed with IOException[" +
                  e.getMessage +
                  "]!"
                LOG.E(err)
                throw new AnyPlaceException(err)
            }
            case e: InterruptedException => {
                val err = "Tiling for image[" + imageFile.toString + "] failed with InterruptedException[" +
                  e.getMessage +
                  "]!"
                LOG.E(err)
                throw new AnyPlaceException(err)
            }
        }
        true
    }
}


