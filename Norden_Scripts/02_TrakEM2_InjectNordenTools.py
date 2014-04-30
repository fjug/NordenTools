# Florian Jug for the Norden Lab 
# 2013-10, MPI-CBG
# Thanks to Albert Cardona for code template...

from ini.trakem2.display import Display
import sys
from java.lang.System import getProperty
sys.path.append(getProperty("fiji.dir") + "/plugins/NordenTools")
import NordenTools as nt

# for development: reload new source
nt = reload(nt)

def injectNordenTools(display):
  tabs = display.getTabbedPane()
  # Check that it's not there already
  title = "Norden Tools"
  for i in range(tabs.getTabCount()):
    if tabs.getTitleAt(i) == title:
      tabs.remove(i)
      #IJ.showMessage("Norden Tools have already been injected!")
      #return
  # Otherwise, add it new:
  from javax.swing import JPanel, JButton
  panel = JPanel()
  btnExportMask = JButton("Export mask", actionPerformed=nt.exportMask)
  panel.add(btnExportMask)
  btnImportMask = JButton("Import mask", actionPerformed=nt.importMask)
  panel.add(btnImportMask)
  btnOpenStack = JButton("Open Stack", actionPerformed=nt.openStack)
  panel.add(btnOpenStack)
  btnExportSegmentation = JButton("Export segmentation", actionPerformed=nt.exportSegmentation)
  panel.add(btnExportSegmentation)
  tabs.add(title, panel)
  tabs.setSelectedComponent(panel)
 
# MAIN
display = Display.getFront()
if display is not None:
  injectNordenTools(display)
else:
  IJ.showMessage("No active TrackEM2-project found. Open one!")