# What got my install to work

First, clone our battlecode repo.

If not on Windows 10 Pro, get Docker Toolbox from https://docs.docker.com/toolbox/toolbox_install_windows/

Restart.

Run "Docker Quickstart Terminal" (from search bar on windows)

If it complains about VT-x not being enabled in the BIOS, go to "Turn Windows Features Off/On" and make sure "Hyper-V" is NOT checked.

Reboot, and during startup, go into your BIOS/UEFI, and make sure to enable Intel Virtualization (for me, it was under the security tab)

Finish restarting.

Run "Docker Quickstart Terminal" and pray it works.

Navigate to our repo folder with standard windows DOS commands.

Run `bash run.sh`

It should work, and everything should be good to go!


~ Neil
