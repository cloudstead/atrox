#
# Cookbook Name:: base
# Recipe:: default
#

require 'securerandom'

base = Chef::Recipe::Base

# if ntp is installed, uninstall it first. otherwise openntpd will not install correctly
ntp_installed=%x(dpkg -l | awk '{print $2}' | egrep '^ntp$' | wc -l | tr -d ' ').strip
unless ntp_installed.to_s.empty? || ntp_installed.to_i == 0
  %x(if [ -f /etc/apparmor.d/usr.sbin.ntpd ] ; then apparmor_parser -R /etc/apparmor.d/usr.sbin.ntpd ; fi ; apt-get purge -y ntp)
end

essential_packages=%w( openntpd rsync safe-rm uuid )

# ensure packages are up to date
if base.is_docker
  bash 'apt-get update' do
    user 'root'
    code <<-EOF
apt-get update
    EOF
  end

  # do not install ntpd in docker containers
  essential_packages.delete('openntpd')

elsif base.enable_docker
  bash 'apt-get update' do
    user 'root'
    code <<-EOF
# Install docker key
apt-key adv --keyserver hkp://pgp.mit.edu:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D

# Add docker apt repository
echo "deb https://apt.dockerproject.org/repo ubuntu-trusty main" > /etc/apt/sources.list.d/docker.list

apt-get update

# Install docker
apt-get install docker-engine -y
apt-get upgrade docker-engine -y
service docker restart

EOF
  end
end

# every system needs these
essential_packages.each do |pkg|
  package pkg do
    action :install
  end
end

# common utilities
%w( emacs24-nox tshark xtail screen ).each do |pkg|
  package pkg do
    action :install
  end
end

[ '/root/.screenrc', "#{base.chef_user_home}/.screenrc" ].each do |screenrc|
  cookbook_file screenrc do
    source 'dot-screenrc'
    owner 'root'
    group 'root'
    mode '0755'
    action :create
  end
end

# todo: generify this to install whatever intermediate certs are found in databag
startcom_ca_cert_name='StartComClass2PrimaryIntermediateServerCA'
startcom_ca_cert="/usr/share/ca-certificates/mozilla/#{startcom_ca_cert_name}.crt"
startcom_ca_cert_hash='5a5c01b6.0'
ca_cert_dir='/etc/ssl/certs'
cookbook_file startcom_ca_cert do
  source 'StartComClass2PrimaryIntermediateServerCA.pem'
  owner 'root'
  group 'root'
  mode '0644'
  action :create
end

cookbook_file '/etc/ssl/certs/StartSslIntermediate.crt' do
  owner 'root'
  group 'root'
  mode '0644'
  action :create
end

bash 'install StartCom CA Cert' do
  user 'root'
  code <<-EOF
cd #{ca_cert_dir} && \
rm -f #{startcom_ca_cert_name}.pem && \
ln -s #{startcom_ca_cert} #{startcom_ca_cert_name}.pem && \
ln -s #{startcom_ca_cert_name}.pem #{startcom_ca_cert_hash}
  EOF
  not_if { File.exist? "#{ca_cert_dir}/#{startcom_ca_cert_hash}" }
end

bash 'install data_files' do
  user 'root'
  code <<-EOF
DATA_DIR="/opt/cloudos"
DATA_FILES="#{base.chef_dir}/data_files"
mkdir -p ${DATA_DIR} && chown root.root ${DATA_DIR} && chmod 755 ${DATA_DIR}
if [ $(find ${DATA_FILES} -type f 2> /dev/null | wc -l | tr -d ' ') -gt 0 ] ; then
  rsync -avzc ${DATA_FILES}/* ${DATA_DIR}/
  for f in $(find ${DATA_DIR} -type f -name "*.gz") ; do
    gunzip ${f}
  done
  chmod -R 755 ${DATA_DIR}
fi
EOF
end

unless base.is_docker
  rules='/etc/iptables.d'
  bash "touch #{rules}" do
    user 'root'
    code <<-EOF
  mkdir -p #{rules}
    EOF
    not_if { File.exist? rules }
  end

  %w( iptables_header iptables_footer ).each do |rule|
    template "#{rules}/#{rule}" do
      owner 'root'
      group 'root'
      mode '0600'
      action :create
    end
  end

  template '/etc/network/if-pre-up.d/iptables_load' do
    owner 'root'
    group 'root'
    mode '0700'
    action :create
  end
end

# Install any certs provided
base.local_certs('base').each do |cert_name|
  base.install_ssl_cert self, 'base', cert_name
  Chef::Recipe::Java.install_cert(self, cert_name, base.pem_path(cert_name)) if defined? Chef::Recipe::Java.install_cert
end
