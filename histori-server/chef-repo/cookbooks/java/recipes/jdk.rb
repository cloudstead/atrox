#
# Cookbook Name:: java
# Recipe:: jdk
#
include_recipe 'java::default'

%w( openjdk-7-jdk ).each do |pkg|
  package pkg do
    action :install
  end
end

