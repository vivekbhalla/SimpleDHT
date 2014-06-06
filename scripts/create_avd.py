#!/usr/bin/env python

import sys, os

def main():
  if len(sys.argv) < 2:
    print "Please enter the number of AVDs you want to create " \
        + "and (optionally) the architecture (x86 or arm)"
    print "Usage: " + sys.argv[0] + " <number of AVDs> [<arch>]"
    return

  arch = "x86"
  abi = "x86"

  if len(sys.argv) == 3:
    arch = sys.argv[2]
    if arch == "arm":
      abi = "armeabi-v7a"
    elif arch != "x86":
      print "Supported architectures are x86 (default) or arm"
      return

  for i in range(int(sys.argv[1])):
    n = str(i)
    cmd = "android create avd -n avd" + n + " -t android-19 -s QVGA -b " + abi
    print cmd
    os.system(cmd)
    home = os.path.expanduser("~")
    avd_config = os.path.join(home, ".android", "avd", \
        "avd" + n + ".avd", "config.ini")
    f = open(avd_config, "w")
    f.write("avd.ini.encoding=ISO-8859-1\n")
    f.write("hw.dPad=yes\n")
    f.write("hw.lcd.density=120\n")
    f.write("hw.cpu.arch=" + arch + "\n")
    f.write("hw.device.hash=1650583591\n")
    f.write("hw.camera.back=none\n")
    f.write("disk.dataPartition.size=200M\n")
    f.write("skin.path=240x320\n")
    f.write("skin.dynamic=yes\n")
    if arch == "arm":
      f.write("hw.cpu.model=cortex-a8\n")
    f.write("hw.keyboard=yes\n")
    f.write("hw.ramSize=256\n")
    f.write("hw.device.manufacturer=Generic\n")
    f.write("hw.sdCard=no\n")
    f.write("hw.mainKeys=yes\n")
    f.write("hw.accelerometer=yes\n")
    f.write("skin.name=240x320\n")
    f.write("abi.type=" + abi + "\n")
    f.write("hw.trackBall=no\n")
    f.write("hw.device.name=2.7in QVGA\n")
    f.write("hw.battery=yes\n")
    f.write("hw.sensors.proximity=yes\n")
    f.write("image.sysdir.1=system-images/android-19/" + abi + "/\n")
    f.write("hw.sensors.orientation=yes\n")
    f.write("hw.audioInput=yes\n")
    f.write("hw.gps=yes\n")
    f.write("vm.heapSize=16\n")
    f.close()

if __name__ == "__main__":
  main()
