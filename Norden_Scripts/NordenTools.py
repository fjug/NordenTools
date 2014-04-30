from ini.trakem2 import Project
from ini.trakem2.utils import AreaUtils
from ini.trakem2.display import Display, AreaList, Selection, Patch, ZDisplayable
from java.awt import Color
from ij.plugin import ImageCalculator
from ij import IJ
from ij import VirtualStack
from ij import ImagePlus
from ij import WindowManager

def exportMask(evt=None):
  display = Display.getFront()
  arealists = display.getSelection().getSelected(AreaList)
  canvas = display.getCanvas()
  numLayer = display.getLayerSet().size();
  if arealists.isEmpty():
    IJ.log("No arealists selected -- I just take the FIRST ONE!!")
    #IJ.showMessage("No arealists selected -- select one!")
    zDispList = display.getLayerSet().getDisplayableList()
    if len(zDispList) > 0:
      arealists = zDispList
    else:
      IJ.log("Project does not contain any AreaLists!")
      IJ.showMessage("Project does not contain any AreaLists!")
      return
  AreaList.exportAsLabels(arealists, canvas.getFakeImagePlus().getRoi(), 1.0, 0, numLayer-1, False, False, False);
  #IJ.selectWindow("Labels");
  #IJ.run("16-bit");
   
def importMask(evt=None):
  display = Display.getFront()
  # Obtain an image stack
  #imp = IJ.getImage()
  imp = WindowManager.getImage("Labels")

  layerset = display.getLayerSet()
  p = layerset.getProject()

  ali = AreaList(p, "", 0, 0)
  layerset.add(ali)
  p.getProjectTree().insertSegmentations([ali])

  # Obtain the image stack
  stack = imp.getImageStack()
  # Iterate every slice of the stack
  for i in range(1, imp.getNSlices() + 1):
    ip = stack.getProcessor(i) # 1-based
    # Extract all areas (except background) into a map of value vs. java.awt.geom.Area
    m = AreaUtils.extractAreas(ip)
    # Report progress
    if len(m) > 0:
      IJ.log(str(i) + ":" + str(len(m)))
    # Get the Layer instance at the corresponding index
    layer = layerset.getLayers().get(i-1) # 0-based
    # Add the first Area instance to the AreaList at the proper Layer
    if ( m.values().iterator().hasNext() ):
      ali.addArea(layer.getId(), m.values().iterator().next())

  # Change the color of the AreaList
  ali.setColor(Color.red)
  ali.adjustProperties()
  # Ensure bounds are as constrained as possible
  ali.calculateBoundingBox(None)
  # Repaint
  Display.repaint()
  
def openStack(evt=None):
  display = Display.getFront()
  canvas = display.getCanvas()
  numLayer = display.getLayerSet().size();
  
  files = []
  for l in display.getLayerSet().getLayers():
    for p in l.getDisplayables(Patch):
      files.append(p.getFilePath())
  # Create virtual stack 'vs'
  vs = None
  for f in files:
    # if first image...
    if vs is None:
      imp = IJ.openImage(f)
      vs = VirtualStack(imp.width, imp.height, None, "/")
    vs.addSlice(f)
  layerset = display.getLayerSet()
  p = layerset.getProject()
  ImagePlus("VSeg", vs).show()
  IJ.run("Duplicate...", "title=" + p.getTitle() + " duplicate range=1-30");
  WindowManager.getImage("VSeg").close()
  return p.getTitle()
    
def exportSegmentation(evt=None):
  display = Display.getFront()
  canvas = display.getCanvas()
  numLayer = display.getLayerSet().size();

  exportMask(evt)
  
  files = []
  for l in display.getLayerSet().getLayers():
    for p in l.getDisplayables(Patch):
      files.append(p.getFilePath())
  # Create virtual stack 'vs'
  vs = None
  for f in files:
    # if first image...
    if vs is None:
      imp = IJ.openImage(f)
      vs = VirtualStack(imp.width, imp.height, None, "/")
    vs.addSlice(f)
  ImagePlus("VSeg", vs).show()
  IJ.run("Duplicate...", "title=Segmentation duplicate range=1-30");
  WindowManager.getImage("VSeg").close()
  ic = ImageCalculator()
  ic.run("Multiply stack", WindowManager.getImage("Segmentation"), WindowManager.getImage("Labels"));
  WindowManager.getImage("Labels").close()