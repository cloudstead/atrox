#
# Cookbook Name:: elasticsearch
# Recipe:: default
#

bash 'install elasticsearch GPG key and apt repository' do
  user 'root'
  code <<-EOF
if [ $(apt-key list | grep dev_ops@elasticsearch.org | wc -l | tr -d ' ') -eq 0 ] ; then
  wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | apt-key add - || exit 1
fi

DEB_REPO="http://packages.elastic.co/elasticsearch/2.x/debian"
APT_REPO="/etc/apt/sources.list.d/elasticsearch-2.x.list"
if [[ ! -e ${APT_REPO} || $(grep ${DEB_REPO} ${APT_REPO} | wc -l | tr -d ' ') -eq 0 ]] ; then
  echo "deb ${DEB_REPO} stable main" | tee -a ${APT_REPO} || exit 1
  apt-get update && apt-get install elasticsearch || exit 1
  update-rc.d elasticsearch defaults 95 10 || exit 1
  service elasticsearch start
fi

  EOF
end
