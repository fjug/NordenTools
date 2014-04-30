from ij import IJ
import sys
from java.lang.System import getProperty
sys.path.append(getProperty("fiji.dir") + "/plugins/NordenTools")
import NordenTools as nt

from setup import *

for timepointstring in time_points:
  IJ.log("Processing " + timepointstring + "...")
  project = Project.openFSProject(trakem_dir + timepointstring + ".xml", True)
  Thread.sleep(2000)
  # EXPORT RAW
  windowName = nt.openStack()
  IJ.log(">> "+windowName)
  IJ.selectWindow(windowName);
  IJ.run("Save", "save="+output_dir+"raw_"+timepointstring+".tif");
  IJ.run("Close")
  # EXPORT MASK
  nt.exportMask()
  IJ.selectWindow("Labels");
  IJ.run("Save", "save="+output_dir+"labels_"+timepointstring+".tif");
  IJ.run("Close")
  # EXPORT SEGMENTATION
  nt.exportSegmentation()
  IJ.selectWindow("Segmentation");
  IJ.run("Save", "save="+output_dir+"seg_"+timepointstring+".tif");
  IJ.run("Close")
  # CLOSE PROJECT
  Thread.sleep(1000)
  project.getLoader().setChanged(False) # avoid dialog at closing
  Thread.sleep(1000)
  project.destroy()