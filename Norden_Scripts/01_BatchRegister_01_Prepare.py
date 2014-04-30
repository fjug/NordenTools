from ij import IJ
from ij import WindowManager
import glob
import os
import sys
from java.lang.System import getProperty
sys.path.append(getProperty("fiji.dir") + "/plugins/NordenTools")
import NordenTools as nt

from setup import *

def preparePairRegistration(tpA,tpB):
  fileA = "raw_"+tpA+".tif"
  fileB = "raw_"+tpB+".tif"
  
  IJ.open(output_dir+fileA)
  IJ.open(output_dir+fileB)

  IJ.run("Rigid Registration", "initialtransform=[] n=1 tolerance=1.000 level=5 stoplevel=2 materialcenterandbbox=[] " + 
         "template=%s measure=Euclidean"%(fileA))
  IJ.selectWindow("Matrix")
  IJ.saveAs("Text", "/Users/jug/MPI/ProjectNorden/output/matrix_%s.txt"%(tpB))
  window = WindowManager.getWindow("Matrix")
  window.close()
  window = WindowManager.getWindow(output_dir+fileA)
  window.close()
  window = WindowManager.getWindow(output_dir+fileB)
  window.close()


filenum = len(time_points)
if filenum>1:
  num = 1  
  # 1) we go backwards from center
  for i in range((filenum-1)/2,-1,-1):
    IJ.log( "Computing registration matrix (%d/%d)...\n <<< %s <<< %s"%(num,filenum-1,time_points[i],time_points[i+1]) )
    preparePairRegistration(time_points[i+1],time_points[i]) # mind reverse order!
    num+=1
  # 2) we go forward from center
  for i in range((filenum-1)/2+1,filenum-1):
    IJ.log( "Computing registration matrix (%d/%d)...\n >>> %s >>> %s"%(num,filenum-1,time_points[i],time_points[i+1]) )
    preparePairRegistration(time_points[i],time_points[i+1])
    num+=1
else:
    IJ.log( "Registration of ONE file done... Easy! ;)\nYou might consider registering more then one file...")