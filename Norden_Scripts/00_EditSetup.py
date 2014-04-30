from ij import IJ
import sys
from java.lang.System import getProperty
sys.path.append(getProperty("fiji.dir") + "/plugins/NordenTools")
import NordenTools as nt

filename = getProperty("fiji.dir") + "/plugins/NordenTools/setup.py"
IJ.open(filename)