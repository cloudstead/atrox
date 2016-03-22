#
# Cookbook Name:: histori
# Recipe:: stop
#

base = Chef::Recipe::Base

service_name = node[:histori][:service_name]

base.stop_service self, service_name
