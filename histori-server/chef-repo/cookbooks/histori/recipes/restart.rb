#
# Cookbook Name:: histori
# Recipe:: restart
#

base = Chef::Recipe::Base
java = Chef::Recipe::Java

run_as=node[:histori][:run_as]
current=node[:histori][:current]
java.create_service self, 'histori-api', current, run_as, 'histori.server.HistoriServer', node[:histori][:java_opts]

service_name = node[:histori][:service_name]
port = data_bag_item('histori', 'init')['environment']['HISTORI_SERVER_PORT']

puts "Restarting #{service_name} if nothing found listening on port #{port}..."
base.restart_unless_port self, port, service_name
base.flush_redis self