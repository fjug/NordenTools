from ij import IJ
from ij import WindowManager
import glob
import os
import sys
from java.lang.System import getProperty
sys.path.append(getProperty("fiji.dir") + "/plugins/NordenTools")
import NordenTools as nt

from setup import *


def executePairRegistration(prefix,tp,matrix):
  saveMatrixForTransformJ(matrix)
  infile = prefix+"_"+tp+".tif"
  outfile = "reg_%s_%s.tif"%(prefix,tp)

  IJ.open(output_dir+infile)

  IJ.run("TransformJ Affine", "matrix="+output_dir+"TransformJ_matrix.txt interpolation=linear background=0.0")
  IJ.run("Save", "save="+output_dir+outfile )
  while IJ.macroRunning():
    Thread.sleep(100)
  WindowManager.getWindow(infile).close()
  WindowManager.getWindow(outfile).close()


def readMatrix(filename):
  matrix = []
  if os.path.isfile(filename):
    f = open(filename,'r')
    for line in f:
      matrix.extend(line.strip().split())
    if len(matrix)<16:
      IJ.log( "WARNING: Matrix file does not contain 16 values -- I'll take an identity matrix instead!" )
      matrix = getIdentityTransform()
  else:
    IJ.log( "WARNING: Matrix file not found -- I'll take an identity matrix!" )
    matrix = getIdentityTransform()
  matrix = [float(x) for x in matrix]
  return matrix


def getIdentityTransform():
  return [ 1., 0., 0., 0.,  0., 1., 0., 0.,  0., 0., 1., 0.,  0., 0., 0., 1. ]
  
  
def add(m1, m2):
  return [sum(x) for x in zip(m1, m2)]

  
def sub(m1, m2):
  return [x-y for (x,y) in zip(m1, m2)]

  
def saveMatrixForTransformJ(matrix):
  f = open(output_dir+"TransformJ_matrix.txt", "w")
  order = [ 1, 5, 9, 13,  2, 6, 10, 14,  3, 7, 11, 15,  4, 8, 12, 16 ]
  for i,j in enumerate(order):
    f.write(str(matrix[j-1])+"\t ")
    if (i+1)%4 == 0:
      f.write("\n")
  f.close()



filenum = len(time_points)
if filenum>1:
  
  if 'reg_execution_prefixes' in locals() or 'reg_execution_prefixes' in globals():
    prefixes = reg_execution_prefixes
  else:
    IJ.log( "No variable 'reg_execution_prefixes' set -- using default prefix-values..." )
    prefixes = [ "raw", "seg", "labels" ]
  IJ.log( "Performing registration on following prefixed file-sets: "+str(prefixes) )
  
  # copy center stack (registered by definition)
  # - - - - - - - - - - - - - - - - - - - - - - 
  num = 1
  tpCenter = time_points[(filenum-1)/2+1]
  IJ.log( "Init Registration: copy center stack (%s)."%(tpCenter) )
  imp = IJ.openImage(output_dir+"raw_"+tpCenter+".tif")
  IJ.save(imp,output_dir+"reg_%s.tif"%tpCenter)
  imp.close()
  IJ.log( "Init Registration: create identity transform for center-stack." )
  matrix = getIdentityTransform()
  
  # 1) we go backwards from center
  # - - - - - - - - - - - - - - - -
  for i in range((filenum-1)/2,-1,-1):
    IJ.log( "Step %2d of %2d -- Reading matrix '%s', adding, and applying to '%s'.."%(num,filenum-1,time_points[i],str(prefixes)) )
    m = readMatrix(output_dir+"matrix_"+time_points[i]+".txt")
    matrix = add( matrix, sub(m,getIdentityTransform()) )
    for prefix in prefixes:
      executePairRegistration(prefix,time_points[i],matrix)
    num+=1
  # 2) we go forward from center
  # - - - - - - - - - - - - - - -
  IJ.log( "Back to center stack -- reinit to identity transform." )
  matrix = getIdentityTransform()
  for i in range((filenum-1)/2+1,filenum-1):
    IJ.log( "Step %2d of %2d -- Reading matrix '%s', adding, and applying to '%s'.."%(num,filenum-1,time_points[i],str(prefixes)) )
    m = readMatrix(output_dir+"matrix_"+time_points[i+1]+".txt")
    matrix = add( matrix, sub(m,getIdentityTransform()) )
    for prefix in prefixes:
      executePairRegistration(prefix,time_points[i+1],matrix)
    num+=1
    
else:
  IJ.log( "Registration of ONE file done... Easy! ;)\nYou might consider registering more then one file...")