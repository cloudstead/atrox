#
# Cookbook Name:: java
# Recipe:: default
#
package 'openjdk-7-jre-headless' do
  action :install
end

%w( jrun jrun-init ).each do |file|
  cookbook_file "/usr/local/bin/#{file}" do
    owner 'root'
    group 'root'
    mode '0755'
    action :create
  end
end

# todo: generify this to install whatever intermediate certs are found in databag
startcom_ca_cert_name='StartComClass2PrimaryIntermediateServerCA'
startcom_ca_cert="/usr/share/ca-certificates/mozilla/#{startcom_ca_cert_name}.crt"

startcom_ca_cert_name2='StartComClass2PrimaryIntermediateServerCA2'
startcom_ca_cert2='/etc/ssl/certs/StartSslIntermediate.crt'

java = Chef::Recipe::Java
java.install_cert self, startcom_ca_cert_name, startcom_ca_cert
java.install_cert self, startcom_ca_cert_name2, startcom_ca_cert2
