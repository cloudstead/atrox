#
# Cookbook Name:: histori
# Recipe:: restart
#

base = Chef::Recipe::Base

histori_bag = data_bag_item('histori', 'init')

base.restart_unless_port self, histori_bag['HISTORI_SERVER_PORT'], ''