#
# Cookbook Name:: histori
# Recipe:: restart
#

base = Chef::Recipe::Base

service_name = node[:histori][:service_name]
port = data_bag_item('histori', 'init')['environment']['HISTORI_SERVER_PORT']

puts "Restarting #{service_name} if nothing found listening on port #{port}..."
base.restart_unless_port self, port, service_name
