#
# Cookbook Name:: postgresql
# Recipe:: default
#

bash 'ensure postgresql is running' do
  user 'root'
  code <<-EOF

if [ ! -x /usr/bin/psql ] ; then
  echo "deb http://apt.postgresql.org/pub/repos/apt/ $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list
  wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -
  apt-get update
  apt-get install postgresql-9.4
fi

status="$(service postgresql status)"
if [ -z "${status}" ] ; then
  echo "Error getting postgresql service status"
  exit 1
fi
if [[ ! "${status}" =~ "online" ]] ; then
  service postgresql restart
fi
  EOF
end
