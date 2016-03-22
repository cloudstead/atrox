#
# Cookbook Name:: apache
# Recipe:: default
#

base = Chef::Recipe::Base
apache2 = Chef::Recipe::Apache2

package 'apache2' do
  action :install
end

base.public_port self, 'apache2', 80
base.public_port self, 'apache2', 443

apache2_bag = nil
begin
  data_bag_item('apache2', 'init')
rescue
  puts 'No apache2 init databag defined, using default settings'
end

if apache2_bag.nil? || apache2_bag['https_only']
  apache2.enable_module self, 'ssl'
  apache2.enable_module self, 'rewrite'
  template '/etc/apache2/ports.conf' do
    source 'https-only.conf.erb'
    mode '0744'
    action :create
  end
end
