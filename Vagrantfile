# -*- mode: ruby -*-
# vi: set ft=ruby :

$script = <<SCRIPT

 # Exit on any errors.
 set -e

 # Setup
 apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv E56151BF
 DISTRO=$(lsb_release -is | tr '[:upper:]' '[:lower:]')
 CODENAME=$(lsb_release -cs)

 echo "Installing prerequisite packages for plugin building..."
 apt-get -y --force-yes install openjdk-7-jdk maven

# make a convenience symlink to our local project mirror
ln -s /vagrant block-queued-job-plugin

# Build mesos-jenkins plugin.
echo "Building block-queued-job-plugin"
su - vagrant -c "cd block-queued-job-plugin && mvn package -DskipTests"
echo "Done"

echo "****************************************************************"
echo "Successfully provisioned the machine."
echo "You can run the Jenkins server with plugin installed as follows:"
echo "> vagrant ssh"
echo "> cd block-queued-job-plugin"
echo "> mvn hpi:run"
echo "****************************************************************"

SCRIPT


# All Vagrant configuration is done below. The "2" in Vagrant.configure
# configures the configuration version (we support older styles for
# backwards compatibility). Please don't change it unless you know what
# you're doing.
Vagrant.configure(2) do |config|
  # The most common configuration options are documented and commented below.
  # For a complete reference, please see the online documentation at
  # https://docs.vagrantup.com.

  # Every Vagrant development environment requires a box. You can search for
  # boxes at https://atlas.hashicorp.com/search.
  config.vm.box = "ubuntu/trusty64"

  # Forward mesos ports.
  config.vm.network "forwarded_port", guest: 5050, host: 5050
  config.vm.network "forwarded_port", guest: 5051, host: 5051

  # Forward jenkins port.
  config.vm.network "forwarded_port", guest: 8080, host: 8080

  # Provision the system.
  config.vm.provision "shell", inline: $script

  config.vm.provider :virtualbox do |vb|
     # Use VBoxManage to customize the VM. For example to change memory:
     vb.customize ["modifyvm", :id, "--memory", "2048"]
     vb.customize ["modifyvm", :id, "--cpus", "2"]
  end
end
