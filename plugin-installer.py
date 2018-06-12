#! /usr/bin/env python

# FAQ:
# How to use in the process of plugin develop?
# If you use it as a client, you should run "./cmd or ./cmd client" in the project directory.
# If you use it as a server, you should run "./cmd or ./cmd server" in the jenkins home directory.
#
# ATTENTIONS:
# BEFORE YOU RUN IT, YOU SHOULD MODIFIY THE IP ADDRESS OF THIS PYTHON SCRIPT ACCORDING THE TRUE HOST IP,
# ADN THEN IT IS NECESSARY THAT THE DEVELOP MACHINE HAS THE PUBLIC KEY OF JENKINS SERVER!

import os
import sys
import socket

BufferSize = 1024
ServerAddress = ("chenlong-uc-01", 7878)

def get_client_info():
    result = os.popen("hostname")
    hostname = result.read().rstrip("\n")
    result = os.popen("whoami")
    loginname = result.read().rstrip("\n")
    result = os.popen("find target -maxdepth 1 -regex \".*\.hpi\"")
    hpifile =  os.path.join(os.getcwd(), result.read().rstrip("\n"))
    return (hostname, loginname, hpifile)

def client_main():
    retval = os.system("mvn package") >> 8
    if retval != 0:
        return
    
    udpcliSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    
    udpcliSocket.sendto("%s:%s:%s" % get_client_info(), ServerAddress)
    data, addr = udpcliSocket.recvfrom(BufferSize)
    if data:
        print(data)
        
    udpcliSocket.close()

def server_main():
    udpSerSocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udpSerSocket.bind(ServerAddress)
    
    print("Server has been entered the work cycle ...")
    while True:
        data, addr = udpSerSocket.recvfrom(BufferSize)
        if not data:
            continue
        hostname = data.split(":")[0].strip()
        loginname = data.split(":")[1].strip()
        hpifile = data.split(":")[2].strip()
        retval = os.system("scp %s@%s:%s ." % (loginname, hostname, hpifile)) >> 8
        if retval != 0:
            udpSerSocket.sendto("Copying the plugin error!", addr)
            continue
        
        hpifilename = os.path.basename(hpifile)
        retval = os.system("service jenkins stop") >> 8
        if retval != 0:
            udpSerSocket.sendto("Stopping jenkins server error!", addr)
            continue
    
        retval = os.system("rm -rf plugins/%s*" % hpifilename.split(".")[0]) >> 8
        if retval != 0:
            udpSerSocket.sendto("Installed jenkins plugin error!", addr)
            continue
    
        retval = os.system("mv %s plugins/%s" % (hpifilename, hpifilename)) >> 8
        if retval != 0:
            udpSerSocket.sendto("Installed jenkins plugin error!", addr)
            continue
    
        retval = os.system("touch plugins/%s.jpi" % hpifilename.split(".")[0]) >> 8
        if retval != 0:
            udpSerSocket.sendto("Installed jenkins plugin error!", addr)
            continue
    
        os.system("chown jenkins:jenkins plugins/%s plugins/%s.jpi" % (hpifilename, hpifilename.split(".")[0]))
        
        retval = os.system("service jenkins start") >> 8
        if retval != 0:
            udpSerSocket.sendto("Starting jenkins server error!", addr)
            continue
        
        udpSerSocket.sendto("Jenkins server is being started ...", addr)
        
    udpSerSocket.close()

def main(argv):
	if len(argv) == 1:
		client_main()
		sys.exit(1)
	if cmp(argv[1], "server") == 0:
		server_main()
	else:
		client_main()

if __name__ == "__main__":
	main(sys.argv)

