#
# Cookbook Name:: histori
# Recipe:: bbs
#
# WIP -- see below for TODOS
#

bash 'install docker' do
  code <<-EOH
if [ -z "$(which docker)" ] ; then
  apt-get -y install docker.io
  ln -sf /usr/bin/docker.io /usr/local/bin/docker
  sed -i '$acomplete -F _docker docker' /etc/bash_completion.d/docker
  update-rc.d docker defaults
fi
EOH

end

# todo: install and configure discourse

# todo: setup discourse to run as service

# todo: configure apache vhost config, enable vhost
